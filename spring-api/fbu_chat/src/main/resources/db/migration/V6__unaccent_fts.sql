-- Unaccent extension + immutable wrapper cho FTS không dấu
-- Cho phép user gõ "lich nghi le" vẫn match "lịch nghỉ lễ"

-- 1. Bật extension unaccent
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2. Tạo immutable wrapper (bắt buộc để dùng trong GIN index expression)
--    PostgreSQL yêu cầu function IMMUTABLE cho index expression,
--    nhưng unaccent() chỉ là STABLE → cần wrapper
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
  SELECT public.unaccent($1);
$$ LANGUAGE sql IMMUTABLE STRICT;

-- 3. GIN Index mới cho FTS không dấu trên cột content
--    Biểu thức này PHẢI khớp 100% với WHERE clause trong hybridSearch query
--    Nếu sai lệch 1 ký tự → Postgres sẽ bỏ qua index → Seq Scan → chậm
CREATE INDEX IF NOT EXISTS idx_chunks_fts_unaccent
    ON document_chunks
    USING GIN(to_tsvector('simple', immutable_unaccent(content)));
