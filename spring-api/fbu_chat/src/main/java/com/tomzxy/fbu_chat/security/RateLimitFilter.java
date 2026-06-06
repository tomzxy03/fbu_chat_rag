package com.tomzxy.fbu_chat.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting cho POST /api/chat bằng Bucket4j (in-memory, token bucket).
 *
 * - Anonymous (no valid JWT): 10 req/phút theo IP
 * - Authenticated (valid JWT): 30 req/phút theo username
 *
 * Vì filter này chạy TRƯỚC JwtFilter, SecurityContextHolder chưa có auth.
 * Dùng JwtUtil để parse token thủ công; fallback về IP nếu token invalid.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int ANONYMOUS_LIMIT = 10;
    private static final int AUTHENTICATED_LIMIT = 30;
    private static final int REGISTER_LIMIT = 5; // 5 lần đăng ký/giờ/IP — chống spam
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    private final JwtUtil jwtUtil;

    /** Bucket per client key — key format: "ip:{addr}" hoặc "user:{username}" */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Rate limit cho POST /api/chat (theo user/IP)
        if ("POST".equals(method) && "/api/chat".equals(uri)) {
            String clientKey = resolveClientKey(request);
            boolean isAuthenticated = clientKey.startsWith("user:");
            Bucket bucket = buckets.computeIfAbsent(clientKey, k -> buildBucket(isAuthenticated));

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
                int limit = isAuthenticated ? AUTHENTICATED_LIMIT : ANONYMOUS_LIMIT;
                String message = String.format(
                        "Bạn đã gửi quá nhiều tin nhắn (%d/%d phút). Vui lòng đợi %d giây trước khi gửi tiếp.",
                        limit, 1, waitSeconds);
                log.warn("Chat rate limit exceeded for key={}, waitSeconds={}", clientKey, waitSeconds);
                writeRateLimitResponse(response, message);
                return;
            }

            chain.doFilter(request, response);
            return;
        }

        // Rate limit cho POST /api/auth/register (5 lần/giờ/IP — chống spam tài khoản)
        if ("POST".equals(method) && "/api/auth/register".equals(uri)) {
            String ip = request.getRemoteAddr();
            String registerKey = "register:" + ip;
            Bucket bucket = buckets.computeIfAbsent(registerKey, k -> buildRegisterBucket());

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long waitMinutes = Math.max(1, probe.getNanosToWaitForRefill() / 60_000_000_000L);
                String message = String.format(
                        "Quá nhiều yêu cầu đăng ký từ địa chỉ này. Vui lòng thử lại sau %d phút.",
                        waitMinutes);
                log.warn("Register rate limit exceeded for ip={}", ip);
                writeRateLimitResponse(response, message);
                return;
            }

            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        // Escape quotes trong message để tránh JSON injection
        String safeMessage = message.replace("\"", "'");
        response.getWriter().write(String.format(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\"}",
                safeMessage));
    }

    /**
     * Xác định client key:
     * - Nếu có Bearer token hợp lệ → "user:{username}"
     * - Ngược lại → "ip:{remoteAddr}"
     */
    private String resolveClientKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null && !username.isBlank()) {
                    return "user:" + username;
                }
            } catch (Exception e) {
                // Token invalid hoặc expired — fallback về IP
                log.debug("JWT parse failed in RateLimitFilter, falling back to IP: {}", e.getMessage());
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private Bucket buildBucket(boolean authenticated) {
        int capacity = authenticated ? AUTHENTICATED_LIMIT : ANONYMOUS_LIMIT;
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket buildRegisterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REGISTER_LIMIT)
                .refillIntervally(REGISTER_LIMIT, REGISTER_WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
