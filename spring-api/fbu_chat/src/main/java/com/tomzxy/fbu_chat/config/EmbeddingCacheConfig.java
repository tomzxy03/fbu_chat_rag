package com.tomzxy.fbu_chat.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class EmbeddingCacheConfig {

    /**
     * Cache cho query embedding vector.
     * - Key  : segmented query string (đã lowercase + tokenized)
     * - Value: List<Float> vector 384 chiều từ e5-small-v2
     * - TTL  : 45 phút — đủ dài để cache warm cho các query phổ biến trong 1 session
     * - Max  : 500 entries ≈ 500 × 384 × 4 bytes ≈ ~750 KB RAM — hoàn toàn chấp nhận được
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("queryEmbeddings");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(45, TimeUnit.MINUTES)
                        .recordStats()   // cho phép log hit/miss nếu cần debug
        );
        return manager;
    }
}
