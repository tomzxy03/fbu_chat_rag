-- Bật pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Bảng lưu các chunks tài liệu
CREATE TABLE IF NOT EXISTS document_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT NOT NULL,
    embedding   VECTOR(384),          -- e5-small-v2 = 384 dims
    source_file VARCHAR(255),
    chunk_index INTEGER,
    doc_type    VARCHAR(100) DEFAULT 'general',  -- 'qche', 'thong_bao', 'lich', 'general'...
    year        INTEGER DEFAULT 2026,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Index cosine distance để tìm kiếm nhanh
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_chunks_year      ON document_chunks (year);
CREATE INDEX IF NOT EXISTS idx_chunks_doc_type  ON document_chunks (doc_type);

-- Bảng hội thoại (Spring Boot quản lý)
CREATE TABLE IF NOT EXISTS conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255),
    user_id     VARCHAR(100),         -- sau khi có auth
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Bảng tin nhắn
CREATE TABLE IF NOT EXISTS messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant')),
    content         TEXT NOT NULL,
    sources         JSONB,            -- sources trả về từ AI
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages (conversation_id, created_at);
