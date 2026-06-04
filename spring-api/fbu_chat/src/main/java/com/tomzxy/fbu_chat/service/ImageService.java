package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.EmbeddingRequest;
import com.tomzxy.fbu_chat.dto.EmbeddingResponse;
import com.tomzxy.fbu_chat.dto.ImageUploadResponse;
import com.tomzxy.fbu_chat.entity.DocumentImage;
import com.tomzxy.fbu_chat.repository.DocumentImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageService {
    private static final int MIN_CAPTION_LENGTH = 10;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "co_so_vat_chat",
            "khuon_vien",
            "giang_duong",
            "thu_vien",
            "phong_thuc_hanh",
            "the_thao",
            "su_kien",
            "logo",
            "tai_lieu",
            "khac");

    private final StorageService storageService;
    private final DocumentImageRepository imageRepository;
    private final RestTemplate aiRestTemplate;
    private final String aiBaseUrl;
    private final VietnameseTokenizerService tokenizerService;

    public ImageService(
            StorageService storageService,
            DocumentImageRepository imageRepository,
            RestTemplate aiRestTemplate,
            @Qualifier("aiServiceBaseUrl") String aiBaseUrl,
            VietnameseTokenizerService tokenizerService) {
        this.storageService = storageService;
        this.imageRepository = imageRepository;
        this.aiRestTemplate = aiRestTemplate;
        this.aiBaseUrl = aiBaseUrl;
        this.tokenizerService = tokenizerService;
    }

    @Transactional
    public ImageUploadResponse uploadImage(MultipartFile file, String caption, String tags, String category) {
        validateImageUpload(file, caption, tags, category);

        String normalizedCaption = normalize(caption);
        String normalizedTags = normalize(tags);
        String normalizedCategory = normalize(category);
        String textToEmbed = List.of(normalizedCaption, normalizedTags, normalizedCategory).stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(". "));

        String minioUrl = storageService.uploadFile(file, "images");
        float[] embedding = toFloatArray(embedImageMetadata(textToEmbed));

        DocumentImage image = DocumentImage.builder()
                .minioUrl(minioUrl)
                .caption(normalizedCaption)
                .tags(normalizedTags != null ? normalizedTags : "")
                .category(normalizedCategory)
                .tagEmbedding(embedding)
                .build();

        DocumentImage saved = imageRepository.save(image);
        log.info("Uploaded image {} with category {}", saved.getId(), saved.getCategory());

        return ImageUploadResponse.builder()
                .id(saved.getId())
                .url(saved.getMinioUrl())
                .caption(saved.getCaption())
                .tags(saved.getTags())
                .category(saved.getCategory())
                .build();
    }

    private void validateImageUpload(MultipartFile file, String caption, String tags, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("File upload phải là ảnh");
        }
        if (caption == null || caption.isBlank()) {
            throw new IllegalArgumentException("Mô tả ảnh không được để trống");
        }
        if (caption.trim().length() < MIN_CAPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả ảnh cần có ít nhất " + MIN_CAPTION_LENGTH + " ký tự");
        }
        if (tags == null || tags.isBlank()) {
            throw new IllegalArgumentException("Từ khóa ảnh không được để trống");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category ảnh không được để trống");
        }
        if (!ALLOWED_CATEGORIES.contains(category.trim())) {
            throw new IllegalArgumentException("Category ảnh không hợp lệ");
        }
    }

    private List<Float> embedImageMetadata(String text) {
        EmbeddingRequest req = new EmbeddingRequest();
        req.setTexts(List.of(tokenizerService.segmentForEmbedding(text)));
        req.setMode("passage");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<EmbeddingResponse> response = aiRestTemplate.exchange(
                aiBaseUrl + "/v1/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                EmbeddingResponse.class);

        if (response.getBody() == null || response.getBody().getEmbeddings().isEmpty()) {
            throw new RuntimeException("Lỗi sinh embedding cho metadata ảnh");
        }
        return response.getBody().getEmbeddings().get(0);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static float[] toFloatArray(List<Float> vec) {
        float[] arr = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            arr[i] = vec.get(i);
        }
        return arr;
    }
}
