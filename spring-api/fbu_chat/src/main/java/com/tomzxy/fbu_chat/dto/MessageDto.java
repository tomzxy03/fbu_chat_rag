package com.tomzxy.fbu_chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MessageDto {
    private UUID id;
    private String role;
    private String content;
    private Instant createdAt;
    private List<ChatResponse.SourceInfo> sources;
    private List<ChatResponse.ImageInfo> images;
}
