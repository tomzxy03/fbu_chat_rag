package com.tomzxy.fbu_chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ChatResponse {
    private UUID conversationId;
    private UUID messageId;
    private String query;
    private String answer;
    private List<SourceInfo> sources;

    @Data
    @Builder
    public static class SourceInfo {
        private String file;
        private Integer year;
        private String docType;
    }
}
