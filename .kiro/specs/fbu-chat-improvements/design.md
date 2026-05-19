# Design Document: FBU Chat Improvements

## Overview

Feature này thực hiện 5 nhóm cải tiến cho hệ thống FBU Chat RAG:

1. **Re-ingest Script** — script Python command-line để nạp lại toàn bộ PDF với embedding prefix đúng
2. **Rate Limiting nâng cấp** — thay thế custom rate limiter bằng Bucket4j với giới hạn khác nhau cho anonymous/authenticated
3. **Document List API** — `GET /api/documents` trả về danh sách tài liệu đã ingest
4. **Document List UI** — bảng quản lý tài liệu trong Admin section
5. **Delete Document API** — `DELETE /api/documents/{filename}` xóa toàn bộ chunk của một tài liệu
6. **Bảo mật DocumentController** — enforce ADMIN-only cho tất cả write endpoints

**Phát hiện quan trọng từ code review:**
- `SecurityConfig.java` đã có rule `hasRole("ADMIN")` cho `POST /api/documents/**`, nhưng chưa cover `GET` và `DELETE`
- `RateLimitFilter.java` đã tồn tại với custom implementation (không dùng Bucket4j), giới hạn 20 req/phút flat cho tất cả users
- Cần nâng cấp RateLimitFilter để phân biệt anonymous (10/phút theo IP) vs authenticated (30/phút theo userId)
- `@EnableMethodSecurity` chưa có trong SecurityConfig — cần thêm nếu dùng `@PreAuthorize`

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         chat-ui (Nginx)                         │
│  index.html + app.js                                            │
│  - Admin section: upload form + document list table             │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP (Nginx proxy)
┌──────────────────────────▼──────────────────────────────────────┐
│                    Spring Boot 3.5 API                          │
│                                                                 │
│  Filter Chain:                                                  │
│  RateLimitFilter (Bucket4j) → JwtFilter → SecurityConfig        │
│                                                                 │
│  Controllers:                                                   │
│  ├── ChatController      POST /api/chat                         │
│  ├── DocumentController  POST /ingest (ADMIN)                   │
│  │                       GET  /       (ADMIN)                   │
│  │                       DELETE /{fn} (ADMIN)                   │
│  └── AuthController      POST /api/auth/**                      │
│                                                                 │
│  Services:                                                      │
│  ├── DocumentService     ingestDocument(), listDocuments(),     │
│  │                       deleteDocument()                       │
│  └── ChatService         ...                                    │
│                                                                 │
│  Repository:                                                    │
│  └── DocumentChunkRepository  + findAllSummaries() query        │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Internal Docker network
┌──────────────────────────▼──────────────────────────────────────┐
│                    FastAPI AI Service                           │
│  POST /chunk        — PDF chunking                              │
│  POST /v1/embeddings — embedding với passage:/query: prefix     │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                    PostgreSQL + pgvector                        │
│  table: document_chunks                                         │
│  (id, content, embedding, source_file, chunk_index,            │
│   doc_type, year)                                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                 ai-service/reingest.py (CLI)                    │
│  Đọc pdf_input/ → POST /api/documents/ingest (Bearer token)     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Components and Interfaces

### 1. RateLimitFilter (nâng cấp)

**File:** `spring-api/.../security/RateLimitFilter.java`

**Thay đổi:** Thay thế custom token bucket bằng Bucket4j. Phân biệt 2 loại key:
- Anonymous: key = `"ip:" + clientIp`, limit = 10 req/phút
- Authenticated: key = `"user:" + username`, limit = 30 req/phút

**Dependency mới trong pom.xml:**
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

**Logic:**
```
doFilterInternal(request, response, chain):
  if NOT (POST /api/chat):
    chain.doFilter(); return

  key = extractKey(request)
  bucket = buckets.computeIfAbsent(key, k -> buildBucket(isAuthenticated))
  probe = bucket.tryConsumeAndReturnRemaining(1)
  if probe.isConsumed():
    chain.doFilter()
  else:
    waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000
    response.status = 429
    response.body = { "status": 429, "error": "Too Many Requests",
                      "message": "Vui lòng đợi {waitSeconds} giây trước khi gửi tiếp." }
```

**extractKey logic:**
- Nếu có `Authorization: Bearer <token>` hợp lệ → parse username từ JWT → `"user:" + username`
- Ngược lại → `"ip:" + request.getRemoteAddr()`

> **Lưu ý:** RateLimitFilter chạy TRƯỚC JwtFilter trong filter chain (theo SecurityConfig hiện tại). Để lấy username từ JWT trong RateLimitFilter, cần inject `JwtUtil` và parse token thủ công — không dùng `SecurityContextHolder` vì context chưa được set tại thời điểm này.

### 2. DocumentController (mở rộng)

**File:** `spring-api/.../controller/DocumentController.java`

**Endpoints mới:**

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/documents` | ADMIN | Danh sách tài liệu |
| `DELETE` | `/api/documents/{filename}` | ADMIN | Xóa tài liệu theo tên |

**SecurityConfig cần cập nhật** — thêm rules cho GET và DELETE:
```java
.requestMatchers(HttpMethod.GET, "/api/documents").hasRole("ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/documents/**").hasRole("ADMIN")
```

Hoặc đơn giản hóa thành:
```java
.requestMatchers("/api/documents/**").hasRole("ADMIN")
```

### 3. DocumentService (mở rộng)

**File:** `spring-api/.../service/DocumentService.java`

**Methods mới:**
```java
public List<DocumentSummaryDto> listDocuments()
public void deleteDocument(String filename)
```

### 4. DocumentChunkRepository (mở rộng)

**File:** `spring-api/.../repository/DocumentChunkRepository.java`

**Query mới:**
```java
@Query(value = """
    SELECT source_file AS filename,
           year,
           doc_type   AS docType,
           COUNT(*)   AS chunkCount
    FROM document_chunks
    GROUP BY source_file, year, doc_type
    ORDER BY source_file
    """, nativeQuery = true)
List<DocumentSummaryProjection> findAllSummaries();
```

Dùng Spring Data Projection interface thay vì DTO để tránh constructor mapping phức tạp với native query.

### 5. DocumentSummaryDto (mới)

**File:** `spring-api/.../dto/DocumentSummaryDto.java`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentSummaryDto {
    private String filename;
    private Integer year;
    private String docType;
    private Integer chunkCount;
}
```

### 6. DocumentSummaryProjection (mới)

**File:** `spring-api/.../dto/DocumentSummaryProjection.java`

```java
public interface DocumentSummaryProjection {
    String getFilename();
    Integer getYear();
    String getDocType();
    Long getChunkCount();  // COUNT(*) trả về Long trong native query
}
```

### 7. Admin UI (chat-ui)

**Files:** `chat-ui/index.html`, `chat-ui/app.js`

**Thêm vào `index.html`** — bên dưới upload form trong `#admin-section`:
```html
<div id="doc-list-section">
    <h4>📋 Tài liệu đã ingest</h4>
    <div id="doc-list-container">
        <p class="loading-text">Đang tải...</p>
    </div>
</div>
```

**Thêm vào `app.js`:**
- `loadDocuments()` — gọi `GET /api/documents`, render bảng
- `deleteDocument(filename)` — gọi `DELETE /api/documents/{filename}`, refresh list
- Gọi `loadDocuments()` sau khi upload thành công và khi admin section hiển thị

### 8. Re-ingest Script

**File:** `ai-service/reingest.py`

**Interface:**
```
python reingest.py [--token TOKEN] [--base-url URL] [--pdf-dir DIR]

Options:
  --token     ADMIN JWT token (hoặc env ADMIN_TOKEN)
  --base-url  Spring API base URL (default: http://localhost:8080)
  --pdf-dir   Thư mục chứa PDF (default: ./pdf_input)
```

---

## Data Models

### DocumentSummaryDto

```
DocumentSummaryDto {
  filename:   String   // tên file gốc, ví dụ "TB số. 07.pdf"
  year:       Integer  // năm trích xuất từ tên file
  docType:    String   // "quy_che" | "thong_bao" | "huong_dan" | "general"
  chunkCount: Integer  // số chunk trong DB cho file này
}
```

### Rate Limit State (in-memory)

```
buckets: ConcurrentHashMap<String, Bucket>
  key format:
    "ip:{remoteAddr}"      — anonymous users
    "user:{username}"      — authenticated users

Bucket config (anonymous):
  Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))

Bucket config (authenticated):
  Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)))
```

### HTTP 429 Response Body

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Bạn đã gửi quá nhiều tin nhắn. Vui lòng đợi {N} giây trước khi gửi tiếp."
}
```

---

## API Contracts

### GET /api/documents

**Auth:** Bearer token với role ADMIN

**Response 200:**
```json
[
  {
    "filename": "TB số. 07. Vv Nghỉ tết nguyên đán_0001.pdf",
    "year": 2026,
    "docType": "thong_bao",
    "chunkCount": 12
  },
  {
    "filename": "QĐ số 776. Vv Ban hành quy định học bổng_0001.pdf",
    "year": 2026,
    "docType": "quy_che",
    "chunkCount": 34
  }
]
```

**Response 401:** Không có token  
**Response 403:** Token hợp lệ nhưng không phải ADMIN

### DELETE /api/documents/{filename}

**Auth:** Bearer token với role ADMIN

**Path param:** `filename` — tên file gốc (URL-encoded nếu có ký tự đặc biệt)

**Response 200:**
```json
{
  "message": "Đã xóa tài liệu: TB số. 07.pdf"
}
```

**Response 200 (idempotent — file không tồn tại):**
```json
{
  "message": "Không tìm thấy tài liệu: nonexistent.pdf (không có gì để xóa)"
}
```

**Response 401:** Không có token  
**Response 403:** Không phải ADMIN

### POST /api/documents/ingest (thay đổi auth)

Endpoint đã tồn tại. Thay đổi duy nhất: SecurityConfig enforce ADMIN-only (đã có rule cho `POST /api/documents/**`, cần verify bao gồm `/ingest`).

---

## Sequence Diagrams

### Flow: Admin xem và xóa tài liệu

```
Admin Browser          Spring API              PostgreSQL
     │                     │                       │
     │  GET /api/documents  │                       │
     │  (Bearer ADMIN token)│                       │
     │─────────────────────►│                       │
     │                     │  SELECT source_file,   │
     │                     │  year, doc_type,       │
     │                     │  COUNT(*) FROM         │
     │                     │  document_chunks       │
     │                     │  GROUP BY ...          │
     │                     │──────────────────────►│
     │                     │◄──────────────────────│
     │                     │  List<Projection>      │
     │◄─────────────────────│                       │
     │  200 [DocumentSummaryDto[]]                  │
     │                     │                       │
     │  DELETE /api/documents/{filename}            │
     │─────────────────────►│                       │
     │                     │  DELETE FROM           │
     │                     │  document_chunks       │
     │                     │  WHERE source_file=?   │
     │                     │──────────────────────►│
     │                     │◄──────────────────────│
     │◄─────────────────────│                       │
     │  200 { message: ... }│                       │
     │                     │                       │
     │  GET /api/documents  │  (refresh list)       │
     │─────────────────────►│                       │
```

### Flow: Rate Limiting (Anonymous)

```
Anonymous Client       RateLimitFilter         ChatController
     │                      │                       │
     │  POST /api/chat       │                       │
     │  (no token, IP=X)     │                       │
     │─────────────────────►│                       │
     │                      │ key="ip:X"             │
     │                      │ bucket.tryConsume(1)   │
     │                      │ → consumed=true (1/10) │
     │                      │──────────────────────►│
     │◄─────────────────────────────────────────────│
     │  200 { answer: ... }  │                       │
     │                      │                       │
     │  ... (9 more requests)│                       │
     │                      │                       │
     │  POST /api/chat (11th)│                       │
     │─────────────────────►│                       │
     │                      │ bucket.tryConsume(1)   │
     │                      │ → consumed=false       │
     │◄─────────────────────│                       │
     │  429 { message: ... } │                       │
```

### Flow: Re-ingest Script

```
reingest.py            Spring API              AI Service
     │                     │                       │
     │  Đọc pdf_input/*.pdf │                       │
     │  for each file:      │                       │
     │                     │                       │
     │  POST /api/documents/ingest                  │
     │  (multipart, Bearer) │                       │
     │─────────────────────►│                       │
     │                      │  POST /chunk          │
     │                      │──────────────────────►│
     │                      │◄──────────────────────│
     │                      │  POST /v1/embeddings  │
     │                      │──────────────────────►│
     │                      │◄──────────────────────│
     │                      │  saveAll(chunks)       │
     │◄─────────────────────│                       │
     │  200 { message, filename }                   │
     │  Print: "✅ file.pdf — 34 chunks"            │
     │                     │                       │
     │  (on error):         │                       │
     │  Print: "❌ file.pdf — <error>"              │
     │  continue next file  │                       │
     │                     │                       │
     │  Print summary:      │                       │
     │  "Thành công: N, Thất bại: M"               │
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Anonymous rate limit enforcement

*For any* IP address, after exactly 10 requests to `POST /api/chat` within a 60-second window, the 11th request SHALL receive HTTP 429 with a JSON body containing a `message` field.

**Validates: Requirements 2.1, 2.3**

### Property 2: Authenticated rate limit enforcement

*For any* authenticated user ID, after exactly 30 requests to `POST /api/chat` within a 60-second window, the 31st request SHALL receive HTTP 429 with a JSON body containing a `message` field.

**Validates: Requirements 2.2, 2.4**

### Property 3: Rate limit window reset

*For any* client key (IP or userId), after the 60-second window expires following the first request, the client SHALL be able to send requests again up to the full limit without receiving HTTP 429.

**Validates: Requirements 2.6**

### Property 4: Document list aggregation correctness

*For any* set of document chunks in the database grouped by `source_file`, the `GET /api/documents` response SHALL return one entry per unique `source_file` where `chunkCount` equals the exact number of chunks with that `source_file`.

**Validates: Requirements 3.2, 3.4**

### Property 5: Delete idempotency

*For any* filename (whether it exists in the database or not), `DELETE /api/documents/{filename}` SHALL return HTTP 200.

**Validates: Requirements 5.2, 5.3**

### Property 6: Delete removes all chunks

*For any* filename that has N chunks in the database (N ≥ 1), after `DELETE /api/documents/{filename}` completes, the database SHALL contain 0 chunks with that `source_file`.

**Validates: Requirements 5.2**

### Property 7: Re-ingest script error resilience

*For any* list of PDF files where a subset fails to ingest (network error, server error), the script SHALL attempt to process all files in the list and SHALL NOT abort after the first failure.

**Validates: Requirements 1.5**

### Property 8: Re-ingest script summary accuracy

*For any* list of N PDF files where K files fail, the script's summary output SHALL report exactly (N - K) successes and K failures.

**Validates: Requirements 1.6**

### Property 9: Admin UI renders all document fields

*For any* list of `DocumentSummaryDto` objects returned by the API, the rendered document table SHALL display `filename`, `year`, `docType`, and `chunkCount` for each entry, and SHALL render exactly one delete button per entry.

**Validates: Requirements 4.2, 4.4**

---

## Error Handling

### Spring API

| Scenario | HTTP Status | Response |
|----------|-------------|----------|
| Rate limit exceeded | 429 | `{ "status": 429, "error": "Too Many Requests", "message": "..." }` |
| No JWT token on protected endpoint | 401 | Spring Security default |
| Valid token but wrong role | 403 | Spring Security default |
| File empty on ingest | 400 | `{ "message": "File không được để trống" }` |
| AI Service unreachable | 500 | `{ "message": "AI Service không trả về chunk nào" }` |
| DELETE non-existent file | 200 | `{ "message": "Không tìm thấy tài liệu: ... (không có gì để xóa)" }` |

**GlobalExceptionHandler** đã tồn tại — cần verify nó handle `IllegalArgumentException` → 400 và generic `RuntimeException` → 500.

### Re-ingest Script

- Lỗi kết nối (ConnectionError): log `❌ {filename} — Lỗi kết nối: {error}`, tiếp tục
- HTTP 4xx/5xx: log `❌ {filename} — HTTP {status}: {message}`, tiếp tục
- File không đọc được: log `❌ {filename} — Không thể đọc file`, tiếp tục
- Thiếu token: exit với message hướng dẫn

### Admin UI

- Lỗi load danh sách: hiển thị `"❌ Không thể tải danh sách tài liệu"` trong `#doc-list-container`
- Lỗi xóa: hiển thị `"❌ Xóa thất bại: {message}"` trong `#upload-status`
- 401/403 response: `apiFetch` helper đã xử lý logout tự động

---

## Testing Strategy

### Unit Tests (Spring API)

Dùng JUnit 5 + Mockito + Spring Boot Test.

**RateLimitFilter:**
- Test anonymous limit: 10 requests pass, 11th returns 429
- Test authenticated limit: 30 requests pass, 31st returns 429
- Test key isolation: requests from different IPs không ảnh hưởng nhau
- Test response body: 429 response có `message` field

**DocumentController:**
- Test `GET /api/documents` với ADMIN token → 200
- Test `GET /api/documents` với USER token → 403
- Test `GET /api/documents` không có token → 401
- Test `DELETE /api/documents/{filename}` với ADMIN → 200
- Test `DELETE /api/documents/{filename}` với USER → 403
- Test `POST /api/documents/ingest` với USER → 403

**DocumentService:**
- Test `listDocuments()` với mock repository trả về N entries
- Test `deleteDocument()` gọi `deleteBySourceFile()` đúng filename
- Test `deleteDocument()` với filename không tồn tại không throw exception

**DocumentChunkRepository:**
- Test `findAllSummaries()` với integration test (H2 hoặc Testcontainers PostgreSQL)
- Verify GROUP BY trả về đúng chunkCount

### Property-Based Tests

Dùng **jqwik** (Java property-based testing library, tích hợp tốt với JUnit 5).

**Dependency:**
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.0</version>
    <scope>test</scope>
</dependency>
```

**Property 1 & 2 — Rate limit enforcement:**
```
// Feature: fbu-chat-improvements, Property 1: Anonymous rate limit enforcement
@Property(tries = 50)
void anonymousRateLimitEnforced(@ForAll @StringLength(min=7, max=15) String ip) {
    // Send 10 requests → all pass
    // Send 11th → 429
}
```

**Property 4 — Document list aggregation:**
```
// Feature: fbu-chat-improvements, Property 4: Document list aggregation correctness
@Property(tries = 100)
void documentListChunkCountMatchesDB(@ForAll List<@From("chunkGroups") ChunkGroup> groups) {
    // Insert groups into DB
    // Call listDocuments()
    // Verify each entry's chunkCount matches group size
}
```

**Property 5 & 6 — Delete idempotency and completeness:**
```
// Feature: fbu-chat-improvements, Property 5: Delete idempotency
@Property(tries = 100)
void deleteAlwaysReturns200(@ForAll String filename) { ... }

// Feature: fbu-chat-improvements, Property 6: Delete removes all chunks
@Property(tries = 100)
void deleteRemovesAllChunks(@ForAll @Positive int chunkCount, @ForAll String filename) { ... }
```

**Property 7 & 8 — Re-ingest script (Python, dùng Hypothesis):**
```python
# Feature: fbu-chat-improvements, Property 7: Re-ingest script error resilience
@given(st.lists(st.text(), min_size=1), st.sets(st.integers()))
def test_all_files_attempted(filenames, failing_indices):
    # Mock HTTP server where failing_indices fail
    # Run script
    # Verify len(attempted) == len(filenames)

# Feature: fbu-chat-improvements, Property 8: Re-ingest script summary accuracy
@given(st.lists(st.text(), min_size=1), st.sets(st.integers()))
def test_summary_accuracy(filenames, failing_indices):
    # Verify summary shows correct counts
```

**Property 9 — Admin UI rendering (JavaScript, dùng fast-check):**
```javascript
// Feature: fbu-chat-improvements, Property 9: Admin UI renders all document fields
fc.assert(fc.property(
    fc.array(documentSummaryArb, { minLength: 1 }),
    (docs) => {
        renderDocumentTable(docs);
        // Verify each doc's fields appear in DOM
        // Verify N delete buttons
    }
), { numRuns: 100 });
```

### Integration Tests

- End-to-end: upload PDF → verify chunks in DB → list documents → delete → verify 0 chunks
- Rate limit integration: test với real HTTP requests (Spring MockMvc)
- Re-ingest script: test với mock HTTP server (responses.activate hoặc httpretty)

### Smoke Tests

- Verify Bucket4j dependency present
- Verify SecurityConfig covers all `/api/documents/**` endpoints
- Verify `@Transactional` on `deleteBySourceFile`

---

## Implementation Notes

### 1. Thứ tự thực hiện đề xuất

1. Cập nhật `pom.xml` — thêm Bucket4j
2. Nâng cấp `RateLimitFilter` — thay custom logic bằng Bucket4j
3. Cập nhật `SecurityConfig` — thêm rules cho GET và DELETE `/api/documents`
4. Thêm `DocumentSummaryProjection` interface
5. Thêm `DocumentSummaryDto` class
6. Thêm query `findAllSummaries()` vào `DocumentChunkRepository`
7. Thêm `listDocuments()` và `deleteDocument()` vào `DocumentService`
8. Thêm endpoints vào `DocumentController`
9. Cập nhật `index.html` — thêm doc list section
10. Cập nhật `app.js` — thêm `loadDocuments()` và `deleteDocument()`
11. Tạo `ai-service/reingest.py`

### 2. Bucket4j version compatibility

Spring Boot 3.5 dùng Spring Framework 6.x. Bucket4j 8.x tương thích tốt. Không cần `bucket4j-spring-boot-starter` — chỉ cần `bucket4j-core` là đủ cho in-memory usage.

### 3. RateLimitFilter — JWT parsing thủ công

Vì RateLimitFilter chạy trước JwtFilter, `SecurityContextHolder` chưa có authentication. Cần inject `JwtUtil` vào `RateLimitFilter` và gọi `jwtUtil.extractUsername(token)` trực tiếp. Wrap trong try-catch để handle token invalid gracefully (fallback về IP-based key).

### 4. DELETE endpoint — URL encoding

Tên file PDF tiếng Việt có ký tự đặc biệt (dấu, khoảng trắng). Frontend cần dùng `encodeURIComponent(filename)` khi build URL. Backend nhận `@PathVariable String filename` — Spring tự decode URL.

Tuy nhiên, `/` trong filename sẽ gây vấn đề với path variable. Kiểm tra tên file trong `pdf_input/` — không có file nào chứa `/` nên không cần xử lý đặc biệt.

### 5. findAllSummaries — Projection vs DTO

Native query với `COUNT(*)` trả về `Long` (không phải `Integer`). Dùng Projection interface (`DocumentSummaryProjection`) để Spring Data tự map. Sau đó convert sang `DocumentSummaryDto` trong service layer để tránh expose projection ra ngoài.

### 6. SecurityConfig — rule order

Spring Security xử lý rules theo thứ tự. Rule hiện tại:
```java
.requestMatchers(HttpMethod.POST, "/api/documents/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

Cần thêm trước `anyRequest()`:
```java
.requestMatchers("/api/documents/**").hasRole("ADMIN")
```

Rule này cover tất cả methods (GET, POST, DELETE) cho `/api/documents/**`.

### 7. Re-ingest script — multipart upload

Python `requests` library gửi multipart với:
```python
with open(pdf_path, 'rb') as f:
    files = {'file': (filename, f, 'application/pdf')}
    response = requests.post(url, files=files, headers=headers, timeout=120)
```

Timeout 120 giây vì ingest PDF lớn có thể mất thời gian (chunking + embedding).

### 8. Admin UI — loadDocuments timing

Gọi `loadDocuments()` tại 3 thời điểm:
1. Khi `updateAuthUI()` phát hiện `user.role === 'ADMIN'`
2. Sau khi upload thành công (trong upload form submit handler)
3. Sau khi xóa thành công (trong `deleteDocument()`)
