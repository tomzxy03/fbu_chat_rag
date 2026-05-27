package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.ChunkCandidate;
import com.tomzxy.fbu_chat.dto.DocumentSummaryDto;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.entity.DocumentChunk;
import com.tomzxy.fbu_chat.entity.ParentChunk;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentService {

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final DocumentChunkRepository chunkRepository;
    private final ParentChunkRepository parentChunkRepository;

    public DocumentService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            DocumentChunkRepository chunkRepository,
            ParentChunkRepository parentChunkRepository) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.chunkRepository = chunkRepository;
        this.parentChunkRepository = parentChunkRepository;
    }

    @Transactional
    public IngestResponse ingestDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        boolean isMarkdown = filename != null && filename.toLowerCase().endsWith(".md");

        // 1. Idempotency: Xóa dữ liệu cũ
        log.info("Xóa dữ liệu cũ cho file: {}", filename);
        if (isMarkdown) {
            // Cascade: xóa parent → tự xóa child có parent_id
            parentChunkRepository.deleteBySourceFile(filename);
        }
        chunkRepository.deleteBySourceFile(filename);

        // 2. Gửi sang Python lấy chunks
        List<ChunkCandidate> candidates = callPythonChunker(file);
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("AI Service không trả về chunk nào");
        }

        log.info("Đã extract {} chunks từ Python.", candidates.size());

        // 3. Xử lý khác nhau cho .md và non-.md
        List<DocumentChunk> entitiesToSave;
        if (isMarkdown) {
            entitiesToSave = processMarkdownCandidates(candidates, filename);
        } else {
            entitiesToSave = processLegacyCandidates(candidates, filename);
        }

        // 4. Lưu toàn bộ batch bằng Spring Data
        chunkRepository.saveAll(entitiesToSave);
        log.info("Lưu thành công {} chunks (với vector 384) vào database.", entitiesToSave.size());

        return IngestResponse.builder()
                .message("Nạp thành công " + entitiesToSave.size() + " đoạn từ " + filename)
                .filename(filename)
                .chunks(entitiesToSave.size())
                .build();
    }

    // ─── .md flow: parent-child chunking ──────────────────────────────────────

    private List<DocumentChunk> processMarkdownCandidates(List<ChunkCandidate> candidates, String filename) {
        // Group by parentHeading → tạo ParentChunk per unique heading
        Map<String, List<ChunkCandidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParentHeading() != null ? c.getParentHeading() : "Nội dung chính",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Lưu ParentChunks và map heading → UUID
        Map<String, UUID> headingToParentId = new HashMap<>();
        for (Map.Entry<String, List<ChunkCandidate>> entry : grouped.entrySet()) {
            ChunkCandidate sample = entry.getValue().get(0);
            ParentChunk parent = ParentChunk.builder()
                    .sourceFile(sample.getSourceFile() != null ? sample.getSourceFile() : filename)
                    .heading(entry.getKey())
                    .content(sample.getParentContent() != null ? sample.getParentContent() : "")
                    .year(sample.getYear())
                    .docType(sample.getDocType())
                    .title(sample.getTitle())
                    .build();
            parent = parentChunkRepository.save(parent);
            headingToParentId.put(entry.getKey(), parent.getId());
        }

        log.info("Tạo {} parent chunks cho file .md", headingToParentId.size());

        // Embed child chunks theo batch
        List<DocumentChunk> entities = new ArrayList<>();
        int batchSize = 10;

        for (int i = 0; i < candidates.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, candidates.size());
            List<ChunkCandidate> batch = candidates.subList(i, toIndex);

            List<String> texts = batch.stream().map(ChunkCandidate::getContent).collect(Collectors.toList());
            List<List<Float>> embeddings = getEmbeddingsFromPython(texts);

            if (embeddings == null || embeddings.size() != batch.size()) {
                throw new RuntimeException("Lỗi sinh Embedding (số lượng trả về không khớp)");
            }

            for (int k = 0; k < batch.size(); k++) {
                ChunkCandidate cand = batch.get(k);
                String heading = cand.getParentHeading() != null ? cand.getParentHeading() : "Nội dung chính";

                DocumentChunk chunk = new DocumentChunk();
                chunk.setContent(cand.getContent());
                chunk.setSourceFile(cand.getSourceFile() != null ? cand.getSourceFile() : filename);
                chunk.setChunkIndex(cand.getChunkIndex());
                chunk.setEmbedding(toFloatArray(embeddings.get(k)));
                chunk.setDocType(cand.getDocType());
                chunk.setYear(cand.getYear());
                chunk.setParentId(headingToParentId.get(heading));
                entities.add(chunk);
            }
        }

        return entities;
    }

    // ─── Legacy flow: PDF/DOCX/etc (no parent) ───────────────────────────────

    private List<DocumentChunk> processLegacyCandidates(List<ChunkCandidate> candidates, String filename) {
        int year = extractYear(filename);
        String docType = extractDocType(filename);

        List<DocumentChunk> entities = new ArrayList<>();
        int batchSize = 10;

        for (int i = 0; i < candidates.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, candidates.size());
            List<ChunkCandidate> batch = candidates.subList(i, toIndex);

            List<String> texts = batch.stream().map(ChunkCandidate::getContent).collect(Collectors.toList());
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
                chunk.setEmbedding(toFloatArray(embeddings.get(k)));
                chunk.setDocType(docType);
                chunk.setYear(year);
                // parentId = null cho non-.md
                entities.add(chunk);
            }
        }

        return entities;
    }

    // ─── Python API calls ─────────────────────────────────────────────────────

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

    // ─── Utilities ────────────────────────────────────────────────────────────

    private static float[] toFloatArray(List<Float> vec) {
        float[] arr = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++)
            arr[i] = vec.get(i);
        return arr;
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
        if (lower.contains("qđ") || lower.contains("quyết định") || lower.contains("qd")) {
            return "quy_che";
        }
        if (lower.contains("tb") || lower.contains("thông báo")) {
            return "thong_bao";
        }
        return "general";
    }

    // ─── Document Management ──────────────────────────────────────────────────

    /**
     * Trả về danh sách tài liệu đã ingest, group theo source_file.
     */
    public List<DocumentSummaryDto> listDocuments() {
        return chunkRepository.findAllSummaries().stream()
                .map(p -> DocumentSummaryDto.builder()
                        .filename(p.getFilename())
                        .year(p.getYear())
                        .docType(p.getDocType())
                        .chunkCount(p.getChunkCount() != null ? p.getChunkCount().intValue() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Xóa toàn bộ chunks của một tài liệu theo tên file.
     * Xóa parent_chunks trước → cascade tự xóa child document_chunks có parent_id.
     * Sau đó xóa document_chunks không có parent (legacy PDF data).
     * Idempotent: không throw nếu file không tồn tại.
     */
    @Transactional
    public void deleteDocument(String filename) {
        log.info("Xóa tài liệu: {}", filename);
        parentChunkRepository.deleteBySourceFile(filename);
        chunkRepository.deleteBySourceFile(filename);
    }
}
