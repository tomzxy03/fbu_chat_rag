package com.tomzxy.fbu_chat.controller;

import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.MessageDto;
import com.tomzxy.fbu_chat.entity.User;
import com.tomzxy.fbu_chat.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        ChatResponse response = ragService.chat(request, user.getId().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<?>> getConversations(@AuthenticationPrincipal User user) {
        var conversations = ragService.getUserConversations(user.getId().toString()).stream()
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
