package com.tomzxy.fbu_chat.controller;

import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * POST /api/documents/ingest
     * Upload PDF để embed và lưu vào vector store
     */
    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(@RequestParam("file") MultipartFile file) {
        IngestResponse response = documentService.ingestDocument(file);
        return ResponseEntity.ok(response);
    }
}
