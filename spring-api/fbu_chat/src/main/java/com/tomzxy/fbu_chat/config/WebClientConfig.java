package com.tomzxy.fbu_chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout-seconds:60}")
    private int aiTimeoutSeconds;

    @Bean
    public String aiServiceBaseUrl() {
        return aiServiceUrl;
    }

    /** RestTemplate dùng để gọi AI service nội bộ (chunking + embedding). */
    @Bean
    public RestTemplate aiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(requestFactory(aiTimeoutSeconds));
        restTemplate.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    /**
     * RestTemplate dùng để gọi Groq external API.
     * Timeout 30s — đủ cho LLM inference, tránh treo request vô thời hạn.
     * Được inject vào RagService thay vì tạo inline.
     */
    @Bean(name = "groqRestTemplate")
    public RestTemplate groqRestTemplate() {
        return new RestTemplate(requestFactory(30));
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int ms = timeoutSeconds * 1000;
        factory.setConnectTimeout(5_000);   // 5s connect timeout
        factory.setReadTimeout(ms);
        return factory;
    }
}
