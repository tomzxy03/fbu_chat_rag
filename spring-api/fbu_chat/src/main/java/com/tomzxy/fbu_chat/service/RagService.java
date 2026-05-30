package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.entity.Conversation;
import com.tomzxy.fbu_chat.entity.Message;
import com.tomzxy.fbu_chat.entity.ParentChunk;
import com.tomzxy.fbu_chat.repository.ConversationRepository;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.MessageRepository;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
import com.tomzxy.fbu_chat.util.TsQueryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class RagService {

    // Cosine distance threshold: chunk có distance > 0.65 bị loại
    // 0.65 phù hợp hơn 0.5 cho chunk dài 1200 tokens — vector bị "pha loãng"
    private static final double SIMILARITY_THRESHOLD = 0.65;

    // Số chunk tối đa lấy về — đủ để cover nhiều điều khoản liên quan
    private static final int DEFAULT_TOP_K = 8;

    // Số lượt hội thoại gần nhất đưa vào context (để LLM hiểu follow-up)
    private static final int HISTORY_WINDOW = 4;

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final DocumentChunkRepository docRepo;
    private final ParentChunkRepository parentChunkRepo;
    private final ObjectMapper objectMapper;
    // Reuse RestTemplate cho Groq — tránh tạo mới mỗi request
    private final RestTemplate groqRestTemplate = new RestTemplate();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public RagService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            ConversationRepository conversationRepo,
            MessageRepository messageRepo,
            DocumentChunkRepository docRepo,
            ParentChunkRepository parentChunkRepo,
            ObjectMapper objectMapper) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.docRepo = docRepo;
        this.parentChunkRepo = parentChunkRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        // 1. Conversation: chỉ tạo/lưu khi user đã login
        Conversation conversation = null;
        if (userId != null) {
            if (request.getConversationId() != null) {
                UUID convId = UUID.fromString(request.getConversationId());
                // Ownership check: chỉ cho phép nếu conversation thuộc về user hiện tại
                conversation = conversationRepo.findByIdAndUserId(convId, userId)
                        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                                "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
            } else {
                conversation = createConversation(request.getQuery(), userId);
            }
            // Lưu user message sẽ được thực hiện SAU khi fetch history (tránh duplicate
            // trong prompt)
        }

        // 3. Tìm vector cho query từ AI Service
        log.info("Encoding query using AI Service...");
        EmbeddingRequest embReq = new EmbeddingRequest();
        embReq.setTexts(List.of(request.getQuery()));
        embReq.setMode("query"); // e5-small-v2: câu hỏi dùng prefix "query:"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(embReq, headers);

        ResponseEntity<EmbeddingResponse> embResponse = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings", HttpMethod.POST, entity, EmbeddingResponse.class);

        if (embResponse.getBody() == null || embResponse.getBody().getEmbeddings().isEmpty()) {
            throw new RuntimeException("Lỗi sinh Embedding cho câu hỏi");
        }

        List<Float> queryVector = embResponse.getBody().getEmbeddings().get(0);
        String vectorStr = "[" + queryVector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";

        int topK = request.getTopK() != null ? request.getTopK() : DEFAULT_TOP_K;
        int candidateK = topK * 3;

        // Build cả AND và OR query từ TsQueryBuilder v2
        String[] tsQueries = TsQueryBuilder.buildSmart(request.getQuery());
        String andQuery = tsQueries[0];
        String orQuery = tsQueries[1];

        log.info("Searching (topK={}, threshold={}, AND={}, year={}, docType={})",
                topK, SIMILARITY_THRESHOLD, andQuery,
                request.getYear(), request.getDocType());

        List<ChunkResult> topContexts = List.of();

        // Pass 1: Hybrid AND (chặt chẽ, chính xác tuyệt đối)
        if (andQuery != null) {
            try {
                List<ChunkResult> andResults = docRepo.hybridSearch(vectorStr, andQuery, topK, candidateK);
                topContexts = filterByMetadata(andResults, request); // Áp bộ lọc ngay lập tức
                log.info("Pass 1 (AND) after metadata filter: {} results", topContexts.size());
            } catch (Exception e) {
                log.warn("Hybrid AND failed: {}", e.getMessage());
            }
        }

        // Pass 2: Hybrid OR Fallback (Kích hoạt nếu Pass 1 quá ít kết quả hợp lệ)
        if (topContexts.size() < 2 && orQuery != null) {
            log.info("Pass 1 insufficient, trying OR fallback...");
            try {
                List<ChunkResult> orResults = docRepo.hybridSearch(vectorStr, orQuery, topK, candidateK);
                List<ChunkResult> filteredOr = filterByMetadata(orResults, request);
                log.info("Pass 2 (OR) after metadata filter: {} results", filteredOr.size());

                if (filteredOr.size() > topContexts.size()) {
                    topContexts = filteredOr;
                }
            } catch (Exception e) {
                log.warn("Hybrid OR failed: {}", e.getMessage());
            }
        }

        // Pass 3: Pure Vector Fallback (Dựa vào AI tìm ngữ nghĩa nếu FTS không khớp từ
        // khóa)
        if (topContexts.size() < 2) {
            log.info("Hybrid insufficient, falling back to pure vector...");
            List<ChunkResult> vectorResults = docRepo.findTopRelatedContexts(vectorStr, topK, SIMILARITY_THRESHOLD);
            List<ChunkResult> filteredVector = filterByMetadata(vectorResults, request);
            log.info("Pass 3 (vector) after metadata filter: {} results", filteredVector.size());

            if (filteredVector.size() > topContexts.size()) {
                topContexts = filteredVector;
            }
        }

        // Pass 4: Vét cạn cuối cùng (Hạ threshold điểm số xuống tối đa nếu vẫn trống
        // rỗng)
        if (topContexts.isEmpty()) {
            log.info("No results at threshold {}, relaxing to 1.0...", SIMILARITY_THRESHOLD);
            List<ChunkResult> relaxedResults = docRepo.findTopRelatedContexts(vectorStr, topK, 1.0);
            topContexts = filterByMetadata(relaxedResults, request);
            log.info("Pass 4 (relaxed vector) after metadata filter: {} results", topContexts.size());
        }

        log.info("Final chunks for LLM: {}", topContexts.size());

        String contextText;
        if (topContexts.isEmpty()) {
            contextText = "Không tìm thấy tài liệu liên quan.";
        } else {
            // Fetch parent content cho các child có parentId
            contextText = buildContextWithParents(topContexts);
        }

        log.info("=== CHUNKS SENT TO LLM ({}) ===", topContexts.size());
        for (int i = 0; i < topContexts.size(); i++) {
            ChunkResult c = topContexts.get(i);
            log.info("[{}] {} | len={} | preview={}",
                    i + 1,
                    c.getSourceFile(),
                    c.getContent() != null ? c.getContent().length() : 0,
                    c.getContent() != null
                            ? c.getContent().substring(0, Math.min(80, c.getContent().length()))
                                    .replace("\n", " ")
                            : "NULL");
        }
        log.info("================================");

        // 5. Gọi Groq LLM API
        String systemPrompt = "Bạn là trợ lý AI chuyên nghiệp của trường Đại học Tài chính - Ngân hàng Hà Nội (FBU).\n" +
            "Nhiệm vụ của bạn là trả lời câu hỏi của sinh viên và giảng viên dựa vào CONTEXT (Ngữ cảnh) được cung cấp.\n\n" +
            "QUY TẮC CHÍ MẠNG:\n" +
            "1. CHỈ trả lời dựa trên CONTEXT được cung cấp. Tuyệt đối không tự suy diễn hoặc dùng kiến thức bên ngoài.\n" +
            "2. Nếu thông tin không có trong CONTEXT, hãy trả lời lịch sự: 'Hệ thống không tìm thấy thông tin này trong tài liệu nội bộ của FBU.'\n" +
            "3. Trả lời bằng tiếng Việt, ngắn gọn, đi thẳng vào vấn đề, phân tách các ý bằng dấu gạch đầu dòng rõ ràng.\n" +
            "4. QUẢN LÝ LỊCH SỬ HỘI THOẠI: Đọc kỹ các câu trả lời trước đó trong lịch sử. KHÔNG lặp lại thông tin đã cung cấp. Chỉ bổ sung thông tin MỚI chưa được đề cập.\n" +
            "5. Nếu câu hỏi là dạng tiếp diễn (ví dụ: 'còn gì nữa không?', 'còn gì khác không?') mà tất cả thông tin trong CONTEXT đã được bạn liệt kê ở các câu trả lời trước, hãy nói rõ: 'Tôi đã cung cấp toàn bộ thông tin tìm thấy trong tài liệu nội bộ liên quan đến vấn đề này.'\n\n" +
        
            "QUY TẮC ĐỊNH DẠNG NGUỒN TRÍCH DẪN:\n" +
            "- Tuyệt đối KHÔNG tự viết chữ 'Nguồn:' hoặc liệt kê danh sách file ở cuối câu trả lời (vì hệ thống đã có bộ lọc tự động hiển thị nguồn riêng).\n" +
            "- Thay vào đó, hãy trích dẫn trực tiếp tên file ngay trong câu văn nếu cần thiết (Ví dụ: 'Theo Quyết định số 115, quy trình kỷ luật...');";

        String userPrompt = "HỘI THOẠI TRƯỚC ĐÓ (Nếu có):\n" + (request.getHistory() != null ? request.getHistory() : "Trống") + "\n\n" +
            "CONTEXT TỪ TÀI LIỆU FBU:\n" + contextText + "\n\n" +
            "CÂU HỎI HIỆN TẠI: " + request.getQuery() + "\n\n" +
            "Trả lời (Tuân thủ tuyệt đối quy tắc định dạng nguồn):";

        log.info("Calling Groq LLM Generator...");
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY chưa được cấu hình ở môi trường Spring Boot");
        }

        HttpHeaders groqHeaders = new HttpHeaders();
        groqHeaders.setBearerAuth(groqApiKey);
        groqHeaders.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> groqMsg1 = new HashMap<>();
        groqMsg1.put("role", "system");
        groqMsg1.put("content", systemPrompt);

        // Thêm lịch sử hội thoại gần nhất để LLM hiểu context follow-up
        List<Map<String, Object>> groqMessages = new java.util.ArrayList<>();
        groqMessages.add(groqMsg1);
        if (conversation != null) {
            // Fetch history TRƯỚC khi lưu user message hiện tại → tránh duplicate trong
            // prompt
            List<Message> recentHistory = messageRepo
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            // Lấy HISTORY_WINDOW * 2 messages gần nhất (user + assistant pairs)
            int fromIdx = Math.max(0, recentHistory.size() - HISTORY_WINDOW * 2);
            for (Message histMsg : recentHistory.subList(fromIdx, recentHistory.size())) {
                Map<String, Object> hm = new HashMap<>();
                hm.put("role", histMsg.getRole());
                hm.put("content", histMsg.getContent());
                groqMessages.add(hm);
            }

            // Lưu user message SAU khi đã fetch history xong
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);
        }

        Map<String, Object> groqMsg2 = new HashMap<>();
        groqMsg2.put("role", "user");
        groqMsg2.put("content", userPrompt);
        groqMessages.add(groqMsg2);

        Map<String, Object> groqPayload = new HashMap<>();
        groqPayload.put("model", "llama-3.3-70b-versatile");
        groqPayload.put("messages", groqMessages);
        groqPayload.put("temperature", 0.3);
        groqPayload.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqPayload, groqHeaders);
        Map groqResp = groqRestTemplate.postForObject("https://api.groq.com/openai/v1/chat/completions", groqEntity,
                Map.class);

        if (groqResp == null || !groqResp.containsKey("choices")) {
            throw new RuntimeException("Lỗi phản hồi từ Groq");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResp.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String answer = (String) message.get("content");

        // 6. Build sources object to save
        List<Map<String, Object>> sources = topContexts.stream().map(c -> {
            Map<String, Object> s = new HashMap<>();
            s.put("file", c.getSourceFile());
            s.put("year", c.getYear());
            s.put("doc_type", c.getDocType());
            return s;
        }).collect(Collectors.toList());

        // 7. Lưu message assistant (chỉ khi logged in)
        UUID messageId = null;
        if (conversation != null) {
            String sourcesJson = "";
            try {
                sourcesJson = objectMapper.writeValueAsString(sources);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize sources", e);
            }
            Message assistantMsg = Message.builder()
                    .conversation(conversation)
                    .role("assistant")
                    .content(answer)
                    .sources(sourcesJson)
                    .build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();

            // Cập nhật updatedAt của conversation để reflect hoạt động chat mới nhất
            // @PreUpdate trên entity sẽ tự set updatedAt = now()
            conversationRepo.save(conversation);
        }

        // 8. Build response — deduplicate sources theo filename
        List<ChatResponse.SourceInfo> sourceInfos = sources.stream()
                .collect(Collectors.toMap(
                        s -> (String) s.get("file"),
                        s -> ChatResponse.SourceInfo.builder()
                                .file((String) s.get("file"))
                                .year(s.get("year") instanceof Integer ? (Integer) s.get("year") : null)
                                .docType((String) s.get("doc_type"))
                                .build(),
                        (existing, duplicate) -> existing // giữ entry đầu tiên nếu trùng
                ))
                .values()
                .stream()
                .toList();

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(sourceInfos)
                .build();
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Lấy lịch sử hội thoại với ownership check.
     * Ném AccessDeniedException nếu conversation không thuộc về userId.
     */
    public List<Message> getHistoryForUser(UUID conversationId, String userId) {
        conversationRepo.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<Conversation> getAllConversations() {
        return conversationRepo.findAllByOrderByUpdatedAtDesc();
    }

    public List<Conversation> getUserConversations(String userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    private Conversation createConversation(String query, String userId) {
        String title = query.length() > 50 ? query.substring(0, 50) + "..." : query;
        Conversation conv = Conversation.builder()
                .userId(userId)
                .title(title)
                .build();
        return conversationRepo.save(conv);
    }

    private List<ChunkResult> filterByMetadata(List<ChunkResult> results, ChatRequest request) {
        if (results == null || results.isEmpty())
            return List.of();

        // Không có filter từ phía client -> giữ nguyên danh sách gốc
        if (request.getYear() == null && request.getDocType() == null)
            return results;

        List<ChunkResult> filtered = results.stream()
                .filter(c -> request.getYear() == null
                        || c.getYear() == null // Hàng rào bảo hiểm: chunk thiếu metadata -> KHÔNG loại
                        || request.getYear().equals(c.getYear()))
                .filter(c -> request.getDocType() == null
                        || c.getDocType() == null // Hàng rào bảo hiểm: chunk thiếu loại văn bản -> KHÔNG loại
                        || request.getDocType().equalsIgnoreCase(c.getDocType()))
                .collect(Collectors.toList());

        // Hệ thống cảnh báo giám sát (Data Observability): Nếu bộ lọc làm rơi rụng quá
        // nửa số chunks
        if (!results.isEmpty() && filtered.size() < results.size() / 2) {
            log.warn("🚨 Metadata filter dropped {}/{} chunks — Check your DB metadata injection quality!",
                    results.size() - filtered.size(), results.size());
        }

        return filtered;
    }

    private String buildContextWithParents(List<ChunkResult> contexts) {
        // 1. Thu thập danh sách parentIds duy nhất
        List<UUID> parentIds = contexts.stream()
                .map(ChunkResult::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. Batch fetch toàn bộ Parents để tối ưu performance (Tránh N+1 query)
        Map<UUID, ParentChunk> parentMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<ParentChunk> parents = parentChunkRepo.findAllById(parentIds);
            for (ParentChunk p : parents) {
                parentMap.put(p.getId(), p);
            }
        }

        Set<UUID> usedParentIds = new HashSet<>();
        Set<String> usedFallbackKeys = new HashSet<>(); // Bộ lọc trùng cho các child chunk legacy/fallback
        List<String> contextParts = new ArrayList<>();

        // 3. Duyệt và dựng cấu trúc Context
        for (ChunkResult c : contexts) {
            UUID pid = c.getParentId();
            String yearLabel = c.getYear() != null ? String.valueOf(c.getYear()) : "Không rõ năm";

            // Cải tiến 2: Dựng nhãn Section (Mục) nếu dữ liệu ingestion có cào được
            String sectionLabel = (c.getSection() != null && !c.getSection().isBlank())
                    ? " | Mục: " + c.getSection()
                    : "";

            if (pid != null && parentMap.containsKey(pid)) {
                // Nhánh 1: Có parent hợp lệ -> Tiến hành Dedup theo parentId
                if (usedParentIds.add(pid)) {
                    ParentChunk parent = parentMap.get(pid);
                    contextParts.add(String.format("[Nguồn: %s | Năm: %s%s]\n%s",
                            c.getSourceFile(), yearLabel, sectionLabel, parent.getContent()));
                }
            } else {
                // Nhánh 2 (Sửa lỗi 1): Fallback khi pid null HOẶC parentId bị lệch pha (DB
                // inconsistent)
                if (pid != null) {
                    log.warn(
                            "Khẩn cấp: Tìm thấy parentId {} cho chunk của file {} nhưng không tồn tại trong bảng parent_chunks! Tự động hạ cấp sang Child Content.",
                            pid, c.getSourceFile());
                }

                // Cải tiến 3: Khử trùng lặp dữ liệu fallback bằng mã băm nội dung hoặc index
                // (nếu có)
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
}
