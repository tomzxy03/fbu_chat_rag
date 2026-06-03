package com.tomzxy.fbu_chat.util;

import com.tomzxy.fbu_chat.service.VietnameseTokenizerService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds PostgreSQL tsquery expressions from CocCoc-tokenized Vietnamese text.
 */
@Component
public class TsQueryBuilder {

    private final VietnameseTokenizerService tokenizerService;

    public TsQueryBuilder(VietnameseTokenizerService tokenizerService) {
        this.tokenizerService = tokenizerService;
    }

    private static final Set<String> STOPWORDS = Set.of(
            "và", "hoặc", "là", "của", "cho", "với", "trong", "có", "không",
            "được", "theo", "về", "tại", "từ", "đến", "này", "đó", "các",
            "những", "một", "hai", "ba", "khi", "nếu", "thì", "mà", "để",
            "như", "vì", "do", "bởi", "nên", "hay", "cũng", "đã", "sẽ",
            "đang", "rất", "quá", "bao", "nhiêu", "thế", "nào", "gì", "ở",
            "ra", "sao", "vậy", "nhỉ", "nhé", "ạ", "à", "ư", "thôi",
            "rồi", "xong", "chứ", "thật", "luôn",
            "biết", "muốn", "cần", "hỏi", "hãy", "giúp", "nói",
            "xem", "tìm", "hiểu", "làm", "phải",
            "trường", "viện", "phòng", "ban", "khoa",
            "bạn", "tôi", "mình", "thể", "còn", "bị", "lại", "hơn", "liệu",
            "đây", "kia", "đấy", "nữa", "lắm", "mấy", "chỉ", "vẫn", "tất",
            "riêng", "chung", "dạ", "vâng", "em", "anh", "chị",
            "xin", "ơi", "nhờ", "nha", "hen", "đi");

    private static final Set<String> HIGH_VALUE_TERMS = Set.of(
            "phí", "lịch", "nghỉ", "lễ", "tết", "kỳ",
            "xét", "miễn", "giảm", "bổng", "luật",
            "thi", "điểm", "hạn", "nộp",
            "nghiệp", "tập", "ký",
            "deadline", "chứng", "nhận", "sơ",
            "thẻ", "vé", "lương", "thuế");

    public String[] buildSmart(String query) {
        if (query == null || query.isBlank())
            return new String[] { null, null };

        List<String> phraseTokens = new ArrayList<>();
        List<String> singleKeywords = new ArrayList<>();

        for (String token : tokenizerService.tokenize(query)) {
            List<String> parts = splitToken(token);
            if (parts.isEmpty() || isStopwordToken(parts)) {
                continue;
            }

            if (parts.size() > 1) {
                phraseTokens.add(String.join(" <-> ", parts));
            } else {
                singleKeywords.add(parts.get(0));
            }
        }

        singleKeywords = singleKeywords.stream()
                .distinct()
                .collect(Collectors.toList());

        singleKeywords.sort((a, b) -> {
            boolean aHigh = HIGH_VALUE_TERMS.contains(a);
            boolean bHigh = HIGH_VALUE_TERMS.contains(b);
            if (aHigh && !bHigh)
                return -1;
            if (!aHigh && bHigh)
                return 1;
            return Integer.compare(b.length(), a.length());
        });

        List<String> andTokens = new ArrayList<>(phraseTokens);
        int singleSlots = Math.max(0, 4 - andTokens.size());
        for (int i = 0; i < Math.min(singleSlots, singleKeywords.size()); i++) {
            andTokens.add(singleKeywords.get(i) + ":*");
        }

        List<String> orTokens = new ArrayList<>(phraseTokens);
        for (String kw : singleKeywords) {
            orTokens.add(kw + ":*");
        }

        String andQuery = andTokens.isEmpty() ? null : String.join(" & ", andTokens);
        String orQuery = orTokens.isEmpty() ? null : String.join(" | ", orTokens);

        return new String[] { andQuery, orQuery };
    }

    public String buildAnd(String query) {
        return buildSmart(query)[0];
    }

    public String buildOr(String query) {
        return buildSmart(query)[1];
    }

    private List<String> splitToken(String token) {
        return Arrays.stream(token.split("\\s+"))
                .filter(part -> !part.isBlank())
                .filter(part -> part.matches("[\\p{L}\\p{N}]+"))
                .toList();
    }

    private boolean isStopwordToken(List<String> parts) {
        return parts.stream().allMatch(STOPWORDS::contains);
    }
}
