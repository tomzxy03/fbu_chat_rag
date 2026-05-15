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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class RagService {

    private final RestTemplate aiRestTemplate;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // 1. Tìm hoặc tạo conversation
        Conversation conversation;
        if (request.getConversationId() != null) {
            UUID convId = UUID.fromString(request.getConversationId());
            conversation = conversationRepo.findById(convId)
                    .orElseGet(() -> createConversation(request.getQuery()));
        } else {
            conversation = createConversation(request.getQuery());
        }

        // 2. Lưu message của user
        Message userMsg = Message.builder()
                .conversation(conversation)
                .role("user")
                .content(request.getQuery())
                .build();
        messageRepo.save(userMsg);

        // 3. Gọi AI Service /chat
        var payload = new HashMap<String, Object>();
        payload.put("query", request.getQuery());
        payload.put("top_k", request.getTopK() != null ? request.getTopK() : 5);
        if (request.getYear() != null)
            payload.put("year", request.getYear());
        if (request.getDocType() != null)
            payload.put("doc_type", request.getDocType());

        Map<String, Object> aiResponse = aiRestTemplate.postForObject("/chat", payload, Map.class);

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

    private Conversation createConversation(String firstQuery) {
        String title = firstQuery.length() > 60
                ? firstQuery.substring(0, 57) + "..."
                : firstQuery;
        Conversation conv = Conversation.builder().title(title).build();
        return conversationRepo.save(conv);
    }
}
