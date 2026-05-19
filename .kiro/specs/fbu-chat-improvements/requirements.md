# Requirements Document

## Introduction

Feature này cải tiến hệ thống FBU Chat RAG hiện tại trên stack Spring Boot 3.5 + FastAPI + PostgreSQL/pgvector. Có 5 nhóm cải tiến: (1) sửa lỗi embedding prefix sai bằng re-ingest script, (2) bảo vệ Groq API quota bằng rate limiting, (3) thêm Document List API + UI để quản lý tài liệu, (4) thêm Delete Document API, và (5) bảo mật DocumentController chỉ cho phép ADMIN.

## Glossary

- **System**: Hệ thống FBU Chat RAG bao gồm Spring Boot API, FastAPI AI Service, và PostgreSQL/pgvector.
- **Spring_API**: Ứng dụng Spring Boot 3.5 xử lý REST API, JWT auth, và JPA/Hibernate.
- **AI_Service**: Ứng dụng FastAPI Python thực hiện chunking PDF và sinh embedding bằng mô hình e5-small-v2.
- **DocumentChunk**: Entity lưu trữ một đoạn văn bản từ tài liệu PDF, kèm vector embedding 384 chiều, trong bảng `document_chunks`.
- **DocumentController**: Spring REST controller xử lý các endpoint `/api/documents/*`.
- **DocumentService**: Spring service thực hiện logic ingest tài liệu (chunking + embedding + lưu DB).
- **DocumentChunkRepository**: Spring Data JPA repository thao tác với bảng `document_chunks`.
- **RateLimiter**: Component Bucket4j in-memory thực hiện giới hạn tốc độ request.
- **Re-ingest_Script**: Script Python command-line gọi API `/api/documents/ingest` để nạp lại toàn bộ PDF.
- **ADMIN**: Role người dùng có quyền quản trị hệ thống, được xác định qua JWT claim.
- **Anonymous_User**: Người dùng chưa đăng nhập, không có JWT token.
- **Authenticated_User**: Người dùng đã đăng nhập, có JWT token hợp lệ với role USER hoặc ADMIN.
- **passage_prefix**: Tiền tố `passage:` được thêm vào văn bản tài liệu trước khi sinh embedding với mô hình e5-small-v2.
- **query_prefix**: Tiền tố `query:` được thêm vào câu hỏi trước khi sinh embedding với mô hình e5-small-v2.
- **HTTP_429**: HTTP status code "Too Many Requests" trả về khi vượt giới hạn rate limit.
- **Bucket4j**: Thư viện Java in-memory rate limiting dựa trên thuật toán token bucket.
- **Admin_UI**: Phần giao diện người dùng trong `chat-ui/app.js` chỉ hiển thị khi user có role ADMIN.

---

## Requirements

### Requirement 1: Re-ingest Script để sửa lỗi embedding prefix

**User Story:** As a system administrator, I want a command-line script that re-ingests all PDFs from the `ai-service/pdf_input/` directory, so that all document embeddings are regenerated with the correct `passage:` prefix and vector search quality is restored.

#### Acceptance Criteria

1. THE Re-ingest_Script SHALL đọc toàn bộ file PDF từ thư mục `ai-service/pdf_input/` và gọi endpoint `POST /api/documents/ingest` cho từng file.
2. WHEN Re-ingest_Script được chạy từ command line, THE Re-ingest_Script SHALL in ra progress output cho từng file đang được xử lý, bao gồm tên file và trạng thái (thành công/thất bại).
3. THE Re-ingest_Script SHALL nhận ADMIN JWT token qua tham số command-line hoặc biến môi trường để xác thực với Spring_API.
4. WHEN một file PDF được ingest thành công, THE Re-ingest_Script SHALL in ra số lượng chunk đã được tạo cho file đó.
5. IF một file PDF không thể ingest (lỗi mạng, lỗi server), THEN THE Re-ingest_Script SHALL ghi log lỗi và tiếp tục xử lý các file còn lại thay vì dừng toàn bộ quá trình.
6. WHEN tất cả file đã được xử lý, THE Re-ingest_Script SHALL in ra tổng kết: số file thành công, số file thất bại.
7. THE Re-ingest_Script SHALL hỗ trợ tham số `--base-url` để chỉ định URL của Spring_API (mặc định: `http://localhost:8080`).

---

### Requirement 2: Rate Limiting trên endpoint `/api/chat`

**User Story:** As a system operator, I want rate limiting on the `/api/chat` endpoint, so that anonymous users cannot exhaust the Groq API quota and authenticated users have a higher allowance.

#### Acceptance Criteria

1. WHILE Anonymous_User gửi request đến `POST /api/chat`, THE RateLimiter SHALL cho phép tối đa 10 request mỗi phút tính theo địa chỉ IP của client.
2. WHILE Authenticated_User gửi request đến `POST /api/chat`, THE RateLimiter SHALL cho phép tối đa 30 request mỗi phút tính theo user ID từ JWT token.
3. WHEN Anonymous_User vượt quá 10 request/phút từ cùng một IP, THE Spring_API SHALL trả về HTTP_429 với body JSON chứa trường `message` mô tả rõ lý do và thời gian chờ.
4. WHEN Authenticated_User vượt quá 30 request/phút, THE Spring_API SHALL trả về HTTP_429 với body JSON chứa trường `message` mô tả rõ lý do và thời gian chờ.
5. THE RateLimiter SHALL được implement bằng Bucket4j in-memory, không yêu cầu Redis hoặc external cache.
6. THE RateLimiter SHALL reset bucket của mỗi client sau mỗi 60 giây kể từ request đầu tiên trong window hiện tại.
7. WHEN Spring_API khởi động lại, THE RateLimiter SHALL bắt đầu với bucket mới cho tất cả clients (in-memory state không được persist).

---

### Requirement 3: Document List API

**User Story:** As an ADMIN, I want an API endpoint that returns the list of ingested documents with metadata, so that I can see what documents are currently in the system.

#### Acceptance Criteria

1. THE DocumentController SHALL cung cấp endpoint `GET /api/documents` trả về danh sách các tài liệu đã ingest.
2. WHEN `GET /api/documents` được gọi, THE DocumentController SHALL trả về HTTP 200 với body JSON là mảng các object, mỗi object chứa: `filename` (String), `year` (Integer), `docType` (String), `chunkCount` (Integer).
3. WHEN không có tài liệu nào trong database, THE DocumentController SHALL trả về HTTP 200 với body JSON là mảng rỗng `[]`.
4. THE DocumentController SHALL tổng hợp dữ liệu từ bảng `document_chunks` bằng cách group theo `source_file` để tính `chunkCount` cho mỗi tài liệu.
5. WHEN `GET /api/documents` được gọi bởi người dùng không có role ADMIN, THE Spring_API SHALL trả về HTTP 403.
6. WHEN `GET /api/documents` được gọi mà không có JWT token, THE Spring_API SHALL trả về HTTP 401.

---

### Requirement 4: Document List UI cho Admin

**User Story:** As an ADMIN, I want to see the list of ingested documents in the admin UI with a delete button for each document, so that I can manage the document store without using the API directly.

#### Acceptance Criteria

1. WHEN Admin_UI được hiển thị cho ADMIN user, THE Admin_UI SHALL render một bảng danh sách tài liệu bằng cách gọi `GET /api/documents`.
2. THE Admin_UI SHALL hiển thị các cột: tên file, năm, loại tài liệu, số chunk cho mỗi tài liệu trong danh sách.
3. WHEN danh sách tài liệu trả về rỗng, THE Admin_UI SHALL hiển thị thông báo "Chưa có tài liệu nào" thay vì bảng trống.
4. THE Admin_UI SHALL hiển thị nút "Xóa" cho mỗi tài liệu trong danh sách.
5. WHEN ADMIN nhấn nút "Xóa" cho một tài liệu, THE Admin_UI SHALL gọi `DELETE /api/documents/{filename}` và làm mới danh sách sau khi nhận phản hồi thành công.
6. IF xóa tài liệu thất bại (HTTP error), THEN THE Admin_UI SHALL hiển thị thông báo lỗi rõ ràng cho người dùng.

---

### Requirement 5: Delete Document API

**User Story:** As an ADMIN, I want an API endpoint to delete all chunks of a specific document by filename, so that I can remove outdated or incorrect documents from the vector store.

#### Acceptance Criteria

1. THE DocumentController SHALL cung cấp endpoint `DELETE /api/documents/{filename}` để xóa toàn bộ DocumentChunk có `source_file` khớp với `{filename}`.
2. WHEN `DELETE /api/documents/{filename}` được gọi với filename của tài liệu tồn tại, THE DocumentService SHALL xóa toàn bộ chunk của tài liệu đó và trả về HTTP 200.
3. WHEN `DELETE /api/documents/{filename}` được gọi với filename của tài liệu không tồn tại, THE DocumentController SHALL trả về HTTP 200 (idempotent — không có gì để xóa vẫn là thành công).
4. WHEN `DELETE /api/documents/{filename}` được gọi bởi người dùng không có role ADMIN, THE Spring_API SHALL trả về HTTP 403.
5. WHEN `DELETE /api/documents/{filename}` được gọi mà không có JWT token, THE Spring_API SHALL trả về HTTP 401.
6. THE DocumentChunkRepository SHALL cung cấp method xóa theo `source_file` trong một transaction duy nhất.

---

### Requirement 6: Bảo mật DocumentController — chỉ ADMIN được ingest

**User Story:** As a security-conscious operator, I want the document ingest endpoint to be restricted to ADMIN role only, so that regular authenticated users cannot upload arbitrary documents into the vector store.

#### Acceptance Criteria

1. WHEN `POST /api/documents/ingest` được gọi bởi Authenticated_User có role USER (không phải ADMIN), THE Spring_API SHALL trả về HTTP 403.
2. WHEN `POST /api/documents/ingest` được gọi mà không có JWT token, THE Spring_API SHALL trả về HTTP 401.
3. WHEN `POST /api/documents/ingest` được gọi bởi Authenticated_User có role ADMIN, THE DocumentController SHALL xử lý request bình thường và trả về HTTP 200 khi thành công.
4. THE DocumentController SHALL sử dụng annotation `@PreAuthorize("hasRole('ADMIN')")` hoặc cấu hình Spring Security tương đương để enforce ADMIN-only access trên tất cả các endpoint write (`POST /ingest`, `DELETE /{filename}`).
