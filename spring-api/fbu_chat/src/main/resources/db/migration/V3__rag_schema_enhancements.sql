-- V3: Comprehensive schema enhancements for RAG (HNSW, FTS, Parent-Child, Unaccent)

-- 1. Đổi IVFFlat → HNSW cho vector search
DROP INDEX IF EXISTS idx_chunks_embedding;
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 2. Thêm cột section bị thiếu trong init.sql cho DB hiện tại
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS section VARCHAR(500);

-- 3. Tạo bảng parent_chunks cho chunking phân cấp
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

CREATE INDEX IF NOT EXISTS idx_parent_chunks_source
    ON parent_chunks(source_file);

-- Thêm Foreign Key parent_id
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS parent_id UUID
        REFERENCES parent_chunks(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_chunks_parent_id
    ON document_chunks(parent_id);

-- 4. Thêm Full-Text Search cơ bản (ts_content)
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS ts_content tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

CREATE INDEX IF NOT EXISTS idx_chunks_fts
    ON document_chunks USING gin(ts_content);

-- 5. Bật Unaccent và tạo GIN index cho tìm kiếm không dấu
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
  SELECT public.unaccent($1);
$$ LANGUAGE sql IMMUTABLE STRICT;

CREATE INDEX IF NOT EXISTS idx_chunks_fts_unaccent
    ON document_chunks USING GIN(to_tsvector('simple', immutable_unaccent(content)));
