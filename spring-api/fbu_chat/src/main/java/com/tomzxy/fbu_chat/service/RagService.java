package com.tomzxy.fbu_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.fbu_chat.dto.ChatRequest;
import com.tomzxy.fbu_chat.dto.ChatResponse;
import com.tomzxy.fbu_chat.dto.ChatHistoryMessage;
import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.dto.ImageResult;
import com.tomzxy.fbu_chat.entity.Conversation;
import com.tomzxy.fbu_chat.entity.Message;
import com.tomzxy.fbu_chat.repository.ConversationRepository;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.DocumentImageRepository;
import com.tomzxy.fbu_chat.repository.MessageRepository;
import com.tomzxy.fbu_chat.service.QueryClassifierService.QueryClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
 * Orchestrator chính của RAG pipeline.
 * Ủy quyền các trách nhiệm cụ thể:
 * - GroqChatClient     : HTTP call đến Groq API
 * - QueryClassifierService : phân loại intent + slot-fill docType
 * - ContextRetrievalService: hybrid search + filter + build context string
 */
@Slf4j
@Service
@SuppressWarnings("unchecked")
public class RagService {

    private static final double IMAGE_SIMILARITY_THRESHOLD = 0.70;
    private static final int IMAGE_TOP_K = 3;
    private static final int HISTORY_WINDOW = 3;
    private static final int MAX_HISTORY_CONTENT_LENGTH = 4000;

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final DocumentImageRepository imageRepo;
    private final DocumentChunkRepository docRepo;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final VietnameseTokenizerService tokenizerService;

    // ── Extracted services ──
    private final GroqChatClient groqClient;
    private final QueryClassifierService queryClassifier;
    private final ContextRetrievalService contextRetrieval;

    public RagService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            ConversationRepository conversationRepo,
            MessageRepository messageRepo,
            DocumentImageRepository imageRepo,
            DocumentChunkRepository docRepo,
            StorageService storageService,
            ObjectMapper objectMapper,
            VietnameseTokenizerService tokenizerService,
            GroqChatClient groqClient,
            QueryClassifierService queryClassifier,
            ContextRetrievalService contextRetrieval) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.imageRepo = imageRepo;
        this.docRepo = docRepo;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.tokenizerService = tokenizerService;
        this.groqClient = groqClient;
        this.queryClassifier = queryClassifier;
        this.contextRetrieval = contextRetrieval;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        Conversation conversation = resolveConversation(request, userId);

        groqClient.ensureApiKeyConfigured();

        QueryClassification classification = queryClassifier.classify(request.getQuery());

        if (!classification.isFbuInfo()) {
            log.info("Detected non-RAG conversational query. Skipping embedding/search pipeline.");
            List<Message> convHistory = conversation != null
                    ? messageRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                    : null;
            return buildConversationalChatResponse(request, conversation, convHistory);
        }

        if (request.getDocType() == null && classification.docType() != null) {
            request.setDocType(classification.docType());
            log.info("Slot-filling inferred docType='{}' from query", classification.docType());
        }

        // Load history 1 lần duy nhất cho toàn bộ request — tránh 4 round-trip DB riêng lẻ.
        // conversationHistory = null khi anonymous (không có conversation DB).
        List<Message> conversationHistory = conversation != null
                ? messageRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                : null;

        String segmentedQuery = tokenizerService.segmentForEmbedding(request.getQuery());
        log.info("Encoding query using AI Service...");
        List<Float> queryVector = getQueryEmbedding(segmentedQuery);
        String vectorStr = "[" + queryVector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";

        boolean imageIntent = isImageRequest(request.getQuery());

        Set<String> shownImageUrls = extractShownImageUrls(conversationHistory);
        boolean isImageFollowUp = imageIntent && !shownImageUrls.isEmpty()
                && isFollowUpMoreRequest(request.getQuery());

        List<ChatResponse.ImageInfo> images;
        if (isImageFollowUp) {
            String prevCategory = extractPreviousImageCategory(conversationHistory);
            images = prevCategory != null
                    ? searchImagesByCategory(vectorStr, prevCategory, shownImageUrls)
                    : List.of();
            log.info("Image follow-up — reusing category='{}', excluding {} shown URLs, found {} new",
                    prevCategory, shownImageUrls.size(), images.size());
        } else {
            images = imageIntent ? searchImages(vectorStr, shownImageUrls) : List.of();
        }

        if (imageIntent && isImageOnlyRequest(request.getQuery())) {
            if (!images.isEmpty()) {
                log.info("Detected image-only query. Returning {} new image results without text RAG.", images.size());
                return buildImageOnlyChatResponse(request, conversation, images);
            }
            log.info("Detected image-only query, but no new image matched. Returning image exhausted response.");
            return buildImageExhaustedChatResponse(request, conversation);
        }

        Set<UUID> usedParentIds = isFollowUpMoreRequest(request.getQuery())
                ? extractUsedParentIds(conversationHistory)
                : Set.of();
        if (!usedParentIds.isEmpty()) {
            log.info("Follow-up 'more info' detected — excluding {} used parent chunks", usedParentIds.size());
        }

        List<ChunkResult> topContexts = contextRetrieval.search(vectorStr, request, usedParentIds);

        if (topContexts.isEmpty()) {
            if (imageIntent && !images.isEmpty()) {
                log.info("No document chunks found, image intent matched. Returning image-only response.");
                return buildImageOnlyChatResponse(request, conversation, images);
            }
            log.info("No reliable document chunks found. Triggering no-data fallback response.");
            return buildFallbackChatResponse(request, conversation);
        }

        log.info("Final chunks for LLM: {}", topContexts.size());
        logChunks(topContexts);

        String contextText = contextRetrieval.buildContextString(topContexts);
        String answer = callRagGroq(request, conversation, conversationHistory, contextText);

        boolean noDataAnswer = isNoDataAnswer(answer);
        if (noDataAnswer) {
            answer = stripNoDataMarker(answer);
            log.info("LLM reported insufficient context. Clearing sources from response.");
            images = List.of();
        }

        List<Map<String, Object>> sources = noDataAnswer ? List.of() : buildSources(topContexts);

        UUID messageId = persistAssistantMessage(conversation, answer, sources, images);

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(toSourceInfos(sources))
                .images(images)
                .build();
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

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

    // ── Embedding (cached) ───────────────────────────────────────────────────

    /**
     * Lấy embedding vector cho query. Kết quả được cache theo segmentedQuery.
     * Cache TTL=45 phút, max=500 entries — xem EmbeddingCacheConfig.
     * Phải là public để Spring proxy áp dụng @Cacheable đúng cách.
     */
    @Cacheable(value = "queryEmbeddings", key = "#segmentedQuery")
    public List<Float> getQueryEmbedding(String segmentedQuery) {
        EmbeddingRequest embReq = new EmbeddingRequest();
        embReq.setTexts(List.of(segmentedQuery));
        embReq.setMode("query");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<EmbeddingResponse> embResponse = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings", HttpMethod.POST,
                new HttpEntity<>(embReq, headers), EmbeddingResponse.class);

        if (embResponse.getBody() == null || embResponse.getBody().getEmbeddings().isEmpty()) {
            throw new RuntimeException("Lỗi sinh Embedding cho câu hỏi");
        }
        log.debug("Cache MISS — fetched embedding for query: {}", segmentedQuery);
        return embResponse.getBody().getEmbeddings().get(0);
    }

    // ── Conversation helpers ─────────────────────────────────────────────────

    private Conversation resolveConversation(ChatRequest request, String userId) {
        if (userId == null) return null;
        if (request.getConversationId() != null) {
            UUID convId = UUID.fromString(request.getConversationId());
            return conversationRepo.findByIdAndUserId(convId, userId)
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                            "Conversation không tồn tại hoặc bạn không có quyền truy cập"));
        }
        return createConversation(request.getQuery(), userId);
    }

    private Conversation createConversation(String query, String userId) {
        String title = query.length() > 50 ? query.substring(0, 50) + "..." : query;
        return conversationRepo.save(Conversation.builder().userId(userId).title(title).build());
    }

    private UUID persistAssistantMessage(Conversation conversation, String answer,
                                          List<Map<String, Object>> sources,
                                          List<ChatResponse.ImageInfo> images) {
        if (conversation == null) return null;
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
                .images(serializeImages(images))
                .build();
        messageRepo.save(assistantMsg);
        conversationRepo.save(conversation);
        return assistantMsg.getId();
    }

    private void addHistoryMessage(List<Map<String, Object>> groqMessages, String role, String content) {
        if (!"user".equals(role) && !"assistant".equals(role)) return;
        if (content == null || content.isBlank()) return;
        Map<String, Object> hm = new HashMap<>();
        hm.put("role", role);
        hm.put("content", content.length() > MAX_HISTORY_CONTENT_LENGTH
                ? content.substring(0, MAX_HISTORY_CONTENT_LENGTH) : content);
        groqMessages.add(hm);
    }

    private void appendDbHistory(List<Map<String, Object>> groqMessages, List<Message> history) {
        if (history == null || history.isEmpty()) return;
        int fromIdx = Math.max(0, history.size() - HISTORY_WINDOW * 2);
        for (Message histMsg : history.subList(fromIdx, history.size())) {
            String content = histMsg.getContent();
            if ("assistant".equals(histMsg.getRole()) && content != null && content.length() > 200) {
                content = content.substring(0, 200) + "... [đã rút gọn]";
            }
            Map<String, Object> hm = new HashMap<>();
            hm.put("role", histMsg.getRole());
            hm.put("content", content);
            groqMessages.add(hm);
        }
    }

    private void appendClientHistory(List<Map<String, Object>> groqMessages, List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) return;
        int fromIdx = Math.max(0, history.size() - HISTORY_WINDOW * 2);
        for (ChatHistoryMessage h : history.subList(fromIdx, history.size())) {
            if (h != null) addHistoryMessage(groqMessages, h.getRole(), h.getContent());
        }
    }

    // ── Groq calls ───────────────────────────────────────────────────────────

    private String callRagGroq(ChatRequest request, Conversation conversation,
                               List<Message> conversationHistory, String contextText) {
        String systemPrompt = "# VAI TRÒ VÀ ĐỊNH DANH\n" +
                "Bạn là Trợ lý AI chuyên nghiệp và thân thiện của Trường Đại học Tài chính - Ngân hàng Hà Nội (FBU). " +
                "Nhiệm vụ của bạn là hỗ trợ sinh viên và giảng viên tra cứu các quy chế, quy định nội bộ dựa trên dữ liệu [CONTEXT] được cung cấp.\n\n" +
                "# NGUYÊN TẮC CỐT LÕI (TUÂN THỦ TUYỆT ĐỐI)\n" +
                "1. CHỈ câu trả lời dựa trên thông tin có trong [CONTEXT]. Tuyệt đối không tự suy diễn, bịa đặt hoặc dùng kiến thức chung trên Internet để đoán quy định của FBU.\n" +
                "2. Trả lời ĐẦY ĐỦ — bao gồm TẤT CẢ thông tin liên quan có trong [CONTEXT]. Nếu CONTEXT có danh sách, bảng biểu, nhiều mục → trình bày đúng cấu trúc đó, KHÔNG được bỏ sót hoặc tóm tắt.\n" +
                "3. Trả lời bằng tiếng Việt lịch sự, truyền cảm hứng, ngắn gọn nhưng đầy đủ ý. Sử dụng các dấu gạch đầu dòng rõ ràng để phân tách các quy trình, điều khoản.\n" +
                "4. Quản lý lịch sử hội thoại: Đọc kỹ các câu trả lời trước đó để KHÔNG lặp lại thông tin cũ. Chỉ tập trung bổ sung thông tin mới đáp ứng đúng câu hỏi tiếp diễn.\n\n" +
                "5. Nếu câu hỏi yêu cầu liệt kê bộ môn/học phần, hãy liệt kê đầy đủ tất cả bộ môn và học phần liên quan xuất hiện trong [CONTEXT].\n\n" +
                "# HƯỚNG DẪN XỬ LÝ KHI THIẾU THÔNG TIN (KỊCH BẢN FALLBACK)\n" +
                "BẠN CHỈ KÍCH HOẠT KỊCH BẢN NÀY KHI: [CONTEXT] hoàn toàn trống rỗng HOẶC tất cả nội dung trong [CONTEXT] không liên quan gì đến câu hỏi.\n" +
                "LƯU Ý QUAN TRỌNG: Nếu [CONTEXT] có chứa BẤT KỲ thông tin nào có thể trả lời câu hỏi — dù là thông tin về quy chế, tác giả, người tạo hệ thống, giới thiệu dự án, hay bất kỳ chủ đề nào khác — bạn PHẢI trả lời dựa trên đó, KHÔNG được dùng [NO_DATA].\n" +
                "Khi rơi vào kịch bản thiếu thông tin này, bạn PHẢI tuân thủ cấu trúc trả về sau:\n" +
                "- Bắt đầu câu trả lời bằng Tag chính xác: [NO_DATA]\n" +
                "- Sau đó, viết một câu thông báo lịch sự, ấm áp rằng hệ thống dữ liệu hiện tại chưa cập nhật thông tin về chủ đề này và mời người dùng gửi phản hồi qua 'Tab Góp ý' hoặc gửi email về trinhdat24102003@gmail.com.\n" +
                "⚠️ CHÚ Ý: Tuyệt đối không dùng văn mẫu cố định của hệ thống trong prompt này, hãy tự viết câu thông báo một cách tự nhiên.\n\n" +
                "# QUY TẮC CẤM ĐỊNH DẠNG NGUỒN\n" +
                "- Tuyệt đối KHÔNG được tự viết chữ 'Nguồn:' hoặc tự tổng hợp danh sách tên file ở cuối câu trả lời dưới mọi hình thức (Hệ thống đã có bộ lọc tự động xử lý phần này).\n" +
                "- Bạn chỉ được phép lồng ghép tên văn bản một cách tự nhiên vào câu văn nếu cần làm rõ tính pháp lý (Ví dụ: 'Dựa trên Quyết định số 116, quy trình xác nhận sinh viên gồm...').";

        String userPrompt = "CONTEXT TỪ TÀI LIỆU FBU:\n" + contextText + "\n\n" +
                "CÂU HỎI HIỆN TẠI: " + request.getQuery() + "\n\n" +
                "Trả lời (Tuân thủ tuyệt đối quy tắc định dạng nguồn):";

        List<Map<String, Object>> groqMessages = new ArrayList<>();
        groqMessages.add(Map.of("role", "system", "content", systemPrompt));

        if (conversation != null) {
            appendDbHistory(groqMessages, conversationHistory);
            messageRepo.save(Message.builder()
                    .conversation(conversation).role("user").content(request.getQuery()).build());
        } else {
            appendClientHistory(groqMessages, request.getHistory());
        }

        groqMessages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", GroqChatClient.GROQ_MODEL);
        payload.put("messages", groqMessages);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 2048);

        log.info("Calling Groq LLM Generator...");
        return groqClient.call(payload);
    }

    private ChatResponse buildConversationalChatResponse(ChatRequest request, Conversation conversation,
                                                          List<Message> conversationHistory) {
        List<Map<String, Object>> groqMessages = new ArrayList<>();
        groqMessages.add(Map.of("role", "system", "content",
                "Bạn là trợ lý AI thân thiện của trường Đại học Tài chính - Ngân hàng Hà Nội (FBU). " +
                "Trả lời các câu xã giao/tán gẫu bằng tiếng Việt, tự nhiên, ngắn gọn. " +
                "Nếu người dùng hỏi thông tin chính thức cần tra cứu tài liệu FBU, hãy gợi ý họ đặt câu hỏi cụ thể."));

        if (conversation != null) {
            appendDbHistory(groqMessages, conversationHistory);
        } else {
            appendClientHistory(groqMessages, request.getHistory());
        }
        groqMessages.add(Map.of("role", "user", "content", request.getQuery()));

        if (conversation != null) {
            messageRepo.save(Message.builder()
                    .conversation(conversation).role("user").content(request.getQuery()).build());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", GroqChatClient.GROQ_MODEL);
        payload.put("messages", groqMessages);
        payload.put("temperature", 0.4);
        payload.put("max_tokens", 256);

        String answer = groqClient.call(payload);
        UUID messageId = null;
        if (conversation != null) {
            Message assistantMsg = Message.builder()
                    .conversation(conversation).role("assistant").content(answer).sources("[]").build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();
            conversationRepo.save(conversation);
        }

        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId)
                .query(request.getQuery())
                .answer(answer)
                .sources(List.of())
                .images(List.of())
                .build();
    }

    // ── Fallback / image response builders ──────────────────────────────────

    private ChatResponse buildImageOnlyChatResponse(ChatRequest request, Conversation conversation,
                                                     List<ChatResponse.ImageInfo> images) {
        String answer = "Mình tìm thấy một số hình ảnh minh họa phù hợp với câu hỏi của bạn. " +
                "Bạn có thể xem các ảnh được đính kèm bên dưới.";
        UUID messageId = null;
        if (conversation != null) {
            messageRepo.save(Message.builder().conversation(conversation)
                    .role("user").content(request.getQuery()).build());
            Message assistantMsg = Message.builder().conversation(conversation)
                    .role("assistant").content(answer).sources("[]").images(serializeImages(images)).build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();
            conversationRepo.save(conversation);
        }
        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId).query(request.getQuery()).answer(answer)
                .sources(List.of()).images(images).build();
    }

    private ChatResponse buildImageFallbackChatResponse(ChatRequest request, Conversation conversation) {
        String answer = "Hiện tại hệ thống chưa tìm thấy hình ảnh phù hợp với yêu cầu của bạn.";
        UUID messageId = null;
        if (conversation != null) {
            messageRepo.save(Message.builder().conversation(conversation)
                    .role("user").content(request.getQuery()).build());
            Message assistantMsg = Message.builder().conversation(conversation)
                    .role("assistant").content(answer).sources("[]").build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();
            conversationRepo.save(conversation);
        }
        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId).query(request.getQuery()).answer(answer)
                .sources(List.of()).images(List.of()).build();
    }

    /** Trả về khi user hỏi "còn ảnh khác không?" nhưng đã hết ảnh mới (tất cả đã hiển thị). */
    private ChatResponse buildImageExhaustedChatResponse(ChatRequest request, Conversation conversation) {
        String answer = "Mình đã chia sẻ tất cả hình ảnh hiện có trong hệ thống liên quan đến chủ đề này rồi. "
                + "Nếu bạn muốn xem ảnh về chủ đề khác, hãy đặt câu hỏi cụ thể hơn nhé!";
        UUID messageId = null;
        if (conversation != null) {
            messageRepo.save(Message.builder().conversation(conversation)
                    .role("user").content(request.getQuery()).build());
            Message assistantMsg = Message.builder().conversation(conversation)
                    .role("assistant").content(answer).sources("[]").build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();
            conversationRepo.save(conversation);
        }
        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId).query(request.getQuery()).answer(answer)
                .sources(List.of()).images(List.of()).build();
    }

    private ChatResponse buildFallbackChatResponse(ChatRequest request, Conversation conversation) {
        String answer = "Hiện tại hệ thống dữ liệu của mình chưa có thông tin chính thức về câu hỏi: \""
                + request.getQuery()
                + "\".\n\nNếu bạn biết hoặc có tài liệu chính thức về nội dung này, bạn có thể gửi phản hồi qua "
                + "Phòng Công tác Sinh viên hoặc email support-chatbot@fbu.edu.vn để hệ thống được cập nhật đầy đủ hơn. Cảm ơn bạn đã góp ý.";
        UUID messageId = null;
        if (conversation != null) {
            messageRepo.save(Message.builder().conversation(conversation)
                    .role("user").content(request.getQuery()).build());
            Message assistantMsg = Message.builder().conversation(conversation)
                    .role("assistant").content(answer).sources("[]").build();
            messageRepo.save(assistantMsg);
            messageId = assistantMsg.getId();
            conversationRepo.save(conversation);
        }
        return ChatResponse.builder()
                .conversationId(conversation != null ? conversation.getId() : null)
                .messageId(messageId).query(request.getQuery()).answer(answer)
                .sources(List.of()).images(List.of()).build();
    }

    // ── Image helpers ────────────────────────────────────────────────────────

    private List<ChatResponse.ImageInfo> searchImages(String vectorStr, Set<String> excludeUrls) {
        try {
            return imageRepo.findSimilarImages(vectorStr, IMAGE_TOP_K + excludeUrls.size(), IMAGE_SIMILARITY_THRESHOLD)
                    .stream()
                    .filter(r -> {
                        boolean exists = storageService.objectExistsByUrl(r.getUrl());
                        if (!exists) log.warn("Skipping stale image: {}", r.getUrl());
                        return exists;
                    })
                    // Loại ảnh đã hiển thị ở các lượt trước
                    .filter(r -> !excludeUrls.contains(r.getUrl()))
                    .limit(IMAGE_TOP_K)
                    .map(r -> ChatResponse.ImageInfo.builder()
                            .url(r.getUrl()).caption(r.getCaption()).category(r.getCategory())
                            .score(r.getScore() != null ? r.getScore() : 0.0).build())
                    .toList();
        } catch (Exception e) {
            log.warn("Image search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Gom tất cả parentId đã được dùng trong các lượt assistant trước của conversation.
     * Dùng để exclude khi user hỏi follow-up "còn thông tin gì khác không?".
     * Source file được lưu trong Message.sources dạng JSON: [{"file":"...", "year":..., "doc_type":"..."}]
     * → join với DocumentChunk để lấy parentId theo sourceFile.
     *
     * Approach đơn giản hơn: lấy sourceFile từ sources, rồi exclude chunk theo sourceFile
     * vì parentId không được lưu trực tiếp trong messages.sources.
     */
    private Set<UUID> getUsedParentIds(Conversation conversation, List<ChatHistoryMessage> clientHistory) {
        if (conversation == null) return Set.of();

        Set<UUID> parentIds = new HashSet<>();
        messageRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .map(Message::getSources)
                .filter(json -> json != null && !json.isBlank() && !json.equals("[]"))
                .flatMap(json -> deserializeSources(json).stream())
                .map(ChatResponse.SourceInfo::getFile)
                .filter(Objects::nonNull)
                .forEach(sourceFile -> {
                    // Lấy tất cả parentId của các chunk thuộc sourceFile này
                    docRepo.findParentIdsBySourceFile(sourceFile)
                            .stream()
                            .filter(Objects::nonNull)
                            .forEach(parentIds::add);
                });

        return parentIds;
    }

    /**
     * Detect intent follow-up "còn thông tin gì khác không?" / "xem thêm" / "tiếp tục".
     * Dùng keyword matching đơn giản — đủ cho các pattern phổ biến trong tiếng Việt.
     */
    private boolean isFollowUpMoreRequest(String query) {
        String q = contextRetrieval.normalizeForScope(query);
        return containsAny(q,
                "con thong tin", "thong tin khac", "con gi khac", "co gi khac",
                "xem them", "cho xem them", "biet them", "them thong tin",
                "tiep tuc", "con nua khong", "con nua ko", "co them khong",
                "khai thac them", "mo rong them", "chi tiet hon");
    }

    private Set<String> extractShownImageUrls(List<Message> history) {
        if (history == null || history.isEmpty()) return Set.of();
        Set<String> urls = new HashSet<>();
        history.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .map(Message::getImages)
                .filter(json -> json != null && !json.isBlank() && !json.equals("[]"))
                .flatMap(json -> deserializeImages(json).stream())
                .map(ChatResponse.ImageInfo::getUrl)
                .filter(Objects::nonNull)
                .forEach(urls::add);
        if (!urls.isEmpty()) {
            log.info("Excluding {} already-shown image URLs from search results", urls.size());
        }
        return urls;
    }

    private String extractPreviousImageCategory(List<Message> history) {
        if (history == null || history.isEmpty()) return null;
        return history.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .map(Message::getImages)
                .filter(json -> json != null && !json.isBlank() && !json.equals("[]"))
                .flatMap(json -> deserializeImages(json).stream())
                .map(ChatResponse.ImageInfo::getCategory)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second) // lấy category của turn ảnh cuối cùng
                .orElse(null);
    }

    private Set<UUID> extractUsedParentIds(List<Message> history) {
        if (history == null || history.isEmpty()) return Set.of();
        Set<UUID> parentIds = new HashSet<>();
        history.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .map(Message::getSources)
                .filter(json -> json != null && !json.isBlank() && !json.equals("[]"))
                .flatMap(json -> deserializeSources(json).stream())
                .map(ChatResponse.SourceInfo::getFile)
                .filter(Objects::nonNull)
                .forEach(sourceFile ->
                        docRepo.findParentIdsBySourceFile(sourceFile)
                                .stream()
                                .filter(Objects::nonNull)
                                .forEach(parentIds::add));
        return parentIds;
    }

    /**
     * Search ảnh tiếp theo trong cùng category, exclude các URL đã hiển thị.
     * Tái dùng vector của original query (đã cache) — không embed lại câu follow-up.
     */
    private List<ChatResponse.ImageInfo> searchImagesByCategory(String vectorStr, String category,
                                                                  Set<String> excludeUrls) {
        try {
            // Format PostgreSQL text[] literal: {"url1","url2"} hoặc {} nếu rỗng
            String pgArray = "{" + excludeUrls.stream()
                    .map(u -> "\"" + u.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(",")) + "}";

            return imageRepo.findSimilarImagesByCategory(
                            vectorStr, category, pgArray, IMAGE_TOP_K, IMAGE_SIMILARITY_THRESHOLD)
                    .stream()
                    .filter(r -> {
                        boolean exists = storageService.objectExistsByUrl(r.getUrl());
                        if (!exists) log.warn("Skipping stale image: {}", r.getUrl());
                        return exists;
                    })
                    .map(r -> ChatResponse.ImageInfo.builder()
                            .url(r.getUrl()).caption(r.getCaption()).category(r.getCategory())
                            .score(r.getScore() != null ? r.getScore() : 0.0).build())
                    .toList();
        } catch (Exception e) {
            log.warn("Image category search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isImageRequest(String query) {
        return hasImageKeyword(contextRetrieval.normalizeForScope(query));
    }

    private boolean isImageOnlyRequest(String query) {
        String q = contextRetrieval.normalizeForScope(query);
        if (q.isBlank() || !hasImageKeyword(q)) return false;

        // Câu follow-up hỏi thêm ảnh: "còn ảnh khác không", "có ảnh nào khác không", v.v.
        boolean isFollowUpImageQuery = containsAny(q, "con anh", "anh khac", "hinh khac",
                "co anh nao khac", "co hinh nao khac", "xem them", "anh nao khac");
        if (isFollowUpImageQuery) return true;

        // Câu hỏi về text info → không phải image-only
        boolean asksForTextInfo = containsAny(q,
                "quy dinh", "quy che", "thu tuc", "huong dan", "hoc phi", "diem", "gpa",
                "tin chi", "phuc khao", "hoan thi", "lich thi", "lich hoc", "hoc bong",
                "mien giam", "tot nghiep", "bao nhieu", "khi nao", "o dau", "lam the nao",
                "can gi", "dieu kien", "co gi", "gioi thieu", "thong tin", "mo ta");
        if (asksForTextInfo) return false;

        // Câu hỏi xem ảnh cơ sở vật chất / địa điểm trường
        return containsAny(q, "truong", "fbu", "khuon vien", "co so", "toa nha",
                "giang duong", "thu vien", "phong hoc", "dich vong hau", "me linh", "dai hoc");
    }

    private boolean hasImageKeyword(String q) {
        boolean asksForImage = containsAny(q, "hinh anh", "photo", "image", "logo",
                "cho xem", "xem anh", "xem hinh", "gui anh", "co anh", "co hinh");
        return asksForImage || containsToken(q, "anh") || containsToken(q, "hinh");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private boolean containsToken(String value, String token) {
        return List.of(value.split("\\s+")).contains(token);
    }

    // ── Source / serialization helpers ──────────────────────────────────────

    private List<Map<String, Object>> buildSources(List<ChunkResult> topContexts) {
        Set<String> seen = new HashSet<>();
        return topContexts.stream()
                .filter(c -> seen.add(Objects.toString(c.getSourceFile(), "")))
                .map(c -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("file", c.getSourceFile());
                    s.put("year", c.getYear());
                    s.put("doc_type", c.getDocType());
                    return s;
                }).collect(Collectors.toList());
    }

    private List<ChatResponse.SourceInfo> toSourceInfos(List<Map<String, Object>> sources) {
        return sources.stream()
                .collect(Collectors.toMap(
                        s -> (String) s.get("file"),
                        s -> ChatResponse.SourceInfo.builder()
                                .file((String) s.get("file"))
                                .year(s.get("year") instanceof Integer ? (Integer) s.get("year") : null)
                                .docType((String) s.get("doc_type"))
                                .build(),
                        (existing, duplicate) -> existing))
                .values().stream().toList();
    }

    private String serializeImages(List<ChatResponse.ImageInfo> images) {
        if (images == null || images.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize images", e);
            return "[]";
        }
    }

    public List<ChatResponse.ImageInfo> deserializeImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank() || imagesJson.equals("[]")) return List.of();
        try {
            return objectMapper.readValue(imagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatResponse.ImageInfo.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize images: {}", e.getMessage());
            return List.of();
        }
    }

    public List<ChatResponse.SourceInfo> deserializeSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank() || sourcesJson.equals("[]")) return List.of();
        try {
            return objectMapper.readValue(sourcesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatResponse.SourceInfo.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize sources: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Answer helpers ───────────────────────────────────────────────────────

    private boolean isNoDataAnswer(String answer) {
        if (answer == null) return false;
        return answer.trim().toLowerCase(Locale.ROOT).startsWith("[no_data]");
    }

    private String stripNoDataMarker(String answer) {
        if (answer == null) return "";
        return answer.replaceFirst("(?i)^\\s*\\[NO_DATA\\]\\s*", "").trim();
    }

    private void logChunks(List<ChunkResult> topContexts) {
        log.info("=== CHUNKS SENT TO LLM ({}) ===", topContexts.size());
        for (int i = 0; i < topContexts.size(); i++) {
            ChunkResult c = topContexts.get(i);
            log.info("[{}] {} | len={} | preview={}", i + 1, c.getSourceFile(),
                    c.getContent() != null ? c.getContent().length() : 0,
                    c.getContent() != null
                            ? c.getContent().substring(0, Math.min(80, c.getContent().length())).replace("\n", " ")
                            : "NULL");
        }
        log.info("================================");
    }
}
