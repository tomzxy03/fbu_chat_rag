-- Bật pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Bật unaccent extension + immutable wrapper (cho FTS không dấu)
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
  SELECT public.unaccent($1);
$$ LANGUAGE sql IMMUTABLE STRICT;

-- Bảng lưu các chunks tài liệu
-- Owner: postgres/init.sql (cần pgvector extension trước khi Flyway chạy)
CREATE TABLE IF NOT EXISTS document_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    embedding   VECTOR(384),          -- e5-small-v2 = 384 dims
    source_file VARCHAR(255),
    chunk_index INTEGER,
    doc_type    VARCHAR(100) DEFAULT 'general',  -- 'quy_che', 'thong_bao', 'lich', 'general'...
    year        INTEGER DEFAULT 2026,
    section     VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Index cosine distance để tìm kiếm nhanh
-- HNSW: recall tốt hơn IVFFlat, không cần training, phù hợp máy ít RAM
-- m=16: số connections mỗi node (trade-off: memory vs recall)
-- ef_construction=64: độ chính xác khi build index
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_chunks_year      ON document_chunks (year);
CREATE INDEX IF NOT EXISTS idx_chunks_doc_type  ON document_chunks (doc_type);

-- NOTE: conversations và messages được quản lý bởi Flyway (Spring Boot).
-- Xem: spring-api/src/main/resources/db/migration/V1__create_schema.sql
