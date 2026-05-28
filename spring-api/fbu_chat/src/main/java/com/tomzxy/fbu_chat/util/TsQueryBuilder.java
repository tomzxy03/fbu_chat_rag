package com.tomzxy.fbu_chat.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convert câu hỏi tự nhiên → PostgreSQL tsquery an toàn.
 *
 * Dùng config 'simple' (không stemming) vì PostgreSQL không có Vietnamese stemmer.
 * 'simple' tokenize theo whitespace và lowercase — phù hợp cho tiếng Việt.
 *
 * Output: "học & bổng & điều & kiện" (AND query)
 * Các từ stop word ngắn và ký tự đặc biệt bị lọc để tránh tsquery syntax error.
 */
public class TsQueryBuilder {

    // Từ stop word tiếng Việt phổ biến — không có giá trị tìm kiếm
    private static final Set<String> STOP_WORDS = Set.of(
            "là", "và", "của", "có", "được", "trong", "cho", "với", "về",
            "các", "những", "một", "này", "đó", "khi", "nếu", "thì", "mà",
            "để", "từ", "theo", "tại", "bởi", "vì", "như", "hay", "hoặc",
            "không", "chưa", "đã", "sẽ", "đang", "bị", "hãy",
            "tôi", "bạn", "anh", "chị", "em", "họ", "chúng", "ta",
            "gì", "nào", "đâu", "sao", "thế", "vậy", "ạ", "nhé"
    );

    /**
     * Convert query string → tsquery string.
     * Trả về null nếu không có token hợp lệ nào (caller dùng pure vector search).
     */
    public static String build(String query) {
        if (query == null || query.isBlank()) return null;

        String[] tokens = query.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")  // bỏ ký tự đặc biệt
                .trim()
                .split("\\s+");

        String tsQuery = Arrays.stream(tokens)
                .filter(t -> t.length() >= 2)           // bỏ token quá ngắn
                .filter(t -> !STOP_WORDS.contains(t))   // bỏ stop words
                .map(t -> t + ":*")                     // prefix match: "học:*" match "học bổng"
                .collect(Collectors.joining(" & "));    // AND operator

        return tsQuery.isBlank() ? null : tsQuery;
    }
}
