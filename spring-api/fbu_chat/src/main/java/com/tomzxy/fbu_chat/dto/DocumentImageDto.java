package com.tomzxy.fbu_chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DocumentImageDto {
    private UUID id;
    private String url;
    private String caption;
    private String tags;
    private String category;
    private Instant uploadedAt;
}
