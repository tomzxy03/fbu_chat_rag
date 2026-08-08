-- V6: Tách category ảnh ra bảng riêng thay vì hardcode trong code
-- Admin có thể thêm/sửa/xóa category qua API mà không cần redeploy

CREATE TABLE IF NOT EXISTS image_categories (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,  -- slug dùng trong DB: co_so_vat_chat
    label       VARCHAR(200) NOT NULL,          -- tên hiển thị: Cơ sở vật chất
    description VARCHAR(500),                   -- mô tả ngắn cho admin UI
    sort_order  INT NOT NULL DEFAULT 0,         -- thứ tự hiển thị trong dropdown
    active      BOOLEAN NOT NULL DEFAULT TRUE,  -- ẩn/hiện mà không cần xóa
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Seed dữ liệu từ ALLOWED_CATEGORIES cũ
INSERT INTO image_categories (code, label, sort_order) VALUES
    ('co_so_vat_chat', 'Cơ sở vật chất',    1),
    ('khuon_vien',     'Khuôn viên',         2),
    ('giang_duong',    'Giảng đường',         3),
    ('thu_vien',       'Thư viện',            4),
    ('phong_thuc_hanh','Phòng thực hành',     5),
    ('the_thao',       'Thể thao',            6),
    ('su_kien',        'Sự kiện',             7),
    ('logo',           'Logo',                8),
    ('tai_lieu',       'Tài liệu',            9),
    ('khac',           'Khác',               10)
ON CONFLICT (code) DO NOTHING;

-- FK từ document_images → image_categories (loose coupling qua code, không phải id)
-- Không dùng FK cứng vì category có thể bị đổi code trong tương lai
-- Constraint check sẽ được xử lý ở application layer
