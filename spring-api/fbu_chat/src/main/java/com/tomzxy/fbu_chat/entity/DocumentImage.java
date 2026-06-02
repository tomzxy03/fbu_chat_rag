package com.tomzxy.fbu_chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "minio_url", nullable = false, length = 500)
    private String minioUrl;

    @Column(length = 300)
    private String caption;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tags;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    @Column(name = "tag_embedding", columnDefinition = "vector(384)")
    private float[] tagEmbedding;

    @Column(length = 100)
    private String category;

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (tags == null) {
            tags = "";
        }
    }
}
