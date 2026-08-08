package com.tomzxy.fbu_chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    /**
     * Thread pool riêng dùng cho batch ingest song song.
     * Giới hạn 3 threads để không làm ngợp AI service (vốn đã có semaphore=1)
     * và tránh OOM trên máy yếu.
     * Queue size 20 để không từ chối request khi pool đầy mà xếp hàng đợi.
     */
    @Bean(name = "ingestExecutor")
    public Executor ingestExecutor() {
        return new ThreadPoolExecutor(
                2,              // corePoolSize
                3,              // maximumPoolSize
                60L,            // keepAliveTime
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20),
                r -> {
                    Thread t = new Thread(r, "ingest-worker-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                }
        );
    }
}
