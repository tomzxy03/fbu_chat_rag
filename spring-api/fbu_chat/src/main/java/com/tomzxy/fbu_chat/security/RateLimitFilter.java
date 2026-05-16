package com.tomzxy.fbu_chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter: 20 requests/minute per user.
 * Only applies to /api/chat (POST).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateInfo> ratemap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        // Only rate-limit chat POST
        if ("POST".equals(request.getMethod()) && request.getRequestURI().equals("/api/chat")) {
            String key = getClientKey(request);
            RateInfo info = ratemap.compute(key, (k, v) -> {
                long now = System.currentTimeMillis();
                if (v == null || now - v.windowStart > WINDOW_MS) {
                    return new RateInfo(now, new AtomicInteger(1));
                }
                v.count.incrementAndGet();
                return v;
            });

            if (info.count.get() > MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Bạn đã gửi quá nhiều tin nhắn. Vui lòng đợi 1 phút.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        // Use JWT username if available, else IP
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "user:" + auth.substring(7, Math.min(auth.length(), 30));
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static class RateInfo {
        final long windowStart;
        final AtomicInteger count;

        RateInfo(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
