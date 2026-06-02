package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.DocumentSummaryDto;
import com.tomzxy.fbu_chat.dto.IngestResponse;
import com.tomzxy.fbu_chat.repository.DocumentChunkRepository;
import com.tomzxy.fbu_chat.repository.ParentChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentService {

    private final SingleDocumentIngestorService singleDocumentIngestorService;
    private final DocumentChunkRepository chunkRepository;
    private final ParentChunkRepository parentChunkRepository;

    public DocumentService(
            DocumentChunkRepository chunkRepository,
            ParentChunkRepository parentChunkRepository,
            SingleDocumentIngestorService singleDocumentIngestorService) {
        this.chunkRepository = chunkRepository;
        this.parentChunkRepository = parentChunkRepository;
        this.singleDocumentIngestorService = singleDocumentIngestorService;
    }

    public List<IngestResponse> ingestDocuments(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Danh sách file không được trống");
        }

        List<CompletableFuture<IngestResponse>> futures = Arrays.stream(files)
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return singleDocumentIngestorService.ingestSingleFile(file);
                    } catch (Exception e) {
                        log.error("Lỗi khi nạp file {}: {}", file.getOriginalFilename(), e.getMessage());
                        return IngestResponse.builder()
                                .filename(file.getOriginalFilename())
                                .message("Thất bại: " + e.getMessage())
                                .chunks(0)
                                .build();
                    }
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

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

    @Transactional
    public void deleteDocument(String filename) {
        log.info("Xóa tài liệu: {}", filename);
        parentChunkRepository.deleteBySourceFile(filename);
        chunkRepository.deleteBySourceFile(filename);
    }
}
