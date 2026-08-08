package com.tomzxy.fbu_chat.service;

import com.coccoc.Tokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VietnameseTokenizerService {

    private static final Pattern NON_WORD   = Pattern.compile("[^\\p{L}\\p{N}\\s]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * NATIVE_UNAVAILABLE = true  → native lib không tồn tại trên máy này (UnsatisfiedLinkError /
     *                               ExceptionInInitializerError / NoClassDefFoundError).
     *                               Trạng thái permanent — không thử lại.
     *
     * Lưu ý: RuntimeException thoáng qua (timeout, bad input…) KHÔNG set flag này —
     *         sẽ thử lại ở request tiếp theo thay vì lock permanent sang fallback.
     */
    private volatile boolean nativeUnavailable = false;
    private volatile Tokenizer tokenizer;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Tokenize text bằng CocCoc. Fallback sang whitespace split nếu native lib không có.
     * Fallback chỉ xảy ra trên môi trường dev (không có coccoc-runtime mount).
     * Trên server production, nativeUnavailable sẽ luôn = false.
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (!nativeUnavailable) {
            try {
                return getTokenizer().segmentToStringList(text).stream()
                        .map(this::normalizeToken)
                        .filter(t -> !t.isBlank())
                        .distinct()
                        .toList();
            } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError e) {
                // Native lib không tồn tại → lock permanent, không thử lại
                nativeUnavailable = true;
                log.warn("[CocCoc] Native library unavailable — switching to whitespace fallback permanently. "
                        + "This is expected on dev machines without coccoc-runtime. Error: {}",
                        describeError(e));
            } catch (RuntimeException e) {
                // Lỗi thoáng qua (bad input, internal error) → log warn nhưng KHÔNG lock permanent
                log.warn("[CocCoc] Tokenizer threw RuntimeException for input '{}...'. "
                        + "Using fallback for this request only. Error: {}",
                        text.substring(0, Math.min(40, text.length())), describeError(e));
            }
        }

        return fallbackTokenize(text);
    }

    /**
     * Segment text cho embedding: join tokens bằng space, từ ghép dùng _ thay space nội bộ.
     * Ví dụ CocCoc: "học phí" → token "học phí" → "học_phí"
     * Ví dụ fallback: "học phí" → tokens ["học", "phí"] → "học phí" (chấp nhận được vì dev không ingest)
     */
    public String segmentForEmbedding(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return text;
        }
        return tokens.stream()
                .map(token -> token.replace(' ', '_'))
                .collect(Collectors.joining(" "));
    }

    /** Trả về true nếu CocCoc native lib đã được xác nhận không tồn tại trên máy này. */
    public boolean isNativeUnavailable() {
        return nativeUnavailable;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /** Double-checked locking để init Tokenizer singleton một lần duy nhất. */
    private Tokenizer getTokenizer() {
        Tokenizer current = tokenizer;
        if (current == null) {
            synchronized (this) {
                current = tokenizer;
                if (current == null) {
                    log.info("[CocCoc] Initializing tokenizer (first use)...");
                    current = Tokenizer.getInstance();
                    tokenizer = current;
                    log.info("[CocCoc] Tokenizer initialized successfully.");
                }
            }
        }
        return current;
    }

    /**
     * Fallback đơn giản: normalize + split whitespace.
     * Chỉ chạy trên dev — trên server luôn dùng CocCoc nên không cần complexity hơn.
     */
    private List<String> fallbackTokenize(String text) {
        String normalized = normalizeToken(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+")).stream()
                .filter(t -> !t.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeToken(String token) {
        String s = Normalizer.normalize(token, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        s = NON_WORD.matcher(s).replaceAll(" ");
        return WHITESPACE.matcher(s).replaceAll(" ").trim();
    }

    private String describeError(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        return root.getClass().getName() + (msg == null ? "" : ": " + msg);
    }
}
