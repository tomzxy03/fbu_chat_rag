package com.tomzxy.fbu_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryDto {
    private String filename;
    private Integer year;
    private String docType;
    private Integer chunkCount;
}
