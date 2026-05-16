package com.tomzxy.fbu_chat.controller;

import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.MessageDto;
import com.tomzxy.fbu_chat.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    /**
     * POST /api/chat
     * Gửi tin nhắn và nhận câu trả lời RAG
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = ragService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chat/conversations
     * Danh sách tất cả conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<?>> getConversations() {
        var conversations = ragService.getAllConversations().stream()
                .map(c -> new java.util.HashMap<String, Object>() {
                    {
                        put("id", c.getId());
                        put("title", c.getTitle());
                        put("updatedAt", c.getUpdatedAt());
                    }
                })
                .toList();
        return ResponseEntity.ok(conversations);
    }

    /**
     * GET /api/chat/conversations/{id}/messages
     * Lịch sử tin nhắn trong một conversation
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getHistory(@PathVariable UUID id) {
        List<MessageDto> history = ragService.getHistory(id).stream()
                .map(m -> MessageDto.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(history);
    }
}
