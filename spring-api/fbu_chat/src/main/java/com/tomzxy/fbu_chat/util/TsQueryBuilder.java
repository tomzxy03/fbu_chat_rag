package com.tomzxy.fbu_chat.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TsQueryBuilder v2
 *
 * Chiến lược:
 *  1. Lọc stopwords + từ ngắn
 *  2. Giữ lại "từ khóa vàng" — từ hiếm, có giá trị tìm kiếm cao
 *  3. Build AND query từ keywords
 *  4. Nếu AND quá chặt → fallback OR với keyword quan trọng nhất
 *
 * Ví dụ:
 *  "lịch nghỉ lễ của trường ra sao"
 *    → AND: "lịch:* & nghỉ:* & lễ:*"   (bỏ: của, trường, ra, sao)
 *    → OR fallback: "lịch:* | nghỉ:* | lễ:*"
 */
public class TsQueryBuilder {

    // ── Stopwords tiếng Việt mở rộng───────────────
    private static final Set<String> STOPWORDS = Set.of(
        // Đại từ, trợ từ
        "và", "hoặc", "là", "của", "cho", "với", "trong", "có", "không",
        "được", "theo", "về", "tại", "từ", "đến", "này", "đó", "các",
        "những", "một", "hai", "ba", "khi", "nếu", "thì", "mà", "để",
        "như", "vì", "do", "bởi", "nên", "hay", "cũng", "đã", "sẽ",
        "đang", "rất", "quá", "bao", "nhiêu", "thế", "nào", "gì", "ở",

        // Câu hỏi / cảm thán
        "ra", "sao", "vậy", "nhỉ", "nhé", "ạ", "à", "ư", "thôi",
        "rồi", "xong", "chứ", "thật", "luôn",

        // Động từ phổ biến
        "biết", "muốn", "cần", "hỏi", "hãy", "giúp", "nói",
        "xem", "tìm", "hiểu", "làm", "phải",

        // Trường / đơn vị hay xuất hiện nhưng không mang tính phân biệt
        "trường", "viện", "phòng", "ban", "khoa"
    );

    // ── Từ có độ phân biệt cao — ưu tiên giữ lại ──────────────────────────
    // Những từ này xuất hiện ít trong corpus nhưng mang nhiều thông tin
    private static final Set<String> HIGH_VALUE_TERMS = Set.of(
        "học phí", "lịch", "nghỉ", "lễ", "tết", "kỳ", "học kỳ",
        "điều kiện", "xét", "miễn", "giảm", "học bổng", "kỷ luật",
        "thi", "điểm", "kết quả", "tốt nghiệp", "thực tập", "đăng ký",
        "deadline", "hạn", "nộp", "hồ sơ", "giấy", "chứng nhận"
    );

    /**
     * Build tsquery AND — dùng làm primary search.
     * Chỉ giữ từ có giá trị tìm kiếm, bỏ stopwords.
     *
     * @return null nếu không có từ nào có nghĩa
     */
    public static String buildAnd(String query) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) return null;

        return keywords.stream()
                .map(t -> t + ":*")
                .collect(Collectors.joining(" & "));
    }

    /**
     * Build tsquery OR — dùng làm fallback khi AND không có kết quả.
     * Rộng hơn, bắt được nhiều chunk hơn.
     */
    public static String buildOr(String query) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) return null;

        return keywords.stream()
                .map(t -> t + ":*")
                .collect(Collectors.joining(" | "));
    }

    /**
     * Build tsquery thông minh:
     * - Nếu ≤ 2 keywords → dùng AND (đủ chặt)
     * - Nếu 3-4 keywords → AND với top 3 quan trọng nhất
     * - Nếu ≥ 5 keywords → AND với top 3 + fallback OR nếu cần
     *
     * Trả về [andQuery, orQuery] để caller quyết định dùng cái nào.
     */
    public static String[] buildSmart(String query) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) return new String[]{null, null};

        // OR luôn dùng tất cả keywords
        String orQuery = keywords.stream()
                .map(t -> t + ":*")
                .collect(Collectors.joining(" | "));

        // AND chỉ dùng top keywords (tối đa 3) để tránh quá chặt
        List<String> topKeywords = prioritize(keywords).stream()
                .limit(3)
                .collect(Collectors.toList());

        String andQuery = topKeywords.stream()
                .map(t -> t + ":*")
                .collect(Collectors.joining(" & "));

        return new String[]{andQuery, orQuery};
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return List.of();

        return Arrays.stream(
                query.toLowerCase()
                     .replaceAll("[\"'()\\[\\]{};:\\-/]", " ")
                     .trim()
                     .split("\\s+")
                )
                .filter(t -> t.length() >= 2)
                .filter(t -> !STOPWORDS.contains(t))
                .filter(t -> t.matches("[\\p{L}\\p{N}]+"))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Sắp xếp keywords: HIGH_VALUE_TERMS lên đầu, còn lại theo độ dài giảm dần.
     * Từ dài thường cụ thể hơn → ưu tiên hơn.
     */
    private static List<String> prioritize(List<String> keywords) {
        return keywords.stream()
                .sorted((a, b) -> {
                    boolean aHigh = HIGH_VALUE_TERMS.contains(a);
                    boolean bHigh = HIGH_VALUE_TERMS.contains(b);
                    if (aHigh && !bHigh) return -1;
                    if (!aHigh && bHigh) return 1;
                    return Integer.compare(b.length(), a.length()); // dài hơn → trước
                })
                .collect(Collectors.toList());
    }
}
