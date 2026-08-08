package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phân loại query của người dùng trong 1 Groq call.
 * Trả về intent (FBU_INFO / GENERAL_CHAT) + docType slot trong 1 record.
 * Tiết kiệm so với 2 calls tuần tự cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryClassifierService {

    private final GroqChatClient groqClient;
    private final ObjectMapper objectMapper;

    public record QueryClassification(boolean isFbuInfo, String docType) {}

    private static final String CLASSIFIER_PROMPT =
            "Phân tích câu hỏi của người dùng cho chatbot FBU và trả về JSON.\n\n"
            + "Trường 'intent':\n"
            + "- \"FBU_INFO\": câu hỏi cần tra cứu tài liệu nội bộ FBU (học phí, lịch thi, học bổng, quy chế, ngành học, cơ sở vật chất, giới thiệu trường, tác giả/người tạo chatbot, thông tin dự án)\n"
            + "- \"GENERAL_CHAT\": chào hỏi, cảm ơn, tạm biệt, hỏi AI là gì, trò chuyện xã giao, câu đùa\n\n"
            + "Trường 'docType' (chỉ điền khi intent=FBU_INFO, ngược lại để null):\n"
            + "- \"introduction\": giới thiệu trường, khoa, chuyên ngành, lịch sử, cơ sở vật chất\n"
            + "- \"department\": bộ môn, khoa/viện, học phần, môn học\n"
            + "- \"regulation\": quy chế, quy định, học phí, học bổng, thi cử, tốt nghiệp\n"
            + "- null: câu hỏi chung hoặc không thuộc nhóm trên\n\n"
            + "Chỉ trả về JSON hợp lệ, không giải thích. Ví dụ: {\"intent\":\"FBU_INFO\",\"docType\":\"regulation\"}";

    public QueryClassification classify(String query) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", CLASSIFIER_PROMPT));
        messages.add(Map.of("role", "user", "content", query));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", GroqChatClient.GROQ_MODEL);
        payload.put("messages", messages);
        payload.put("temperature", 0);
        payload.put("max_tokens", 32);

        try {
            String raw = groqClient.call(payload).trim();
            // Strip markdown code block nếu LLM wrap trong ```json ... ```
            raw = raw.replaceAll("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```", "$1").trim();

            JsonNode node = objectMapper.readTree(raw);
            String intent = node.path("intent").asText("FBU_INFO").toUpperCase(Locale.ROOT);
            boolean isFbuInfo = !"GENERAL_CHAT".equals(intent);

            String docType = null;
            JsonNode docTypeNode = node.path("docType");
            if (!docTypeNode.isNull() && !docTypeNode.isMissingNode()) {
                String dt = docTypeNode.asText("").toLowerCase(Locale.ROOT);
                if (dt.contains("introduction") || dt.contains("department") || dt.contains("regulation")) {
                    docType = dt;
                }
            }

            log.info("Query classified: intent={}, docType={}", intent, docType);
            return new QueryClassification(isFbuInfo, docType);

        } catch (Exception e) {
            log.warn("Query classifier failed ({}), falling back to FBU_INFO with no docType", e.getMessage());
            return new QueryClassification(true, null);
        }
    }
}
