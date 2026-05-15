package com.tomzxy.fbu_chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestResponse {
    private String message;
    private String filename;
    private int chunks;
}
