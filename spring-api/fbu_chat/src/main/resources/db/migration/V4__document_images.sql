CREATE TABLE IF NOT EXISTS document_images (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    minio_url     VARCHAR(500) NOT NULL,
    caption       VARCHAR(300),
    tags          TEXT NOT NULL DEFAULT '',
    tag_embedding VECTOR(384),
    category      VARCHAR(100),
    uploaded_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_images_embedding
    ON document_images USING hnsw (tag_embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_images_category ON document_images(category);
