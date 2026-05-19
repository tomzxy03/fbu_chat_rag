package com.tomzxy.fbu_chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmbeddingResponse {
    private List<List<Float>> embeddings;
}
