package com.tomzxy.fbu_chat.controller;

import com.tomzxy.fbu_chat.dto.DocumentSummaryDto;
import com.tomzxy.fbu_chat.dto.ImageUploadResponse;
import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.service.DocumentService;
import com.tomzxy.fbu_chat.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
// @CrossOrigin removed — CORS handled globally in SecurityConfig
public class DocumentController {

    private final DocumentService documentService;
    private final ImageService imageService;

    /**
     * POST /api/documents/ingest — ADMIN only (enforced by SecurityConfig)
     * Upload file để chunk, embed và lưu vào vector store.
     */
    @PostMapping("/ingest")
    public ResponseEntity<List<IngestResponse>> ingest(@RequestParam("files") MultipartFile[] files) {
        List<IngestResponse> responses = documentService.ingestDocuments(files);
        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/documents/images — ADMIN only
     * Upload ảnh minh họa, lưu MinIO, embed caption/tags và lưu vào vector store.
     */
    @PostMapping("/images")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(imageService.uploadImage(file, caption, tags, category));
    }

    /**
     * GET /api/documents — ADMIN only
     * Trả về danh sách tài liệu đã ingest với metadata.
     * Trả về [] nếu chưa có tài liệu nào (HTTP 200).
     */
    @GetMapping
    public ResponseEntity<List<DocumentSummaryDto>> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    /**
     * DELETE /api/documents/{filename} — ADMIN only
     * Xóa toàn bộ chunks của tài liệu theo tên file.
     * Idempotent: trả 200 dù file tồn tại hay không.
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String filename) {
        documentService.deleteDocument(filename);
        return ResponseEntity.ok(Map.of("message", "Đã xóa tài liệu: " + filename));
    }
}
