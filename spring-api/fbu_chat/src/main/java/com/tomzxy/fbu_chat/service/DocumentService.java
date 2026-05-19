package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.ChunkCandidate;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.entity.DocumentChunk;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentService {

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final DocumentChunkRepository chunkRepository;

    public DocumentService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            DocumentChunkRepository chunkRepository) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public IngestResponse ingestDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        int year = extractYear(filename);
        String docType = extractDocType(filename);

        // 1. Idempotency: Xóa các chunk cũ của tài liệu này (nếu đã tồn tại)
        log.info("Xóa dữ liệu cũ cho file: {}", filename);
        chunkRepository.deleteBySourceFile(filename);

        // 2. Gửi sang Python lấy chunks (Semantic text extraction)
        List<ChunkCandidate> candidates = callPythonChunker(file);
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("AI Service không trả về chunk nào");
        }

        log.info("Đã extract {} chunks từ Python.", candidates.size());

        // 3. Xử lý theo batch
        List<DocumentChunk> entitiesToSave = new ArrayList<>();
        int batchSize = 20;

        for (int i = 0; i < candidates.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, candidates.size());
            List<ChunkCandidate> batch = candidates.subList(i, toIndex);

            List<String> texts = batch.stream().map(ChunkCandidate::getContent).collect(Collectors.toList());

            // Gọi python generate Embeddings
            List<List<Float>> embeddings = getEmbeddingsFromPython(texts);

            if (embeddings == null || embeddings.size() != batch.size()) {
                throw new RuntimeException("Lỗi sinh Embedding (số lượng trả về không khớp)");
            }

            for (int k = 0; k < batch.size(); k++) {
                ChunkCandidate cand = batch.get(k);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(cand.getContent());
                chunk.setSourceFile(filename);
                chunk.setChunkIndex(cand.getChunkIndex());
                chunk.setEmbedding(embeddings.get(k));
                chunk.setDocType(docType);
                chunk.setYear(year);
                entitiesToSave.add(chunk);
            }
        }

        // 4. Lưu toàn bộ batch bằng Spring Data
        chunkRepository.saveAll(entitiesToSave);
        log.info("Lưu thành công {} chunks (với vector 384) vào database.", entitiesToSave.size());

        return IngestResponse.builder()
                .message("Nạp thành công " + entitiesToSave.size() + " đoạn từ " + filename)
                .filename(filename)
                .build();
    }

    private List<ChunkCandidate> callPythonChunker(MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<ChunkCandidate[]> response = aiRestTemplate.exchange(
                aiBaseUrl + "/chunk",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                ChunkCandidate[].class);

        return response.getBody() != null ? List.of(response.getBody()) : new ArrayList<>();
    }

    private List<List<Float>> getEmbeddingsFromPython(List<String> texts) {
        EmbeddingRequest req = new EmbeddingRequest();
        req.setTexts(texts);
        req.setMode("passage"); // e5-small-v2: document phải dùng prefix "passage:"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<EmbeddingResponse> response = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                EmbeddingResponse.class);

        return response.getBody() != null ? response.getBody().getEmbeddings() : null;
    }

    private int extractYear(String filename) {
        if (filename == null)
            return 2026;
        Matcher m = Pattern.compile("20\\d{2}").matcher(filename);
        if (m.find()) {
            return Integer.parseInt(m.group());
        }
        return 2026;
    }

    private String extractDocType(String filename) {
        if (filename == null)
            return "general";
        String lower = filename.toLowerCase();
        if (lower.contains("qđ") || lower.contains("quyết định") || lower.contains("qd") || lower.contains("qđ")) {
            return "quy_che";
        }
        if (lower.contains("tb") || lower.contains("thông báo")) {
            return "thong_bao";
        }
        return "general";
    }
}
