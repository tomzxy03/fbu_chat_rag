package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.entity.DocumentChunk;
import com.tomzxy.fbu_chat.entity.ParentChunk;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.dto.ChunkCandidate;
import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class SingleDocumentIngestorService {

    private final DocumentChunkRepository chunkRepository;
    private final ParentChunkRepository parentChunkRepository;
    private final RestTemplate aiRestTemplate;
    @Qualifier("aiServiceBaseUrl")
    private final String aiBaseUrl;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestResponse ingestSingleFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        boolean isMarkdown = filename != null && filename.toLowerCase().endsWith(".md");

        log.info("Xóa dữ liệu cũ cho file: {}", filename);
        if (isMarkdown) {
            parentChunkRepository.deleteBySourceFile(filename);
        }
        chunkRepository.deleteBySourceFile(filename);

        List<ChunkCandidate> candidates = callPythonChunker(file);
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("AI Service không trả về chunk nào");
        }

        log.info("Đã extract {} chunks từ Python.", candidates.size());

        List<DocumentChunk> entitiesToSave;
        if (isMarkdown) {
            entitiesToSave = processMarkdownCandidates(candidates, filename);
        } else {
            entitiesToSave = processLegacyCandidates(candidates, filename);
        }

        chunkRepository.saveAll(entitiesToSave);
        log.info("Lưu thành công {} chunks vào database cho file: {}", entitiesToSave.size(), filename);
        
        return IngestResponse.builder()
                .message("Nạp thành công " + entitiesToSave.size() + " đoạn")
                .filename(filename)
                .chunks(entitiesToSave.size())
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
    private List<DocumentChunk> processMarkdownCandidates(List<ChunkCandidate> candidates, String filename) {
        Map<String, List<ChunkCandidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParentHeading() != null ? c.getParentHeading() : "Nội dung chính",
                        LinkedHashMap::new,
                        Collectors.toList()));

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

    private List<List<Float>> getEmbeddingsFromPython(List<String> texts) {
        EmbeddingRequest req = new EmbeddingRequest();
        req.setTexts(texts);
        req.setMode("passage");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<EmbeddingResponse> response = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                EmbeddingResponse.class);

        return response.getBody() != null ? response.getBody().getEmbeddings() : null;
    }

    private static float[] toFloatArray(List<Float> vec) {
        float[] arr = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++)
            arr[i] = vec.get(i);
        return arr;
    }

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
                entities.add(chunk);
            }
        }

        return entities;
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
}