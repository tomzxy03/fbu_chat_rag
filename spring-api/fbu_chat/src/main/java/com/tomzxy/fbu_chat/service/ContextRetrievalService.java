package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.entity.ParentChunk;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
import com.tomzxy.fbu_chat.util.TsQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chịu trách nhiệm toàn bộ pipeline tìm kiếm context từ vector DB:
 * hybrid search (AND → OR → pure vector), filter metadata/scope, build context string.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextRetrievalService {

    private static final double SIMILARITY_THRESHOLD = 0.70;
    private static final double HYBRID_VECTOR_THRESHOLD = 0.40;
    private static final int DEFAULT_TOP_K = 8;

    private final DocumentChunkRepository docRepo;
    private final ParentChunkRepository parentChunkRepo;
    private final TsQueryBuilder tsQueryBuilder;

    /**
     * Chạy 3-pass hybrid search: AND → OR → pure vector.
     * Trả về danh sách chunk đã qua metadata filter, sẵn sàng đưa vào LLM.
     */
    public List<ChunkResult> search(String vectorStr, ChatRequest request) {
        return search(vectorStr, request, Set.of());
    }

    /**
     * Overload với excludeParentIds — dùng cho follow-up "xem thêm / còn thông tin khác không?"
     * để tránh trả lại chunk từ cùng parent block đã dùng ở lượt trước.
     */
    public List<ChunkResult> search(String vectorStr, ChatRequest request, Set<UUID> excludeParentIds) {
        int topK = request.getTopK() != null ? request.getTopK() : DEFAULT_TOP_K;
        // Request thêm candidate khi có exclude để bù vào các chunk bị loại
        int candidateK = topK * 5 + excludeParentIds.size() * 2;

        String[] tsQueries = tsQueryBuilder.buildSmart(request.getQuery());
        String andQuery = tsQueries[0];
        String orQuery = tsQueries[1];

        log.info("Searching (topK={}, threshold={}, AND={}, year={}, docType={}, excludeParents={})",
                topK, SIMILARITY_THRESHOLD, andQuery, request.getYear(), request.getDocType(),
                excludeParentIds.size());

        List<ChunkResult> results = List.of();

        // Pass 1: hybrid AND (precision cao)
        if (andQuery != null) {
            try {
                List<ChunkResult> andResults = docRepo.hybridSearch(
                        vectorStr, andQuery, topK, candidateK, HYBRID_VECTOR_THRESHOLD);
                results = filterByMetadata(andResults, request, excludeParentIds);
                log.info("Pass 1 (AND) after metadata filter: {} results", results.size());
            } catch (Exception e) {
                log.warn("Hybrid AND failed: {}", e.getMessage());
            }
        }

        // Pass 2: hybrid OR (recall cao hơn nếu Pass 1 không đủ)
        if (results.size() < 2 && orQuery != null) {
            log.info("Pass 1 insufficient, trying OR fallback...");
            try {
                List<ChunkResult> orResults = docRepo.hybridSearch(
                        vectorStr, orQuery, topK, candidateK, HYBRID_VECTOR_THRESHOLD);
                List<ChunkResult> filteredOr = filterByMetadata(orResults, request, excludeParentIds);
                log.info("Pass 2 (OR) after metadata filter: {} results", filteredOr.size());
                if (filteredOr.size() > results.size()) {
                    results = filteredOr;
                }
            } catch (Exception e) {
                log.warn("Hybrid OR failed: {}", e.getMessage());
            }
        }

        // Pass 3: pure vector fallback
        if (results.size() < 2) {
            log.info("Hybrid insufficient, falling back to pure vector...");
            List<ChunkResult> vectorResults = docRepo.findTopRelatedContexts(
                    vectorStr, topK, SIMILARITY_THRESHOLD);
            List<ChunkResult> filteredVector = filterByMetadata(vectorResults, request, excludeParentIds);
            log.info("Pass 3 (vector) after metadata filter: {} results", filteredVector.size());
            if (filteredVector.size() > results.size()) {
                results = filteredVector;
            }
        }

        return results;
    }

    /**
     * Ghép nội dung các chunk (ưu tiên parent content) thành string context cho LLM.
     *
     * Chiến lược:
     * - Parent <= PARENT_CONTENT_MAX_CHARS: gửi toàn bộ parent (context đầy đủ)
     * - Parent > PARENT_CONTENT_MAX_CHARS: gửi child content đã match thay vì parent.
     *   Lý do: parent quá dài thường là danh sách lặp cấu trúc (giảng viên, đối tác...) —
     *   gửi toàn bộ gây nhiễu LLM. Child đã được chọn vì semantic match, đủ context.
     */
    private static final int PARENT_CONTENT_MAX_CHARS = 4000;

    public String buildContextString(List<ChunkResult> contexts) {
        List<UUID> parentIds = contexts.stream()
                .map(ChunkResult::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, ParentChunk> parentMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            for (ParentChunk p : parentChunkRepo.findAllById(parentIds)) {
                parentMap.put(p.getId(), p);
            }
        }

        Set<UUID> usedParentIds = new HashSet<>();
        Set<String> usedFallbackKeys = new HashSet<>();
        List<String> contextParts = new ArrayList<>();

        for (ChunkResult c : contexts) {
            UUID pid = c.getParentId();
            String yearLabel = c.getYear() != null ? String.valueOf(c.getYear()) : "Không rõ năm";
            String sectionLabel = (c.getSection() != null && !c.getSection().isBlank())
                    ? " | Mục: " + c.getSection() : "";

            if (pid != null && parentMap.containsKey(pid)) {
                ParentChunk parent = parentMap.get(pid);
                String parentText = parent.getContent();

                if (parentText != null && parentText.length() > PARENT_CONTENT_MAX_CHARS) {
                    // Parent quá dài (danh sách lặp cấu trúc) → dùng child đã match
                    // Mỗi child đều được thêm vì chúng là các entries khác nhau trong danh sách
                    String fallbackKey = c.getSourceFile() + ":" + (c.getContent() != null ? c.getContent().hashCode() : "");
                    if (usedFallbackKeys.add(fallbackKey)) {
                        log.debug("Parent too large ({} chars) for '{}' — using matched child instead",
                                parentText.length(), c.getSourceFile());
                        contextParts.add(String.format("[Nguồn: %s | Năm: %s%s]\n%s",
                                c.getSourceFile(), yearLabel, sectionLabel, c.getContent()));
                    }
                } else if (usedParentIds.add(pid)) {
                    contextParts.add(String.format("[Nguồn: %s | Năm: %s%s]\n%s",
                            c.getSourceFile(), yearLabel, sectionLabel, parentText));
                }
            } else {
                if (pid != null) {
                    log.warn("parentId {} của file {} không tồn tại trong parent_chunks — hạ cấp sang Child Content.",
                            pid, c.getSourceFile());
                }
                String fallbackKey = c.getSourceFile() + ":"
                        + (c.getContent() != null ? c.getContent().hashCode() : "");
                if (usedFallbackKeys.add(fallbackKey)) {
                    contextParts.add(String.format("[Nguồn: %s | Năm: %s%s (Mẩu tin nhỏ)]\n%s",
                            c.getSourceFile(), yearLabel, sectionLabel, c.getContent()));
                }
            }
        }

        return String.join("\n\n---\n\n", contextParts);
    }

    // ── Metadata + scope filter ──────────────────────────────────────────────

    private List<ChunkResult> filterByMetadata(List<ChunkResult> results, ChatRequest request) {
        return filterByMetadata(results, request, Set.of());
    }

    private List<ChunkResult> filterByMetadata(List<ChunkResult> results, ChatRequest request,
                                                Set<UUID> excludeParentIds) {
        if (results == null || results.isEmpty()) return List.of();

        // Loại chunk thuộc parent block đã dùng ở lượt trước (follow-up "xem thêm")
        List<ChunkResult> afterExclude = excludeParentIds.isEmpty() ? results : results.stream()
                .filter(c -> c.getParentId() == null || !excludeParentIds.contains(c.getParentId()))
                .collect(Collectors.toList());

        if (!excludeParentIds.isEmpty()) {
            log.info("Excluded {}/{} chunks via usedParentIds filter",
                    results.size() - afterExclude.size(), results.size());
        }

        if (afterExclude.isEmpty()) return List.of();

        if (request.getYear() == null && request.getDocType() == null) {
            return filterByQueryScope(afterExclude, request.getQuery());
        }

        List<ChunkResult> filtered = afterExclude.stream()
                .filter(c -> request.getYear() == null || c.getYear() == null
                        || request.getYear().equals(c.getYear()))
                .filter(c -> request.getDocType() == null || c.getDocType() == null
                        || request.getDocType().equalsIgnoreCase(c.getDocType()))
                .collect(Collectors.toList());

        // Nếu filter quá nghiêm (loại >50%), giữ lại toàn bộ để không mất context
        if (!afterExclude.isEmpty() && filtered.size() < afterExclude.size() / 2) {
            log.warn("Metadata filter dropped {}/{} chunks — keeping unfiltered candidates.",
                    afterExclude.size() - filtered.size(), afterExclude.size());
            return filterByQueryScope(afterExclude, request.getQuery());
        }

        return filterByQueryScope(filtered, request.getQuery());
    }

    private List<ChunkResult> filterByQueryScope(List<ChunkResult> results, String query) {
        List<String> scopeTerms = inferQueryScopeTerms(query);
        if (scopeTerms.isEmpty()) return results;

        List<ChunkResult> scoped = results.stream()
                .filter(c -> {
                    String haystack = normalizeForScope(String.join(" ",
                            Objects.toString(c.getSourceFile(), ""),
                            Objects.toString(c.getContent(), ""),
                            Objects.toString(c.getSection(), "")));
                    return scopeTerms.stream().anyMatch(haystack::contains);
                })
                .collect(Collectors.toList());

        if (!scoped.isEmpty()) {
            log.info("Query scope filter kept {}/{} chunks for terms {}", scoped.size(), results.size(), scopeTerms);
            return scoped;
        }

        log.warn("Query scope filter matched no chunks for terms {} — keeping pre-filter candidates.", scopeTerms);
        return results;
    }

    private List<String> inferQueryScopeTerms(String query) {
        String q = normalizeForScope(query);
        if (q.contains("cong nghe thong tin") || q.contains("cntt") || q.contains("tin ung dung")) {
            return List.of("viencongnghethongtin", "cong nghe thong tin", "cntt", "tin ung dung");
        }
        if (q.contains("ke toan") || q.contains("kiem toan")) {
            return List.of("vienketoankiemtoan", "ke toan", "kiem toan");
        }
        if (q.contains("quan tri kinh doanh") || q.contains("kinh doanh")) {
            return List.of("vienquantrikinhdoanh", "quan tri kinh doanh");
        }
        if (q.contains("tai chinh") || q.contains("ngan hang")) {
            return List.of("taichinhnganhang", "tai chinh", "ngan hang");
        }
        if (q.contains("ngoai ngu")) {
            return List.of("ngoaingu", "ngoai ngu");
        }
        return List.of();
    }

    public String normalizeForScope(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
