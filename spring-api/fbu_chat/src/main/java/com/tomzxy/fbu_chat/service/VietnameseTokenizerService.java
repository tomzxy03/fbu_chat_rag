package com.tomzxy.fbu_chat.service;

import com.coccoc.Tokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VietnameseTokenizerService {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private volatile Tokenizer tokenizer;
    private volatile boolean tokenizerUnavailable;

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (!tokenizerUnavailable) {
            try {
                return getTokenizer().segmentToStringList(text).stream()
                        .map(this::normalizeToken)
                        .filter(token -> !token.isBlank())
                        .distinct()
                        .toList();
            } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError e) {
                tokenizerUnavailable = true;
                log.warn("CocCoc tokenizer native library is unavailable. Falling back to regex tokenizer: {}",
                        e.getMessage());
            } catch (RuntimeException e) {
                log.warn("CocCoc tokenizer failed for query. Falling back to regex tokenizer: {}", e.getMessage());
            }
        }

        return fallbackTokenize(text);
    }

    public String segmentForEmbedding(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return text;
        }
        return tokens.stream()
                .map(token -> token.replace(' ', '_'))
                .collect(java.util.stream.Collectors.joining(" "));
    }
    private Tokenizer getTokenizer() {
        Tokenizer current = tokenizer;
        if (current == null) {
            synchronized (this) {
                current = tokenizer;
                if (current == null) {
                    try {
                        System.loadLibrary("coccoc_tokenizer_jni");
                        log.info("Native library coccoc_tokenizer_jni loaded successfully");
                    } catch (UnsatisfiedLinkError e) {
                        log.error("Failed to load native library: {}", e.getMessage());
                    }
                    current = Tokenizer.getInstance();
                    tokenizer = current;
                    log.info("CocCoc tokenizer initialized via getInstance()");
                }
            }
        }
        return current;
    }

    private List<String> fallbackTokenize(String text) {
        String normalized = normalizeToken(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeToken(String token) {
        String normalized = Normalizer.normalize(token, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        normalized = NON_WORD.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized).replaceAll(" ").trim();
    }

}
