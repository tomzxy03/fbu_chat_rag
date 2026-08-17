package com.tomzxy.fbu_chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client duy nhất cho Groq API.
 * Chịu trách nhiệm: build headers, POST request, parse choices[0].message.content.
 * Không biết gì về business logic (RAG, classification, conversation).
 */
@Slf4j
@Service
@SuppressWarnings("unchecked")
public class GroqChatClient {

    static final String GROQ_MODEL = "llama-3.1-8b-instant";

    private final RestTemplate groqRestTemplate;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public GroqChatClient(@Qualifier("groqRestTemplate") RestTemplate groqRestTemplate) {
        this.groqRestTemplate = groqRestTemplate;
    }

    /**
     * Gửi payload đến Groq và trả về nội dung text của choices[0].message.content.
     *
     * @param payload Map hoàn chỉnh gồm "model", "messages", "temperature", "max_tokens"
     * @return nội dung phản hồi từ LLM
     */
    public String call(Map<String, Object> payload) {
        ensureApiKeyConfigured();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(groqApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map groqResp = groqRestTemplate.postForObject(
                "https://api.groq.com/openai/v1/chat/completions",
                new HttpEntity<>(payload, headers),
                Map.class);

        if (groqResp == null || !groqResp.containsKey("choices")) {
            throw new RuntimeException("Lỗi phản hồi từ Groq");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResp.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    public void ensureApiKeyConfigured() {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY chưa được cấu hình ở môi trường Spring Boot");
        }
    }
}
