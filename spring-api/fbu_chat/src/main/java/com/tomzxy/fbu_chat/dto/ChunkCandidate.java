package com.tomzxy.fbu_chat.dto;

import lombok.Data;

@Data
public class ChunkCandidate {
    private String content;
    private int pageNumber;
    private int chunkIndex;
}
