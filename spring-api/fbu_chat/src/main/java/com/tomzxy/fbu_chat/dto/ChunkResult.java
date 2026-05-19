package com.tomzxy.fbu_chat.dto;

/**
 * Projection cho kết quả vector search.
 * Không include cột embedding để tránh lỗi type mapping PGvector → float[].
 * Native query chỉ SELECT các cột cần thiết cho RAG context.
 */
public interface ChunkResult {
    String getContent();
    String getSourceFile();
    Integer getYear();
    String getDocType();
}
