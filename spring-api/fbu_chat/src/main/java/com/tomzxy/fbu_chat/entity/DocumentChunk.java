package com.tomzxy.fbu_chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
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

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(384)")
    private List<Float> embedding;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "doc_type")
    private String docType;

    private Integer year;
}
