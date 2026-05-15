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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final WebClient aiWebClient;
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
        Map<String, Object> aiPayload = Map.of(
                "query", request.getQuery(),
                "top_k", request.getTopK() != null ? request.getTopK() : 5,
                "year", request.getYear() == null ? "" : request.getYear(),
                "doc_type", request.getDocType() == null ? "" : request.getDocType());

        // Remove null values for cleaner request
        var cleanPayload = new java.util.HashMap<String, Object>();
        cleanPayload.put("query", request.getQuery());
        cleanPayload.put("top_k", request.getTopK() != null ? request.getTopK() : 5);
        if (request.getYear() != null)
            cleanPayload.put("year", request.getYear());
        if (request.getDocType() != null)
            cleanPayload.put("doc_type", request.getDocType());

        Map aiResponse = aiWebClient.post()
                .uri("/chat")
                .bodyValue(cleanPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

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
