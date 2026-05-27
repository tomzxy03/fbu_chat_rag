package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.dto.ChunkResult;
import com.tomzxy.fbu_chat.dto.DocumentSummaryProjection;
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

        /**
         * Vector search không filter, có similarity threshold.
         * Chỉ trả về chunks có cosine distance < threshold (tức similarity > 1 -
         * threshold).
         * distance = 0 là giống hoàn toàn, distance = 2 là ngược hoàn toàn.
         */
        @Query(value = """
                        SELECT content, source_file AS sourceFile, year, doc_type AS docType, parent_id AS parentId
                        FROM document_chunks
                        WHERE embedding <=> CAST(:queryVector AS vector) < :threshold
                        ORDER BY embedding <=> CAST(:queryVector AS vector)
                        LIMIT :topK
                        """, nativeQuery = true)
        List<ChunkResult> findTopRelatedContexts(
                        @Param("queryVector") String queryVector,
                        @Param("topK") int topK,
                        @Param("threshold") double threshold);

        /**
         * Vector search với filter tùy chọn theo year và/hoặc docType + similarity
         * threshold.
         */
        @Query(value = """
                        SELECT content, source_file AS sourceFile, year, doc_type AS docType, parent_id AS parentId
                        FROM document_chunks
                        WHERE embedding <=> CAST(:queryVector AS vector) < :threshold
                          AND (:year    IS NULL OR year     = :year)
                          AND (:docType IS NULL OR doc_type = :docType)
                        ORDER BY embedding <=> CAST(:queryVector AS vector)
                        LIMIT :topK
                        """, nativeQuery = true)
        List<ChunkResult> findTopRelatedContextsFiltered(
                        @Param("queryVector") String queryVector,
                        @Param("topK") int topK,
                        @Param("year") Integer year,
                        @Param("docType") String docType,
                        @Param("threshold") double threshold);

        /**
         * Hybrid search: kết hợp vector similarity + full-text keyword search dùng RRF.
         *
         * RRF (Reciprocal Rank Fusion) merge 2 ranking:
         * - Vector search: tìm theo ngữ nghĩa
         * - Full-text search: tìm theo từ khóa chính xác (số điều, mã văn bản, tên
         * riêng)
         *
         * Chunk xuất hiện trong cả 2 kết quả được boost lên đầu.
         * tsQuery: câu query đã convert sang PostgreSQL tsquery (dùng TsQueryBuilder).
         */
        @Query(value = """
                        WITH vector_ranked AS (
                            SELECT id,
                                   ROW_NUMBER() OVER (ORDER BY embedding <=> CAST(:queryVector AS vector)) AS rank
                            FROM document_chunks
                            WHERE embedding <=> CAST(:queryVector AS vector) < :threshold
                            LIMIT :topK
                        ),
                        fts_ranked AS (
                            SELECT id,
                                   ROW_NUMBER() OVER (ORDER BY ts_rank(ts_content, query) DESC) AS rank
                            FROM document_chunks,
                                 to_tsquery('simple', :tsQuery) query
                            WHERE ts_content @@ query
                            LIMIT :topK
                        ),
                        combined AS (
                            SELECT COALESCE(v.id, f.id) AS id,
                                   COALESCE(1.0 / (60 + v.rank), 0) + COALESCE(1.0 / (60 + f.rank), 0) AS rrf_score
                            FROM vector_ranked v
                            FULL OUTER JOIN fts_ranked f ON v.id = f.id
                        )
                        SELECT dc.content,
                               dc.source_file AS sourceFile,
                               dc.year,
                               dc.doc_type    AS docType,
                               dc.parent_id   AS parentId
                        FROM combined c
                        JOIN document_chunks dc ON dc.id = c.id
                        ORDER BY c.rrf_score DESC
                        LIMIT :topK
                        """, nativeQuery = true)
        List<ChunkResult> hybridSearch(
                        @Param("queryVector") String queryVector,
                        @Param("tsQuery") String tsQuery,
                        @Param("topK") int topK,
                        @Param("threshold") double threshold);

        /**
         * Tổng hợp danh sách tài liệu đã ingest, group theo source_file.
         * Dùng Spring Data Projection để tránh constructor mapping phức tạp với native
         * query.
         * COUNT(*) trả về Long — DocumentSummaryProjection.getChunkCount() là Long.
         */
        @Query(value = """
                        SELECT source_file  AS filename,
                               year,
                               doc_type     AS docType,
                               COUNT(*)     AS chunkCount
                        FROM document_chunks
                        GROUP BY source_file, year, doc_type
                        ORDER BY source_file
                        """, nativeQuery = true)
        List<DocumentSummaryProjection> findAllSummaries();
}
