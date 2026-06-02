package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.ChatHistoryMessage;
import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.dto.ImageResult;
import com.tomzxy.fbu_chat.entity.Conversation;
import com.tomzxy.fbu_chat.entity.Message;
import com.tomzxy.fbu_chat.entity.ParentChunk;
import com.tomzxy.fbu_chat.repository.ConversationRepository;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.DocumentImageRepository;
import com.tomzxy.fbu_chat.repository.MessageRepository;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
import com.tomzxy.fbu_chat.util.TsQueryBuilder;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class RagService {

    private static final double SIMILARITY_THRESHOLD = 0.65;
    private static final double IMAGE_SIMILARITY_THRESHOLD = 0.70;

    private static final int DEFAULT_TOP_K = 8;
    private static final int IMAGE_TOP_K = 3;

    private static final int HISTORY_WINDOW = 4;
    private static final int MAX_HISTORY_CONTENT_LENGTH = 4000;
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final DocumentChunkRepository docRepo;
    private final DocumentImageRepository imageRepo;
    private final ParentChunkRepository parentChunkRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate groqRestTemplate = new RestTemplate();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public RagService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            ConversationRepository conversationRepo,
            MessageRepository messageRepo,
            DocumentChunkRepository docRepo,
            DocumentImageRepository imageRepo,
            ParentChunkRepository parentChunkRepo,
            ObjectMapper objectMapper) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.docRepo = docRepo;
        this.imageRepo = imageRepo;
        this.parentChunkRepo = parentChunkRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        Conversation conversation = null;
        if (userId != null) {
            if (request.getConversationId() != null) {
                UUID convId = UUID.fromString(request.getConversationId());
                conversation = conversationRepo.findByIdAndUserId(convId, userId)
                        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                                "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
            } else {
                conversation = createConversation(request.getQuery(), userId);
            }
        }

        ensureGroqApiKeyConfigured();
        if (!isFbuInformationQuery(request.getQuery())) {
            log.info("Detected non-RAG conversational query. Skipping embedding/search pipeline.");
            return buildConversationalChatResponse(request, conversation);
        }

        log.info("Encoding query using AI Service...");
        EmbeddingRequest embReq = new EmbeddingRequest();
        embReq.setTexts(List.of(request.getQuery()));
        embReq.setMode("query");

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
        List<ChatResponse.ImageInfo> images = searchImages(vectorStr);

        int topK = request.getTopK() != null ? request.getTopK() : DEFAULT_TOP_K;
        int candidateK = topK * 3;

        String[] tsQueries = TsQueryBuilder.buildSmart(request.getQuery());
        String andQuery = tsQueries[0];
        String orQuery = tsQueries[1];

        log.info("Searching (topK={}, threshold={}, AND={}, year={}, docType={})",
                topK, SIMILARITY_THRESHOLD, andQuery,
                request.getYear(), request.getDocType());

        List<ChunkResult> topContexts = List.of();

        if (andQuery != null) {
            try {
                List<ChunkResult> andResults = docRepo.hybridSearch(vectorStr, andQuery, topK, candidateK);
                topContexts = filterByMetadata(andResults, request);
                log.info("Pass 1 (AND) after metadata filter: {} results", topContexts.size());
            } catch (Exception e) {
                log.warn("Hybrid AND failed: {}", e.getMessage());
            }
        }

        if (topContexts.size() < 2 && orQuery != null) {
            log.info("Pass 1 insufficient, trying OR fallback...");
            try {
                List<ChunkResult> orResults = docRepo.hybridSearch(vectorStr, orQuery, topK, candidateK);
                List<ChunkResult> filteredOr = filterByMetadata(orResults, request);
                log.info("Pass 2 (OR) after metadata filter: {} results", filteredOr.size());

                if (filteredOr.size() > topContexts.size()) {
                    topContexts = filteredOr;
                }
            } catch (Exception e) {
                log.warn("Hybrid OR failed: {}", e.getMessage());
            }
        }

        if (topContexts.size() < 2) {
            log.info("Hybrid insufficient, falling back to pure vector...");
            List<ChunkResult> vectorResults = docRepo.findTopRelatedContexts(vectorStr, topK, SIMILARITY_THRESHOLD);
            List<ChunkResult> filteredVector = filterByMetadata(vectorResults, request);
            log.info("Pass 3 (vector) after metadata filter: {} results", filteredVector.size());

            if (filteredVector.size() > topContexts.size()) {
                topContexts = filteredVector;
            }
        }

        if (topContexts.isEmpty()) {
            if (!images.isEmpty()) {
                log.info("No document chunks found, but {} images matched. Returning image-only response.", images.size());
                return buildImageOnlyChatResponse(request, conversation, images);
            }
            log.info("No reliable document chunks found. Triggering no-data fallback response.");
            return buildFallbackChatResponse(request, conversation);
        }

        log.info("Final chunks for LLM: {}", topContexts.size());

        String contextText = buildContextWithParents(topContexts);

        log.info("=== CHUNKS SENT TO LLM ({}) ===", topContexts.size());
        for (int i = 0; i < topContexts.size(); i++) {
            ChunkResult c = topContexts.get(i);
            log.info("[{}] {} | len={} | preview={}",
                    i + 1,
                    c.getSourceFile(),
                    c.getContent() != null ? c.getContent().length() : 0,
                    c.getContent() != null
                            ? c.getContent().substring(0, Math.min(80, c.getContent().length()))
                                    .replace("\n", " ")
                            : "NULL");
        }
        log.info("================================");

        String systemPrompt = 
            "# VAI TRÒ VÀ ĐỊNH DANH\n" +
            "Bạn là Trợ lý AI chuyên nghiệp và thân thiện của Trường Đại học Tài chính - Ngân hàng Hà Nội (FBU). " +
            "Nhiệm vụ của bạn là hỗ trợ sinh viên và giảng viên tra cứu các quy chế, quy định nội bộ dựa trên dữ liệu [CONTEXT] được cung cấp.\n\n" +
    
            "# NGUYÊN TẮC CỐT LÕI (TUÂN THỦ TUYỆT ĐỐI)\n" +
            "1. CHỈ câu trả lời dựa trên thông tin có trong [CONTEXT]. Tuyệt đối không tự suy diễn, bịa đặt hoặc dùng kiến thức chung trên Internet để đoán quy định của FBU.\n" +
            "2. Trả lời bằng tiếng Việt lịch sự, truyền cảm hứng, ngắn gọn nhưng đầy đủ ý. Sử dụng các dấu gạch đầu dòng rõ ràng để phân tách các quy trình, điều khoản.\n" +
            "3. Quản lý lịch sử hội thoại: Đọc kỹ các câu trả lời trước đó để KHÔNG lặp lại thông tin cũ. Chỉ tập trung bổ sung thông tin mới đáp ứng đúng câu hỏi tiếp diễn.\n\n" +
    
            "# HƯỚNG DẪN XỬ LÝ KHI THIẾU THÔNG TIN (KỊCH BẢN FALLBACK)\n" +
            "Khi [CONTEXT] trống rỗng, thông tin quá sơ sài hoặc hoàn toàn lệch khỏi chủ đề người dùng đang hỏi, bạn TUYỆT ĐỐI không từ chối một cách cụt ngủn. Hãy trả lời theo văn phong ấm áp và hướng dẫn sau:\n" +
            "- Ghi nhận câu hỏi và khéo léo thông báo rằng hệ thống dữ liệu nội bộ hiện tại chưa cập nhật thông tin chính thức về chủ đề này.\n" +
            "- Chủ động và lịch sự mời gọi người dùng đóng góp dữ liệu: Nếu họ có tài liệu hoặc thông tin chính xác, họ có thể gửi phản hồi trực tiếp qua 'Tab Góp ý' trên hệ thống, hoặc liên hệ qua kênh Facebook/Gmail của tác giả phát triển dự án để đội ngũ kỹ thuật cập nhật kịp thời.\n" +
            "- *Ví dụ văn phong:* 'Hiện tại hệ thống dữ liệu của mình chưa có thông tin chính thức về vấn đề này mất rồi... Nếu bạn biết hoặc đang giữ tài liệu liên quan, bạn có thể gửi phản hồi qua...'\n\n" +
    
            "# QUY TẮC CẤM ĐỊNH DẠNG NGUỒN\n" +
            "- Tuyệt đối KHÔNG được tự viết chữ 'Nguồn:' hoặc tự tổng hợp danh sách tên file ở cuối câu trả lời dưới mọi hình thức (Hệ thống đã có bộ lọc tự động xử lý phần này).\n" +
            "- Bạn chỉ được phép lồng ghép tên văn bản một cách tự nhiên vào câu văn nếu cần làm rõ tính pháp lý (Ví dụ: 'Dựa trên Quyết định số 116, quy trình xác nhận sinh viên gồm...').";

        String userPrompt = "CONTEXT TỪ TÀI LIỆU FBU:\n" + contextText + "\n\n" +
            "CÂU HỎI HIỆN TẠI: " + request.getQuery() + "\n\n" +
            "Trả lời (Tuân thủ tuyệt đối quy tắc định dạng nguồn):";

        log.info("Calling Groq LLM Generator...");
        Map<String, Object> groqMsg1 = new HashMap<>();
        groqMsg1.put("role", "system");
        groqMsg1.put("content", systemPrompt);

        List<Map<String, Object>> groqMessages = new java.util.ArrayList<>();
        groqMessages.add(groqMsg1);
        if (conversation != null) {
            List<Message> recentHistory = messageRepo
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            int fromIdx = Math.max(0, recentHistory.size() - HISTORY_WINDOW * 2);
            for (Message histMsg : recentHistory.subList(fromIdx, recentHistory.size())) {
                Map<String, Object> hm = new HashMap<>();
                hm.put("role", histMsg.getRole());
                hm.put("content", histMsg.getContent());
                groqMessages.add(hm);
            }

            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);
        } else if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            int fromIdx = Math.max(0, request.getHistory().size() - HISTORY_WINDOW * 2);
            List<ChatHistoryMessage> clientHistory = request.getHistory()
                    .subList(fromIdx, request.getHistory().size());

            for (ChatHistoryMessage histMsg : clientHistory) {
                if (histMsg == null) {
                    continue;
                }
                addHistoryMessage(groqMessages, histMsg.getRole(), histMsg.getContent());
            }
        }

        Map<String, Object> groqMsg2 = new HashMap<>();
        groqMsg2.put("role", "user");
        groqMsg2.put("content", userPrompt);
        groqMessages.add(groqMsg2);

        Map<String, Object> groqPayload = new HashMap<>();
        groqPayload.put("model", GROQ_MODEL);
        groqPayload.put("messages", groqMessages);
        groqPayload.put("temperature", 0.3);
        groqPayload.put("max_tokens", 1024);

        String answer = callGroq(groqPayload);
        boolean noDataAnswer = isNoDataAnswer(answer);
        if (noDataAnswer) {
            answer = stripNoDataMarker(answer);
            log.info("LLM reported insufficient context. Clearing sources from response.");
            images = List.of();
        }

        List<Map<String, Object>> sources = noDataAnswer
                ? List.of()
                : topContexts.stream().map(c -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("file", c.getSourceFile());
                    s.put("year", c.getYear());
                    s.put("doc_type", c.getDocType());
                    return s;
                }).collect(Collectors.toList());

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

            conversationRepo.save(conversation);
        }

        List<ChatResponse.SourceInfo> sourceInfos = sources.stream()
                .collect(Collectors.toMap(
                        s -> (String) s.get("file"),
                        s -> ChatResponse.SourceInfo.builder()
                                .file((String) s.get("file"))
                                .year(s.get("year") instanceof Integer ? (Integer) s.get("year") : null)
                                .docType((String) s.get("doc_type"))
                                .build(),
                        (existing, duplicate) -> existing
                ))
                .values()
                .stream()
                .toList();

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(sourceInfos)
                .images(images)
                .build();
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    private void addHistoryMessage(List<Map<String, Object>> groqMessages, String role, String content) {
        if (!isAllowedHistoryRole(role) || content == null || content.isBlank()) {
            return;
        }

        Map<String, Object> hm = new HashMap<>();
        hm.put("role", role);
        hm.put("content", content.length() > MAX_HISTORY_CONTENT_LENGTH
                ? content.substring(0, MAX_HISTORY_CONTENT_LENGTH)
                : content);
        groqMessages.add(hm);
    }

    private boolean isAllowedHistoryRole(String role) {
        return "user".equals(role) || "assistant".equals(role);
    }

    private boolean isFbuInformationQuery(String query) {
        String classifierPrompt = "Phân loại câu hỏi của người dùng cho chatbot FBU.\n"
                + "Trả về đúng một nhãn:\n"
                + "- FBU_INFO: nếu câu hỏi cần tra cứu thông tin/quy định/tài liệu nội bộ của Đại học Tài chính - Ngân hàng Hà Nội (FBU), ví dụ học phí, lịch học, lịch thi, học bổng, tuyển sinh, quy chế, ngành học.\n"
                + "- GENERAL_CHAT: nếu là chào hỏi, cảm ơn, tạm biệt, hỏi bạn là ai, trò chuyện xã giao, câu đùa, hoặc câu không cần tra cứu tài liệu FBU. Chấp nhận lỗi gõ phím/viết sai chính tả như 'xin chafo'.\n"
                + "Chỉ trả về FBU_INFO hoặc GENERAL_CHAT. Không giải thích.";

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", classifierPrompt));
        messages.add(Map.of("role", "user", "content", query));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", GROQ_MODEL);
        payload.put("messages", messages);
        payload.put("temperature", 0);
        payload.put("max_tokens", 8);

        String decision = callGroq(payload).trim().toUpperCase(Locale.ROOT);
        if (decision.contains("GENERAL_CHAT")) {
            return false;
        }
        if (decision.contains("FBU_INFO")) {
            return true;
        }

        log.warn("Unexpected intent classifier response '{}'. Falling back to RAG.", decision);
        return true;
    }

    private ChatResponse buildConversationalChatResponse(ChatRequest request, Conversation conversation) {
        List<Map<String, Object>> groqMessages = new ArrayList<>();
        groqMessages.add(Map.of(
                "role", "system",
                "content", "Bạn là trợ lý AI thân thiện của trường Đại học Tài chính - Ngân hàng Hà Nội (FBU). "
                        + "Trả lời các câu xã giao/tán gẫu bằng tiếng Việt, tự nhiên, ngắn gọn. "
                        + "Nếu người dùng hỏi thông tin chính thức cần tra cứu tài liệu FBU, hãy gợi ý họ đặt câu hỏi cụ thể để hệ thống tra cứu nguồn nội bộ."));

        if (conversation != null) {
            List<Message> recentHistory = messageRepo
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            int fromIdx = Math.max(0, recentHistory.size() - HISTORY_WINDOW * 2);
            for (Message histMsg : recentHistory.subList(fromIdx, recentHistory.size())) {
                addHistoryMessage(groqMessages, histMsg.getRole(), histMsg.getContent());
            }
        } else if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            int fromIdx = Math.max(0, request.getHistory().size() - HISTORY_WINDOW * 2);
            List<ChatHistoryMessage> clientHistory = request.getHistory()
                    .subList(fromIdx, request.getHistory().size());

            for (ChatHistoryMessage histMsg : clientHistory) {
                if (histMsg == null) {
                    continue;
                }
                addHistoryMessage(groqMessages, histMsg.getRole(), histMsg.getContent());
            }
        }

        groqMessages.add(Map.of("role", "user", "content", request.getQuery()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", GROQ_MODEL);
        payload.put("messages", groqMessages);
        payload.put("temperature", 0.4);
        payload.put("max_tokens", 256);

        String answer = callGroq(payload);
        UUID messageId = null;
        if (conversation != null) {
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);

            Message assistantMsg = Message.builder()
                    .conversation(conversation)
                    .role("assistant")
                    .content(answer)
                    .sources("[]")
                    .build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();

            conversationRepo.save(conversation);
        }

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(List.of())
                .images(List.of())
                .build();
    }

    private ChatResponse buildImageOnlyChatResponse(
            ChatRequest request,
            Conversation conversation,
            List<ChatResponse.ImageInfo> images) {
        String answer = "Mình tìm thấy một số hình ảnh minh họa phù hợp với câu hỏi của bạn. "
                + "Bạn có thể xem các ảnh được đính kèm bên dưới.";

        UUID messageId = null;
        if (conversation != null) {
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);

            Message assistantMsg = Message.builder()
                    .conversation(conversation)
                    .role("assistant")
                    .content(answer)
                    .sources("[]")
                    .build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();

            conversationRepo.save(conversation);
        }

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(List.of())
                .images(images)
                .build();
    }

    private ChatResponse buildFallbackChatResponse(ChatRequest request, Conversation conversation) {
        String answer = "Hiện tại hệ thống dữ liệu của mình chưa có thông tin chính thức về câu hỏi: \""
                + request.getQuery()
                + "\".\n\nNếu bạn biết hoặc có tài liệu chính thức về nội dung này, bạn có thể gửi phản hồi qua Phòng Công tác Sinh viên hoặc email support-chatbot@fbu.edu.vn để hệ thống được cập nhật đầy đủ hơn. Cảm ơn bạn đã góp ý.";

        UUID messageId = null;
        if (conversation != null) {
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);

            Message assistantMsg = Message.builder()
                    .conversation(conversation)
                    .role("assistant")
                    .content(answer)
                    .sources("[]")
                    .build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();

            conversationRepo.save(conversation);
        }

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(List.of())
                .images(List.of())
                .build();
    }

    private List<ChatResponse.ImageInfo> searchImages(String vectorStr) {
        try {
            return imageRepo.findSimilarImages(vectorStr, IMAGE_TOP_K, IMAGE_SIMILARITY_THRESHOLD)
                    .stream()
                    .map(this::toImageInfo)
                    .toList();
        } catch (Exception e) {
            log.warn("Image search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private ChatResponse.ImageInfo toImageInfo(ImageResult result) {
        Double score = result.getScore();
        return ChatResponse.ImageInfo.builder()
                .url(result.getUrl())
                .caption(result.getCaption())
                .category(result.getCategory())
                .score(score != null ? score : 0.0)
                .build();
    }

    private boolean isNoDataAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String normalized = answer.toLowerCase(Locale.ROOT);
        return normalized.startsWith("[no_data]")
                || normalized.contains("hệ thống dữ liệu") && normalized.contains("chưa có thông tin chính thức")
                || normalized.contains("không tìm thấy thông tin này trong tài liệu nội bộ");
    }

    private String stripNoDataMarker(String answer) {
        return answer.replaceFirst("(?i)^\\s*\\[NO_DATA]\\s*", "").trim();
    }

    private String callGroq(Map<String, Object> payload) {
        ensureGroqApiKeyConfigured();

        HttpHeaders groqHeaders = new HttpHeaders();
        groqHeaders.setBearerAuth(groqApiKey);
        groqHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(payload, groqHeaders);
        Map groqResp = groqRestTemplate.postForObject("https://api.groq.com/openai/v1/chat/completions", groqEntity,
                Map.class);

        if (groqResp == null || !groqResp.containsKey("choices")) {
            throw new RuntimeException("Lỗi phản hồi từ Groq");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResp.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private void ensureGroqApiKeyConfigured() {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY chưa được cấu hình ở môi trường Spring Boot");
        }
    }

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

    private List<ChunkResult> filterByMetadata(List<ChunkResult> results, ChatRequest request) {
        if (results == null || results.isEmpty())
            return List.of();

        if (request.getYear() == null && request.getDocType() == null)
            return results;

        List<ChunkResult> filtered = results.stream()
                .filter(c -> request.getYear() == null
                        || c.getYear() == null
                        || request.getYear().equals(c.getYear()))
                .filter(c -> request.getDocType() == null
                        || c.getDocType() == null
                        || request.getDocType().equalsIgnoreCase(c.getDocType()))
                .collect(Collectors.toList());

        if (!results.isEmpty() && filtered.size() < results.size() / 2) {
            log.warn("Metadata filter dropped {}/{} chunks — Check your DB metadata injection quality!",
                    results.size() - filtered.size(), results.size());
        }

        return filtered;
    }

    private String buildContextWithParents(List<ChunkResult> contexts) {
        List<UUID> parentIds = contexts.stream()
                .map(ChunkResult::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, ParentChunk> parentMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<ParentChunk> parents = parentChunkRepo.findAllById(parentIds);
            for (ParentChunk p : parents) {
                parentMap.put(p.getId(), p);
            }
        }

        Set<UUID> usedParentIds = new HashSet<>();
        Set<String> usedFallbackKeys = new HashSet<>();
        List<String> contextParts = new ArrayList<>();

        for (ChunkResult c : contexts) {
            UUID pid = c.getParentId();
            String yearLabel = c.getYear() != null ? String.valueOf(c.getYear()) : "Không rõ năm";

            String sectionLabel = (c.getSection() != null && !c.getSection().isBlank())
                    ? " | Mục: " + c.getSection()
                    : "";

            if (pid != null && parentMap.containsKey(pid)) {
                if (usedParentIds.add(pid)) {
                    ParentChunk parent = parentMap.get(pid);
                    contextParts.add(String.format("[Nguồn: %s | Năm: %s%s]\n%s",
                            c.getSourceFile(), yearLabel, sectionLabel, parent.getContent()));
                }
            } else {
                if (pid != null) {
                    log.warn(
                            "Khẩn cấp: Tìm thấy parentId {} cho chunk của file {} nhưng không tồn tại trong bảng parent_chunks! Tự động hạ cấp sang Child Content.",
                            pid, c.getSourceFile());
                }

                String fallbackKey = c.getSourceFile() + ":"
                        + (c.getContent() != null ? c.getContent().hashCode() : "");

                if (usedFallbackKeys.add(fallbackKey)) {
                    contextParts.add(String.format("[Nguồn: %s | Năm: %s%s (Mẩu tin nhỏ)]\n%s",
                            c.getSourceFile(), yearLabel, sectionLabel, c.getContent()));
                }
            }
        }

        return String.join("\n\n---\n\n", contextParts);
    }
}
