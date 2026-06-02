package com.tomzxy.fbu_chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Data
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "text")
    private String content;

    /**
     * Vector embedding 384 chiều (e5-small-v2).
     * Dùng float[] + @Array(length) theo yêu cầu của hibernate-vector module.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "doc_type")
    private String docType;

    private Integer year;

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String section;

    @Column(name = "parent_id")
    private UUID parentId;
}
