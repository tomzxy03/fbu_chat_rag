package com.tomzxy.fbu_chat.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class ChunkCandidate {
    private String content;
    private int pageNumber;
    private int chunkIndex;

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
}
