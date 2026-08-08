package com.tomzxy.fbu_chat.service;

import com.tomzxy.fbu_chat.dto.ImageCategoryDto;
import com.tomzxy.fbu_chat.entity.ImageCategory;
import com.tomzxy.fbu_chat.repository.ImageCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCategoryService {

    private final ImageCategoryRepository categoryRepository;

    /** Danh sách category active cho dropdown upload UI */
    @Transactional(readOnly = true)
    public List<ImageCategoryDto> listActive() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toDto).toList();
    }

    /** Danh sách tất cả category (kể cả inactive) cho admin quản lý */
    @Transactional(readOnly = true)
    public List<ImageCategoryDto> listAll() {
        return categoryRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Validate code khi upload ảnh.
     * Thay thế ALLOWED_CATEGORIES hardcode trong ImageService.
     */
    @Transactional(readOnly = true)
    public boolean isValidCode(String code) {
        return code != null && categoryRepository.findByCode(code.trim())
                .map(ImageCategory::isActive)
                .orElse(false);
    }

    /** Lấy set code active — cache trong memory nếu cần, hiện tại query trực tiếp đủ dùng */
    @Transactional(readOnly = true)
    public Set<String> getActiveCodes() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(ImageCategory::getCode).collect(Collectors.toSet());
    }

    @Transactional
    public ImageCategoryDto create(String code, String label, String description, int sortOrder) {
        if (categoryRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Category code '" + code + "' đã tồn tại");
        }
        ImageCategory category = ImageCategory.builder()
                .code(code.trim().toLowerCase())
                .label(label.trim())
                .description(description)
                .sortOrder(sortOrder)
                .active(true)
                .build();
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public ImageCategoryDto setActive(Integer id, boolean active) {
        ImageCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category không tồn tại: " + id));
        category.setActive(active);
        return toDto(categoryRepository.save(category));
    }

    private ImageCategoryDto toDto(ImageCategory c) {
        return ImageCategoryDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .label(c.getLabel())
                .description(c.getDescription())
                .sortOrder(c.getSortOrder())
                .active(c.isActive())
                .build();
    }
}
