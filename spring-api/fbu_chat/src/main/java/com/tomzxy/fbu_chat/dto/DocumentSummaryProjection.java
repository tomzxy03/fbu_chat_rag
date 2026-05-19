package com.tomzxy.fbu_chat.dto;

/**
 * Spring Data Projection cho kết quả GROUP BY query trên document_chunks.
 * COUNT(*) trong native query trả về Long — không dùng Integer ở đây.
 */
public interface DocumentSummaryProjection {
    String getFilename();
    Integer getYear();
    String getDocType();
    Long getChunkCount();
}
