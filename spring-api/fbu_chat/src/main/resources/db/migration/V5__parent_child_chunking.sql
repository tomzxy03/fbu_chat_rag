-- Parent-Child Chunking: bảng parent_chunks + FK parent_id trên document_chunks
-- Parent = section ##, Child = sub-section ###/####

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

-- FK: xóa parent → cascade xóa child (tránh orphan khi re-ingest)
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS parent_id UUID
        REFERENCES parent_chunks(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_chunks_parent_id
    ON document_chunks(parent_id);
