package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.dto.ImageResult;
import com.tomzxy.fbu_chat.entity.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentImageRepository extends JpaRepository<DocumentImage, UUID> {

    List<DocumentImage> findAllByOrderByUploadedAtDesc();

    @Query(value = """
            SELECT minio_url AS url,
                   caption,
                   tags,
                   category,
                   1 - (tag_embedding <=> CAST(:queryVector AS vector)) AS score
            FROM document_images
            WHERE tag_embedding IS NOT NULL
              AND 1 - (tag_embedding <=> CAST(:queryVector AS vector)) >= :threshold
            ORDER BY tag_embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<ImageResult> findSimilarImages(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("threshold") double threshold);

    /**
     * Tìm ảnh tiếp theo trong cùng category, loại trừ các URL đã hiển thị.
     * Dùng cho follow-up "còn ảnh khác không?" để search đúng ngữ cảnh (category)
     * thay vì embed lại câu follow-up không có context.
     * excludeUrls là mảng text[] của PostgreSQL.
     */
    @Query(value = """
            SELECT minio_url AS url,
                   caption,
                   tags,
                   category,
                   1 - (tag_embedding <=> CAST(:queryVector AS vector)) AS score
            FROM document_images
            WHERE tag_embedding IS NOT NULL
              AND category = :category
              AND minio_url != ALL(CAST(:excludeUrls AS text[]))
              AND 1 - (tag_embedding <=> CAST(:queryVector AS vector)) >= :threshold
            ORDER BY tag_embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<ImageResult> findSimilarImagesByCategory(
            @Param("queryVector") String queryVector,
            @Param("category") String category,
            @Param("excludeUrls") String excludeUrls,
            @Param("topK") int topK,
            @Param("threshold") double threshold);
}
