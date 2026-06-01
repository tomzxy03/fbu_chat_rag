package com.tomzxy.fbu_chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotBlank(message = "Câu hỏi không được để trống")
    private String query;

    private String conversationId; // null → tạo conversation mới
    private List<ChatHistoryMessage> history; // lịch sử hội thoại từ client, dùng cho anonymous chat

    private Integer year; // filter theo năm tài liệu
    private String docType; // filter theo loại tài liệu
    private Integer topK = 5; // số lượng context trả về
}
