package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying
    @Query("DELETE FROM DocumentChunk d WHERE d.sourceFile = :sourceFile")
    void deleteBySourceFile(String sourceFile);

    /** Tìm kiếm vector không filter — dùng khi year và docType đều null */
    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding <=> CAST(:queryVector AS vector) LIMIT :topK",
            nativeQuery = true)
    List<DocumentChunk> findTopRelatedContexts(String queryVector, int topK);

    /**
     * Tìm kiếm vector với filter tùy chọn theo year và/hoặc docType.
     * Truyền null để bỏ qua filter tương ứng.
     */
    @Query(value = """
            SELECT * FROM document_chunks
            WHERE (:year    IS NULL OR year     = :year)
              AND (:docType IS NULL OR doc_type = :docType)
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunk> findTopRelatedContextsFiltered(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("year") Integer year,
            @Param("docType") String docType);
}
