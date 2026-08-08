package com.tomzxy.fbu_chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageCategoryDto {
    private Integer id;
    private String code;
    private String label;
    private String description;
    private int sortOrder;
    private boolean active;
}
