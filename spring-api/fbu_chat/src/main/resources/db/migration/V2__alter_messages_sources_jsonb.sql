-- Đổi cột sources từ TEXT sang JSONB để nhất quán với code Java (ObjectMapper serialize JSON)
-- Dữ liệu TEXT hợp lệ JSON sẽ được cast tự động; NULL giữ nguyên NULL.
ALTER TABLE messages
    ALTER COLUMN sources TYPE JSONB USING sources::jsonb;

-- Thêm index cho conversations theo user để tăng tốc query getUserConversations
CREATE INDEX IF NOT EXISTS idx_conversations_user_updated
    ON conversations (user_id, updated_at DESC);
