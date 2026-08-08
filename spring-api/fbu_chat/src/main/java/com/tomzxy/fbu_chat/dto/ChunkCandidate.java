package com.tomzxy.fbu_chat.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class ChunkCandidate {
    private String content;
    private int pageNumber;
    private int chunkIndex;

    /**
     * Text thuần để embed — không có context prefix.
     * Chỉ có với .md files (MarkdownProcessor).
     * Nếu null → fallback dùng content để embed (legacy processors).
     */
    @JsonProperty("textToEmbed")
    private String textToEmbed;

    @JsonProperty("parentHeading")
    private String parentHeading;

    @JsonProperty("parentContent")
    private String parentContent;

    private String title;
    private Integer year;
    
    @JsonProperty("docType")
    private String docType;

    @JsonProperty("sourceFile")
    private String sourceFile;

    /**
     * Text thực sự dùng để tạo embedding.
     * Ưu tiên textToEmbed (child thuần) nếu có, fallback về content.
     */
    public String getTextForEmbedding() {
        return (textToEmbed != null && !textToEmbed.isBlank()) ? textToEmbed : content;
    }
}
