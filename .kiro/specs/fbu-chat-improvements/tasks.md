# Implementation Plan: FBU Chat Improvements

## Overview

Triển khai 5 nhóm cải tiến cho hệ thống FBU Chat RAG: nâng cấp rate limiting bằng Bucket4j, thêm Document List/Delete API, bảo mật DocumentController chỉ cho ADMIN, cập nhật Admin UI, và tạo re-ingest script.

## Tasks

- [x] 1. Thêm Bucket4j dependency vào pom.xml
  - Thêm `com.bucket4j:bucket4j-core:8.10.1` vào `spring-api/fbu_chat/pom.xml`
  - Không cần `bucket4j-spring-boot-starter` — chỉ cần `bucket4j-core` cho in-memory usage
  - _Requirements: 2.5_

- [x] 2. Cập nhật SecurityConfig — mở rộng ADMIN rules cho DocumentController
  - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/security/SecurityConfig.java`
  - Thay rule `HttpMethod.POST, "/api/documents/**"` thành `.requestMatchers("/api/documents/**").hasRole("ADMIN")` để cover GET, POST, DELETE
  - Đặt rule mới trước `anyRequest()` để đảm bảo thứ tự xử lý đúng
  - _Requirements: 3.5, 3.6, 4.5 (implicit), 5.4, 5.5, 6.1, 6.2, 6.3, 6.4_

- [ ] 3. Nâng cấp RateLimitFilter lên Bucket4j
  - [ ] 3.1 Implement RateLimitFilter mới với Bucket4j
    - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/security/RateLimitFilter.java`
    - Inject `JwtUtil` để parse JWT thủ công (vì filter chạy trước JwtFilter, SecurityContextHolder chưa có auth)
    - Anonymous: key = `"ip:" + clientIp`, dùng `Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))`
    - Authenticated: key = `"user:" + username`, dùng `Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)))`
    - Dùng `ConcurrentHashMap<String, Bucket>` để lưu state in-memory
    - Chỉ áp dụng rate limit cho `POST /api/chat`; các request khác pass through
    - Khi vượt limit: trả HTTP 429 với JSON body `{ "status": 429, "error": "Too Many Requests", "message": "Bạn đã gửi quá nhiều tin nhắn. Vui lòng đợi {N} giây trước khi gửi tiếp." }`
    - Wrap JWT parsing trong try-catch, fallback về IP-based key nếu token invalid
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  - [ ]* 3.2 Viết property test cho anonymous rate limit (Property 1)
    - **Property 1: Anonymous rate limit enforcement**
    - **Validates: Requirements 2.1, 2.3**
    - Dùng jqwik: với bất kỳ IP string nào, sau đúng 10 request trong 60 giây, request thứ 11 phải nhận HTTP 429 với `message` field
  - [ ]* 3.3 Viết property test cho authenticated rate limit (Property 2)
    - **Property 2: Authenticated rate limit enforcement**
    - **Validates: Requirements 2.2, 2.4**
    - Dùng jqwik: với bất kỳ userId nào, sau đúng 30 request trong 60 giây, request thứ 31 phải nhận HTTP 429 với `message` field
  - [ ]* 3.4 Viết unit tests cho RateLimitFilter
    - Test key isolation: requests từ các IP khác nhau không ảnh hưởng nhau
    - Test response body: 429 response có đủ các field `status`, `error`, `message`
    - Test non-chat endpoints không bị rate limit
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 4. Tạo DocumentSummaryProjection interface
  - Tạo file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/dto/DocumentSummaryProjection.java`
  - Interface với 4 getter methods: `getFilename()` (String), `getYear()` (Integer), `getDocType()` (String), `getChunkCount()` (Long — COUNT(*) trả về Long trong native query)
  - _Requirements: 3.2, 3.4_

- [x] 5. Tạo DocumentSummaryDto class
  - Tạo file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/dto/DocumentSummaryDto.java`
  - Dùng Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
  - Fields: `filename` (String), `year` (Integer), `docType` (String), `chunkCount` (Integer)
  - _Requirements: 3.2_

- [ ] 6. Thêm findAllSummaries query vào DocumentChunkRepository
  - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/repository/DocumentChunkRepository.java`
  - Thêm native query với `@Query(nativeQuery = true)`:
    ```sql
    SELECT source_file AS filename, year, doc_type AS docType, COUNT(*) AS chunkCount
    FROM document_chunks GROUP BY source_file, year, doc_type ORDER BY source_file
    ```
  - Return type: `List<DocumentSummaryProjection>`
  - Thêm method `deleteBySourceFile(String sourceFile)` với `@Transactional` nếu chưa có
  - _Requirements: 3.4, 5.6_

- [ ] 7. Mở rộng DocumentService — thêm listDocuments() và deleteDocument()
  - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/service/DocumentService.java`
  - `listDocuments()`: gọi `repository.findAllSummaries()`, convert từng `DocumentSummaryProjection` → `DocumentSummaryDto` (chú ý cast `Long` chunkCount → `Integer`)
  - `deleteDocument(String filename)`: gọi `repository.deleteBySourceFile(filename)`, không throw exception nếu file không tồn tại (idempotent)
  - _Requirements: 3.2, 3.3, 5.2, 5.3_

- [ ] 8. Mở rộng DocumentController — thêm GET và DELETE endpoints
  - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/controller/DocumentController.java`
  - Thêm `GET /api/documents` → gọi `documentService.listDocuments()`, trả `ResponseEntity<List<DocumentSummaryDto>>`
  - Thêm `DELETE /api/documents/{filename}` → gọi `documentService.deleteDocument(filename)`, trả `ResponseEntity<Map<String, String>>` với message
  - Xóa `@CrossOrigin(origins = "*")` nếu có (security risk — CORS đã được xử lý ở Nginx/SecurityConfig)
  - _Requirements: 3.1, 3.2, 3.3, 5.1, 5.2, 5.3_

- [ ] 9. Checkpoint — Kiểm tra Spring API
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Thêm GlobalExceptionHandler cho HTTP 429
  - Sửa file `spring-api/fbu_chat/src/main/java/com/tomzxy/fbu_chat/exception/GlobalExceptionHandler.java`
  - Verify handler đã có cho `IllegalArgumentException` → 400 và generic `RuntimeException` → 500
  - Thêm handler cho custom rate limit exception nếu RateLimitFilter dùng exception thay vì trực tiếp write response
  - Đảm bảo response format nhất quán với `{ "status", "error", "message" }` pattern
  - _Requirements: 2.3, 2.4_

- [x] 11. Cập nhật index.html — thêm document list section
  - Sửa file `chat-ui/index.html`
  - Thêm `<div id="doc-list-section">` bên dưới upload form trong `#admin-section`:
    ```html
    <div id="doc-list-section">
        <h4>📋 Tài liệu đã ingest</h4>
        <div id="doc-list-container">
            <p class="loading-text">Đang tải...</p>
        </div>
    </div>
    ```
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 12. Cập nhật app.js — thêm loadDocuments() và deleteDocument()
  - Sửa file `chat-ui/app.js`
  - `loadDocuments()`: gọi `GET /api/documents`, render bảng với cột filename/year/docType/chunkCount và nút "Xóa" cho mỗi row; hiển thị "Chưa có tài liệu nào" nếu mảng rỗng; hiển thị lỗi trong `#doc-list-container` nếu request thất bại
  - `deleteDocument(filename)`: gọi `DELETE /api/documents/{encodeURIComponent(filename)}`, gọi `loadDocuments()` sau khi thành công; hiển thị lỗi trong `#upload-status` nếu thất bại
  - Gọi `loadDocuments()` tại 3 thời điểm: (1) khi `updateAuthUI()` phát hiện `user.role === 'ADMIN'`, (2) sau upload thành công, (3) sau xóa thành công
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [ ] 13. Tạo re-ingest script
  - [x] 13.1 Tạo file ai-service/reingest.py
    - Tạo file `ai-service/reingest.py`
    - Dùng `argparse`: `--token` (ADMIN JWT, fallback env `ADMIN_TOKEN`), `--base-url` (default `http://localhost:8080`), `--pdf-dir` (default `./pdf_input`)
    - Đọc tất cả file `.pdf` từ `--pdf-dir`
    - Với mỗi file: gửi multipart POST đến `{base-url}/api/documents/ingest` với `Authorization: Bearer {token}`, timeout 120 giây
    - In progress: `✅ {filename} — {chunkCount} chunks` khi thành công
    - Xử lý lỗi: `ConnectionError` → log `❌ {filename} — Lỗi kết nối: {error}`, tiếp tục; HTTP 4xx/5xx → log `❌ {filename} — HTTP {status}: {message}`, tiếp tục; file không đọc được → log `❌ {filename} — Không thể đọc file`, tiếp tục
    - Thiếu token: exit với hướng dẫn sử dụng
    - In tổng kết cuối: `Thành công: N, Thất bại: M`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_
  - [ ]* 13.2 Viết property test cho error resilience (Property 7)
    - **Property 7: Re-ingest script error resilience**
    - **Validates: Requirements 1.5**
    - Dùng Hypothesis: với bất kỳ danh sách PDF nào và tập con các index thất bại, script phải attempt tất cả file và không abort sau lỗi đầu tiên
  - [ ]* 13.3 Viết property test cho summary accuracy (Property 8)
    - **Property 8: Re-ingest script summary accuracy**
    - **Validates: Requirements 1.6**
    - Dùng Hypothesis: với N file và K file thất bại, summary phải báo đúng (N-K) thành công và K thất bại

- [ ] 14. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks đánh dấu `*` là optional và có thể bỏ qua để triển khai MVP nhanh hơn
- Mỗi task tham chiếu requirements cụ thể để đảm bảo traceability
- Checkpoint tại Task 9 và Task 14 để validate incremental progress
- Property tests dùng jqwik (Java) cho Spring API và Hypothesis (Python) cho re-ingest script
- Task 1 phải hoàn thành trước Task 3 (Bucket4j dependency)
- Task 2 phải hoàn thành trước Task 8 (SecurityConfig rules)
- Task 4 phải hoàn thành trước Task 6 (Projection interface)
- Task 5 và Task 6 phải hoàn thành trước Task 7 (DTO + Repository)
- Task 7 phải hoàn thành trước Task 8 (Service methods)
- Task 11 phải hoàn thành trước Task 12 (HTML structure trước JS logic)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2", "4", "5", "11", "13.1"] },
    { "id": 1, "tasks": ["3.1", "6"] },
    { "id": 2, "tasks": ["3.2", "3.3", "3.4", "7"] },
    { "id": 3, "tasks": ["8", "10", "12"] },
    { "id": 4, "tasks": ["13.2", "13.3"] }
  ]
}
```
