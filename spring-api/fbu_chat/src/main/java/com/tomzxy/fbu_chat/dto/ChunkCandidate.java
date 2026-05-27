package com.tomzxy.fbu_chat.dto;

import lombok.Data;

@Data
public class ChunkCandidate {
    private String content;
    private int pageNumber;
    private int chunkIndex;

    // ─── Fields mới cho MarkdownProcessor (parent-child chunking) ───
    private String parentHeading;
    private String parentContent;
    private String title;
    private Integer year;
    private String docType;
    private String sourceFile;
}
