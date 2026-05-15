package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.IngestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DocumentService {

    private final RestTemplate aiRestTemplate;

    public IngestResponse ingestDocument(MultipartFile file, int year, String docType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // Build multipart body
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

        String uri = UriComponentsBuilder.fromPath("/ingest")
                .queryParam("year", year)
                .queryParam("doc_type", docType)
                .build().toUriString();

        ResponseEntity<Map> response = aiRestTemplate.exchange(
                uri,
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
