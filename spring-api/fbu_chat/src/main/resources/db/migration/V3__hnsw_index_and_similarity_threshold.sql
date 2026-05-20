-- Đổi IVFFlat → HNSW cho vector search
-- HNSW: recall tốt hơn, không cần training data, phù hợp máy ít RAM
-- IVFFlat cần lists*3 vectors để hoạt động tốt và có recall thấp hơn

DROP INDEX IF EXISTS idx_chunks_embedding;

CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Thêm cột similarity_score để có thể filter theo threshold sau này
-- (không thêm cột vật lý — dùng computed trong query)
