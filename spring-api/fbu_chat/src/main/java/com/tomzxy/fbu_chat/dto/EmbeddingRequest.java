package com.tomzxy.fbu_chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmbeddingRequest {
    private List<String> texts;

    /**
     * "passage" khi embed document để lưu vào DB.
     * "query"   khi embed câu hỏi của user để tìm kiếm.
     * Nếu null, Python sẽ mặc định dùng "query:".
     */
    private String mode;
}
