package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.entity.Conversation;
import com.tomzxy.fbu_chat.entity.Message;
import com.tomzxy.fbu_chat.repository.ConversationRepository;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class RagService {

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final DocumentChunkRepository docRepo;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public RagService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            ConversationRepository conversationRepo,
            MessageRepository messageRepo,
            DocumentChunkRepository docRepo,
            ObjectMapper objectMapper) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.docRepo = docRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        // 1. Conversation: chỉ tạo/lưu khi user đã login
        Conversation conversation = null;
        if (userId != null) {
            if (request.getConversationId() != null) {
                UUID convId = UUID.fromString(request.getConversationId());
                // Ownership check: chỉ cho phép nếu conversation thuộc về user hiện tại
                conversation = conversationRepo.findByIdAndUserId(convId, userId)
                        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                                "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
            } else {
                conversation = createConversation(request.getQuery(), userId);
            }

            // 2. Lưu message của user
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);
        }

        // 3. Tìm vector cho query từ AI Service
        log.info("Encoding query using AI Service...");
        EmbeddingRequest embReq = new EmbeddingRequest();
        embReq.setTexts(List.of(request.getQuery()));
        embReq.setMode("query"); // e5-small-v2: câu hỏi dùng prefix "query:"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(embReq, headers);

        ResponseEntity<EmbeddingResponse> embResponse = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings", HttpMethod.POST, entity, EmbeddingResponse.class);

        if (embResponse.getBody() == null || embResponse.getBody().getEmbeddings().isEmpty()) {
            throw new RuntimeException("Lỗi sinh Embedding cho câu hỏi");
        }

        List<Float> queryVector = embResponse.getBody().getEmbeddings().get(0);
        String vectorStr = "[" + queryVector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";

        // 4. Tìm kiếm ngữ cảnh trong DB
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        log.info("Searching pgvector database (topK={}, year={}, docType={})...",
                topK, request.getYear(), request.getDocType());

        List<ChunkResult> topContexts;
        if (request.getYear() != null || request.getDocType() != null) {
            topContexts = docRepo.findTopRelatedContextsFiltered(
                    vectorStr, topK, request.getYear(), request.getDocType());
        } else {
            topContexts = docRepo.findTopRelatedContexts(vectorStr, topK);
        }

        String contextText;
        if (topContexts.isEmpty()) {
            contextText = "Không tìm thấy tài liệu liên quan.";
        } else {
            contextText = topContexts.stream()
                    .map(c -> String.format("[Nguồn: %s | Năm: %d]\n%s", c.getSourceFile(), c.getYear(),
                            c.getContent()))
                    .collect(Collectors.joining("\n\n---\n\n"));
        }

        // 5. Gọi Groq LLM API
        String systemPrompt = "Bạn là trợ lý AI của trường FBU\n" +
                "Nhiệm vụ của bạn là trả lời câu hỏi của sinh viên và giảng viên dựa trên tài liệu nội bộ được cung cấp.\n"
                +
                "Quy tắc:\n" +
                "- Chỉ trả lời dựa trên CONTEXT được cung cấp.\n" +
                "- Nếu không có thông tin liên quan trong context, hãy nói rõ là không tìm thấy thông tin.\n" +
                "- Trả lời bằng tiếng Việt, chính xác và đầy đủ.\n" +
                "- Trích dẫn nguồn tài liệu khi trả lời.";

        String userPrompt = "CONTEXT từ tài liệu:\n" + contextText + "\n\nCÂU HỎI: " + request.getQuery()
                + "\n\nTrả lời:";

        log.info("Calling Groq LLM Generator...");
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY chưa được cấu hình ở môi trường Spring Boot");
        }

        RestTemplate groqTemplate = new RestTemplate();
        HttpHeaders groqHeaders = new HttpHeaders();
        groqHeaders.setBearerAuth(groqApiKey);
        groqHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> groqMsg1 = new HashMap<>();
        groqMsg1.put("role", "system");
        groqMsg1.put("content", systemPrompt);

        Map<String, Object> groqMsg2 = new HashMap<>();
        groqMsg2.put("role", "user");
        groqMsg2.put("content", userPrompt);

        Map<String, Object> groqPayload = new HashMap<>();
        groqPayload.put("model", "llama-3.3-70b-versatile");
        groqPayload.put("messages", List.of(groqMsg1, groqMsg2));
        groqPayload.put("temperature", 0.3);
        groqPayload.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqPayload, groqHeaders);
        Map groqResp = groqTemplate.postForObject("https://api.groq.com/openai/v1/chat/completions", groqEntity,
                Map.class);

        if (groqResp == null || !groqResp.containsKey("choices")) {
            throw new RuntimeException("Lỗi phản hồi từ Groq");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResp.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String answer = (String) message.get("content");

        // 6. Build sources object to save
        List<Map<String, Object>> sources = topContexts.stream().map(c -> {
            Map<String, Object> s = new HashMap<>();
            s.put("file", c.getSourceFile());
            s.put("year", c.getYear());
            s.put("doc_type", c.getDocType());
            return s;
        }).collect(Collectors.toList());

        // 7. Lưu message assistant (chỉ khi logged in)
        UUID messageId = null;
        if (conversation != null) {
            String sourcesJson = "";
            try {
                sourcesJson = objectMapper.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources", e);
            }
            Message assistantMsg = Message.builder()
                    .conversation(conversation)
                    .role("assistant")
                    .content(answer)
                    .sources(sourcesJson)
                    .build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();

            // Cập nhật updatedAt của conversation để reflect hoạt động chat mới nhất
            // @PreUpdate trên entity sẽ tự set updatedAt = now()
            conversationRepo.save(conversation);
        }

        // 8. Build response
        List<ChatResponse.SourceInfo> sourceInfos = sources.stream().map(s -> ChatResponse.SourceInfo.builder()
                .file((String) s.get("file"))
                .year(s.get("year") instanceof Integer ? (Integer) s.get("year") : null)
                .docType((String) s.get("doc_type"))
                .build()).toList();

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(sourceInfos)
                .build();
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Lấy lịch sử hội thoại với ownership check.
     * Ném AccessDeniedException nếu conversation không thuộc về userId.
     */
    public List<Message> getHistoryForUser(UUID conversationId, String userId) {
        conversationRepo.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<Conversation> getAllConversations() {
        return conversationRepo.findAllByOrderByUpdatedAtDesc();
    }

    public List<Conversation> getUserConversations(String userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    private Conversation createConversation(String query, String userId) {
        String title = query.length() > 50 ? query.substring(0, 50) + "..." : query;
        Conversation conv = Conversation.builder()
                .userId(userId)
                .title(title)
                .build();
        return conversationRepo.save(conv);
    }

}
