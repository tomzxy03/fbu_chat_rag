package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.IngestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class DocumentService {

    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;

    public DocumentService(
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
    }

    public IngestResponse ingestDocument(MultipartFile file, int year, String docType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", resource);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String url = aiBaseUrl + "/ingest?year=" + year + "&doc_type=" + docType;
        log.info("Calling AI ingest: {}", url);

        ResponseEntity<Map> response = aiRestTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        String message = (String) response.getBody().get("message");
        log.info("Ingest result: {}", message);

        return IngestResponse.builder()
                .message(message)
                .filename(file.getOriginalFilename())
                .build();
    }
}
