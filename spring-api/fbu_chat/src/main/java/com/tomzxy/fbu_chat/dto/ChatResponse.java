package com.tomzxy.fbu_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private List<ImageInfo> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {
        private String file;
        private Integer year;
        private String docType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfo {
        private String url;
        private String caption;
        private String category;
        private double score;
    }
}
