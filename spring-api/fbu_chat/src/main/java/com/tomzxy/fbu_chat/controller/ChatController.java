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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.Map;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    /**
     * POST /api/chat — public, anonymous hoặc authenticated
     * Nếu login: gắn userId, lưu conversation
     * Nếu anonymous: userId = null, không lưu conversation
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        String userId = (user != null) ? user.getId().toString() : null;
        ChatResponse response = ragService.chat(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chat/conversations — requires login
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."
                    ));
        }
        var conversations = ragService.getUserConversations(user.getId().toString()).stream()
                .map(c -> {
                    var map = new java.util.HashMap<String, Object>();
                    map.put("id", c.getId());
                    map.put("title", c.getTitle());
                    map.put("updatedAt", c.getUpdatedAt());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(conversations);
    }

    /**
     * GET /api/chat/conversations/{id}/messages — requires login
     * Chỉ trả về messages nếu conversation thuộc về user hiện tại.
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        List<MessageDto> history = ragService.getHistoryForUser(id, user.getId().toString()).stream()
                .map(m -> MessageDto.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .sources(ragService.deserializeSources(m.getSources()))
                        .images(ragService.deserializeImages(m.getImages()))
                        .build())
                .toList();
        return ResponseEntity.ok(history);
    }
}
