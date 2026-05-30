-- V3: RAG Schema Framework (Bao gồm bảng mảng, index, unaccent, chunk phân cấp)
-- Đảm bảo chạy theo đúng thứ tự khởi tạo!

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Wrapper tạo hàm GIN Index không dấu
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
  SELECT public.unaccent($1);
$$ LANGUAGE sql IMMUTABLE STRICT;

-- 1. Bảng lưu trữ nội dung cốt lõi của RAG
CREATE TABLE IF NOT EXISTS document_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    embedding   VECTOR(384),
    source_file VARCHAR(255),
    chunk_index INTEGER,
    doc_type    VARCHAR(100) DEFAULT 'general',
    year        INTEGER DEFAULT 2026,
    section     VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Bảng Parent chunks 
CREATE TABLE IF NOT EXISTS parent_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_file VARCHAR(255) NOT NULL,
    heading     TEXT,
    content     TEXT NOT NULL,
    year        INTEGER,
    doc_type    VARCHAR(100),
    title       VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_parent_chunks_source ON parent_chunks(source_file);

-- 3. Tạo ràng buộc và Các Index tìm kiếm
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES parent_chunks(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_chunks_parent_id ON document_chunks(parent_id);

CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS ts_content tsvector 
    GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;
CREATE INDEX IF NOT EXISTS idx_chunks_fts ON document_chunks USING gin(ts_content);

CREATE INDEX IF NOT EXISTS idx_chunks_fts_unaccent
    ON document_chunks USING GIN(to_tsvector('simple', immutable_unaccent(content)));

CREATE INDEX IF NOT EXISTS idx_chunks_year      ON document_chunks (year);
CREATE INDEX IF NOT EXISTS idx_chunks_doc_type  ON document_chunks (doc_type);
