package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.IngestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final WebClient aiWebClient;

    public IngestResponse ingestDocument(MultipartFile file, int year, String docType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        if (!java.util.Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file PDF");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        try {
            bodyBuilder.part("file", file.getResource());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }

        Map response = aiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ingest")
                        .queryParam("year", year)
                        .queryParam("doc_type", docType)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String message = (String) response.get("message");
        log.info("Ingest result: {}", message);

        return IngestResponse.builder()
                .message(message)
                .filename(file.getOriginalFilename())
                .build();
    }
}
