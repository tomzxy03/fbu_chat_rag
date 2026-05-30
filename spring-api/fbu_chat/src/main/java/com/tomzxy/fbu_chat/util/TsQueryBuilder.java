package com.tomzxy.fbu_chat.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TsQueryBuilder v3
 *
 * Nâng cấp từ v2:
 * 1. Mở rộng STOPWORDS — lọc sạch đại từ/trợ từ giao tiếp ("bạn", "thể",
 * "tôi"...)
 * 2. HIGH_VALUE_TERMS chuyển sang từ đơn — khớp được với extractKeywords()
 * 3. COMPOUND_TERMS + Phrase Search (<->) — "học phí" → "học <-> phí" thay vì
 * tách lẻ
 *
 * Luồng xử lý của buildSmart():
 * Bước 1: Quét compound terms trong câu gốc (lowercase), emit phrase token (học
 * <-> phí)
 * Bước 2: Xóa compound đã dùng khỏi câu, tách từ đơn còn lại
 * Bước 3: Lọc stopwords, ưu tiên HIGH_VALUE_TERMS, giới hạn token
 * Bước 4: Trả về [andQuery, orQuery] tương thích RagService
 */
public class TsQueryBuilder {

    // ── Stopwords tiếng Việt mở rộng v3 ───────────────────────────────────────
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

            // Trường / đơn vị (hay xuất hiện nhưng không mang tính phân biệt)
            "trường", "viện", "phòng", "ban", "khoa",

            // ── V3: Đại từ / trợ từ giao tiếp (fix Bug 1) ──────────────────────
            "bạn", "tôi", "mình", "thể", "còn", "bị", "lại", "hơn", "liệu",
            "đây", "kia", "đấy", "nữa", "lắm", "mấy", "chỉ", "vẫn", "tất",
            "riêng", "chung", "dạ", "vâng", "em", "anh", "chị",
            "xin", "ơi", "nhờ", "nha", "hen", "đi");

    // ── Từ đơn có giá trị cao (fix Bug 2 — chỉ từ đơn, KHÔNG cụm ghép) ─────
    private static final Set<String> HIGH_VALUE_TERMS = Set.of(
            "phí", "lịch", "nghỉ", "lễ", "tết", "kỳ",
            "xét", "miễn", "giảm", "bổng", "luật",
            "thi", "điểm", "hạn", "nộp",
            "nghiệp", "tập", "ký",
            "deadline", "chứng", "nhận", "sơ",
            "thẻ", "vé", "lương", "thuế");

    // ── Compound terms cho Phrase Search (<->) ──────────────────────────────
    // Sort theo độ dài giảm dần (tránh "ăn non": "phòng đào tạo" quét trước "đào
    // tạo")
    // Mỗi entry là một cụm từ ghép tiếng Việt, sẽ được convert sang "từ1 <-> từ2"
    private static final List<String> COMPOUND_TERMS = List.of(
            // 3 từ (phải đứng trước 2 từ để tránh ăn non)
            "ban giám hiệu",
            "phòng đào tạo",
            "điểm rèn luyện",
            "hội đồng kỷ luật",
            "chương trình đào tạo",
            "chuẩn đầu ra",
            "đồ án tốt nghiệp",
            "khóa luận tốt nghiệp",

            // 2 từ — giáo dục
            "học phí",
            "học kỳ",
            "học bổng",
            "học phần",
            "học vụ",
            "kỷ luật",
            "tốt nghiệp",
            "thực tập",
            "đăng ký",
            "kết quả",
            "hồ sơ",
            "chứng nhận",
            "điều kiện",
            "thông tin",
            "thông báo",
            "quy chế",
            "quy trình",
            "quy định",
            "sinh viên",
            "giảng viên",
            "đào tạo",
            "tín chỉ",
            "niên khóa",
            "năm học",
            "xét tuyển",
            "khảo thí",
            "bảo lưu",
            "chuyển ngành",
            "miễn giảm",
            "đóng phí",
            "lịch thi",
            "lịch học",
            "nghỉ lễ",
            "nghỉ hè",
            "nghỉ tết",
            "đầu vào",
            "đầu ra",
            "rèn luyện",
            "cấp lại",
            "thẻ sinh viên",
            "bảng điểm",
            "văn bằng",
            "xử phạt",
            "cảnh cáo",
            "buộc thôi học",
            "đình chỉ",
            "gia hạn");

    public static String[] buildSmart(String query) {
        if (query == null || query.isBlank())
            return new String[] { null, null };

        // Chuẩn hóa chuỗi, giữ lại chữ và số
        String normalized = query.toLowerCase()
                .replaceAll("[\"'()\\[\\]{};:\\-/]", " ")
                .trim();

        List<String> phraseTokens = new ArrayList<>();

        // ── Bước 1: Quét compound terms ──────────────────────────────────────
        for (String compound : COMPOUND_TERMS) {
            // Sử dụng một trick nhỏ: thêm khoảng trắng 2 đầu để check bọc từ chính xác,
            // tránh việc hoán đổi làm dính các ký tự liền kề
            if ((" " + normalized + " ").contains(" " + compound + " ")) {
                // Convert "học phí" → "học <-> phí"
                String phraseExpr = Arrays.stream(compound.split("\\s+"))
                        .collect(Collectors.joining(" <-> "));
                phraseTokens.add(phraseExpr);

                // Thay thế chính xác cụm từ bằng khoảng trắng để bảo toàn biên cho các từ còn lại
                normalized = (" " + normalized + " ").replace(" " + compound + " ", " ").trim();
            }
        }

        // ── Bước 2: Tách từ đơn còn lại, lọc stopwords (Cho phép từ 1 ký tự như điểm A, B, C) ──
        List<String> singleKeywords = Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank())
                .filter(t -> !STOPWORDS.contains(t)) // "em", "bạn" 1 ký tự nhiễu đã nằm trong này rồi
                .filter(t -> t.matches("[\\p{L}\\p{N}]+"))
                .distinct()
                .collect(Collectors.toList());

        // ── Bước 3: Ưu tiên HIGH_VALUE_TERMS, sau đó theo độ dài ────────────
        singleKeywords.sort((a, b) -> {
            boolean aHigh = HIGH_VALUE_TERMS.contains(a);
            boolean bHigh = HIGH_VALUE_TERMS.contains(b);
            if (aHigh && !bHigh)
                return -1;
            if (!aHigh && bHigh)
                return 1;
            return Integer.compare(b.length(), a.length());
        });

        // ── Bước 4: Build AND query (phrases + top singles, tối đa 4 token) ─
        List<String> andTokens = new ArrayList<>(phraseTokens);
        int singleSlots = Math.max(0, 4 - andTokens.size());
        for (int i = 0; i < Math.min(singleSlots, singleKeywords.size()); i++) {
            andTokens.add(singleKeywords.get(i) + ":*");
        }

        // ── Bước 5: Build OR query (phrases + tất cả singles) ───────────────
        List<String> orTokens = new ArrayList<>(phraseTokens);
        for (String kw : singleKeywords) {
            orTokens.add(kw + ":*");
        }

        String andQuery = andTokens.isEmpty() ? null : String.join(" & ", andTokens);
        String orQuery = orTokens.isEmpty() ? null : String.join(" | ", orTokens);

        return new String[] { andQuery, orQuery };
    }

    /**
     * Build tsquery AND — backward compatible.
     */
    public static String buildAnd(String query) {
        return buildSmart(query)[0];
    }

    /**
     * Build tsquery OR — backward compatible.
     */
    public static String buildOr(String query) {
        return buildSmart(query)[1];
    }
}
