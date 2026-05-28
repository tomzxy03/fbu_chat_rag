package com.tomzxy.fbu_chat.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class ChunkCandidate {
    private String content;
    private int pageNumber;
    private int chunkIndex;

    // ─── Ép Jackson map đúng từ snake_case của Python sang camelCase ───
    @JsonProperty("parent_heading")
    private String parentHeading;

    @JsonProperty("parent_content")
    private String parentContent;

    private String title;
    private Integer year;
    
    @JsonProperty("doc_type") // Check luôn nếu Python trả về doc_type
    private String docType;

    @JsonProperty("source_file") // Check luôn nếu Python trả về source_file
    private String sourceFile;
}
