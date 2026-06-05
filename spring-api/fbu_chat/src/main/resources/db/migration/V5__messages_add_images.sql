-- Thêm cột images vào messages để persist kết quả image search
-- Khi reload conversation, UI sẽ có đủ dữ liệu để render lại ảnh
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS images JSONB;
