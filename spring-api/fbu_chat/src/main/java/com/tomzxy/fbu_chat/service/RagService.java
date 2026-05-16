package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.entity.Conversation;
import com.tomzxy.fbu_chat.entity.Message;
import com.tomzxy.fbu_chat.repository.ConversationRepository;
import com.tomzxy.fbu_chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class RagService {

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    public RagService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            ConversationRepository conversationRepo,
            MessageRepository messageRepo,
            ObjectMapper objectMapper) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        // 1. Tìm hoặc tạo conversation
        Conversation conversation;
        if (request.getConversationId() != null) {
            UUID convId = UUID.fromString(request.getConversationId());
            conversation = conversationRepo.findById(convId)
                    .orElseGet(() -> createConversation(request.getQuery(), userId));
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

        // 3. Gọi AI Service /chat (full URL, explicit JSON)
        var payload = new HashMap<String, Object>();
        payload.put("query", request.getQuery());
        payload.put("top_k", request.getTopK() != null ? request.getTopK() : 5);
        if (request.getYear() != null)
            payload.put("year", request.getYear());
        if (request.getDocType() != null)
            payload.put("doc_type", request.getDocType());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        String url = aiBaseUrl + "/chat";
        log.info("Calling AI service: {} with payload: {}", url, payload);

        Map<String, Object> aiResponse = aiRestTemplate.postForObject(url, entity, Map.class);

        if (aiResponse == null) {
            throw new RuntimeException("AI service trả về response rỗng");
        }

        String answer = (String) aiResponse.get("answer");
        List<Map<String, Object>> sources = (List<Map<String, Object>>) aiResponse.get("sources");

        // 4. Lưu message của assistant
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

        // 5. Build response
        List<ChatResponse.SourceInfo> sourceInfos = sources == null ? List.of()
                : sources.stream().map(s -> ChatResponse.SourceInfo.builder()
                        .file((String) s.get("file"))
                        .year(s.get("year") instanceof Integer ? (Integer) s.get("year") : null)
                        .docType((String) s.get("doc_type"))
                        .build()).toList();

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMsg.getId())
                .query(request.getQuery())
                .answer(answer)
                .sources(sourceInfos)
                .build();
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<Conversation> getAllConversations() {
        return conversationRepo.findAllByOrderByUpdatedAtDesc();
    }

    public List<Conversation> getUserConversations(String userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    private Conversation createConversation(String firstQuery, String userId) {
        String title = firstQuery.length() > 60
                ? firstQuery.substring(0, 57) + "..."
                : firstQuery;
        Conversation conv = Conversation.builder().title(title).userId(userId).build();
        return conversationRepo.save(conv);
    }
}
