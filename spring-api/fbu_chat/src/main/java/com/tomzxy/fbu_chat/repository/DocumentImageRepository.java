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
}
