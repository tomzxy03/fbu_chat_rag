package com.tomzxy.fbu_chat.repository;

import com.tomzxy.fbu_chat.entity.ParentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentChunkRepository extends JpaRepository<ParentChunk, UUID> {

    /**
     * Xóa tất cả parent chunks theo source_file.
     * Cascade ON DELETE sẽ tự xóa child document_chunks.parent_id liên quan.
     */
    @Modifying
    @Query("DELETE FROM ParentChunk p WHERE p.sourceFile = :sourceFile")
    void deleteBySourceFile(String sourceFile);

    List<ParentChunk> findBySourceFile(String sourceFile);
}
