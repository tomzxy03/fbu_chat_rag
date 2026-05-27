package com.tomzxy.fbu_chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    private String heading;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    private Integer year;

    @Column(name = "doc_type")
    private String docType;

    @Column(length = 500)
    private String title;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
