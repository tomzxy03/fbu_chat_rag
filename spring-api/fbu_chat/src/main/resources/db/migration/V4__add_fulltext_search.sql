-- Thêm full-text search cho hybrid search (vector + keyword)
-- Dùng tiếng Việt không dấu (simple config) vì PostgreSQL không có Vietnamese stemmer

ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS ts_content tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

CREATE INDEX IF NOT EXISTS idx_chunks_fts
    ON document_chunks USING gin(ts_content);
