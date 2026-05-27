# Requirements Document

## Introduction

Feature này implement **Parent-Child Chunking pipeline** cho hệ thống FBU Chat RAG.

Pipeline hiện tại dùng `RecursiveCharacterTextSplitter` với chunk 1200 ký tự trực tiếp từ PDF OCR, dẫn đến vector bị "pha loãng" (cosine distance cao) và LLM thiếu context khi trả lời. Giải pháp thay thế tận dụng 23 file `_clean.md` đã được DeepSeek cấu trúc hóa sẵn tại `postgres/pdf_output/md_rag/`, phân cấp theo `##` (parent section) và `###`/`####` (child chunk).

Pipeline mới:
1. `_clean.md` → `MarkdownProcessor` parse YAML front matter + split heading → parent chunks + child chunks
2. Embed **chỉ child chunks** (nhỏ, tập trung → vector chất lượng cao)
3. Lưu `parent_chunks` + `document_chunks` (child, FK `parent_id`)
4. Query → embed → search child → fetch parent → gửi **parent content** vào LLM

---

## Glossary

- **MarkdownProcessor**: Python processor mới trong `ai-service/processors/`, kế thừa `BaseProcessor`, xử lý file `.md` theo cấu trúc `_clean.md`.
- **Parent Chunk**: Một section `##` trong `_clean.md`, chứa toàn bộ nội dung của section đó (bao gồm tất cả `###`/`####` con). Lưu trong bảng `parent_chunks`.
- **Child Chunk**: Một đoạn `###` hoặc `####` trong `_clean.md`, là đơn vị được embed và tìm kiếm. Lưu trong bảng `document_chunks` với FK `parent_id`.
- **YAML Front Matter**: Phần metadata ở đầu file `_clean.md` được bao bởi `---`, chứa các trường `source`, `year`, `type`, `title`, `issued_by`.
- **Context Hint**: Dòng blockquote `>` ngay sau heading `##`, cung cấp tóm tắt ngữ cảnh cho section.
- **DocumentService**: Spring Boot service xử lý ingest tài liệu, gọi AI Service để lấy chunks và embeddings, lưu vào DB.
- **RagService**: Spring Boot service xử lý chat RAG, thực hiện hybrid search và gọi Groq LLM.
- **AI_Service**: FastAPI service tại port 8001, cung cấp `/chunk` và `/v1/embeddings`.
- **Flyway**: Công cụ migration DB được Spring Boot quản lý, hiện tại đang ở V4.
- **ChunkResult**: DTO trong Spring Boot chứa kết quả tìm kiếm từ `DocumentChunkRepository`.
- **Hybrid_Search**: Kết hợp vector similarity search và PostgreSQL full-text search dùng RRF (Reciprocal Rank Fusion).
- **reingest_script**: Script `reingest.py` trong `ai-service/`, dùng để nạp lại hàng loạt tài liệu qua Spring API.

---

## Requirements

### Requirement 1: MarkdownProcessor — Parse file `_clean.md`

**User Story:** As a system administrator, I want to ingest structured `_clean.md` files into the RAG pipeline, so that the knowledge base contains high-quality, hierarchically organized chunks from FBU documents.

#### Acceptance Criteria

1. WHEN a `.md` file is submitted to `POST /chunk`, THE `AI_Service` SHALL route the file to `MarkdownProcessor` instead of any other processor.
2. WHEN `MarkdownProcessor` processes a `_clean.md` file, THE `MarkdownProcessor` SHALL parse the YAML front matter block delimited by `---` and extract the fields `source`, `year`, `type`, `title`, and `issued_by`.
3. IF the YAML front matter is absent or malformed, THEN THE `MarkdownProcessor` SHALL derive `source` from the filename, set `year` to `2026`, set `type` to `general`, and set `title` to the filename stem.
4. WHEN `MarkdownProcessor` splits a document, THE `MarkdownProcessor` SHALL treat each `##`-level heading as a parent section boundary, producing one parent chunk per `##` section.
5. WHEN `MarkdownProcessor` splits a parent section, THE `MarkdownProcessor` SHALL treat each `###`-level and `####`-level heading as a child chunk boundary, producing one child chunk per sub-heading.
6. IF a parent section contains no `###` or `####` sub-headings, THEN THE `MarkdownProcessor` SHALL produce one child chunk containing the full text of that parent section.
7. WHEN `MarkdownProcessor` produces a child chunk, THE `MarkdownProcessor` SHALL prepend the context string `"[Tài liệu: {title}] [{parent_heading}]\n"` to the child chunk content, where `{title}` is from the front matter and `{parent_heading}` is the text of the parent `##` heading.
8. WHEN `MarkdownProcessor` produces a child chunk, THE `MarkdownProcessor` SHALL include the metadata fields `parent_heading`, `source_file`, `year`, `doc_type`, and `title` in the chunk response alongside `content` and `chunkIndex`.
9. THE `MarkdownProcessor` SHALL filter out child chunks whose content (excluding the prepended context string) is shorter than 30 characters.
10. FOR ALL valid `_clean.md` files, THE `MarkdownProcessor` SHALL produce at least one child chunk per document.

---

### Requirement 2: DB Schema — Bảng `parent_chunks` và FK `parent_id`

**User Story:** As a developer, I want a dedicated `parent_chunks` table with a foreign key from `document_chunks`, so that the system can store and retrieve full section context for LLM responses.

#### Acceptance Criteria

1. THE `Flyway` SHALL apply migration `V5__parent_child_chunking.sql` that creates the `parent_chunks` table with columns: `id UUID PRIMARY KEY`, `source_file VARCHAR(255)`, `heading TEXT`, `content TEXT NOT NULL`, `year INTEGER`, `doc_type VARCHAR(100)`, `title VARCHAR(500)`, `created_at TIMESTAMPTZ DEFAULT NOW()`.
2. THE `Flyway` SHALL apply migration `V5__parent_child_chunking.sql` that adds column `parent_id UUID REFERENCES parent_chunks(id) ON DELETE SET NULL` to the `document_chunks` table.
3. WHEN a `document_chunks` row references a `parent_chunks` row via `parent_id`, THE database SHALL enforce referential integrity such that deleting the parent sets `parent_id` to NULL on all referencing child rows.
4. THE `Flyway` SHALL create an index on `document_chunks(parent_id)` to support efficient parent lookup queries.
5. THE `Flyway` SHALL create an index on `parent_chunks(source_file)` to support efficient deletion by source file.
6. WHEN `V5__parent_child_chunking.sql` is applied to a database that already contains rows in `document_chunks`, THE migration SHALL complete without error, leaving existing rows with `parent_id = NULL`.

---

### Requirement 3: DocumentChunk Entity và ParentChunk Entity

**User Story:** As a developer, I want updated JPA entities that reflect the new schema, so that Spring Boot can persist and query parent-child relationships correctly.

#### Acceptance Criteria

1. THE `DocumentChunk` entity SHALL include a field `parentId` of type `UUID` mapped to column `parent_id`.
2. THE system SHALL provide a new `ParentChunk` JPA entity mapped to the `parent_chunks` table, with fields: `id UUID`, `sourceFile String`, `heading String`, `content String`, `year Integer`, `docType String`, `title String`.
3. THE system SHALL provide a `ParentChunkRepository` extending `JpaRepository<ParentChunk, UUID>` with a method `deleteBySourceFile(String sourceFile)` annotated `@Modifying`.
4. THE `ParentChunkRepository` SHALL provide a method `findBySourceFile(String sourceFile)` returning `List<ParentChunk>`.
5. WHEN `DocumentChunkRepository.hybridSearch` is called, THE query SHALL return `parent_id` as an additional column in the result set so that `ChunkResult` can carry `parentId`.
6. THE `ChunkResult` DTO SHALL include a field `parentId` of type `UUID` (nullable) to carry the parent reference from search results.

---

### Requirement 4: Ingest Endpoint — Xử lý file `.md`

**User Story:** As a system administrator, I want to ingest `.md` files via the existing `POST /api/documents/ingest` endpoint, so that I can load structured `_clean.md` documents into the knowledge base without a separate endpoint.

#### Acceptance Criteria

1. WHEN a `.md` file is submitted to `POST /api/documents/ingest`, THE `DocumentService` SHALL forward the file to `AI_Service` `POST /chunk` and receive a list of chunk candidates that include `parentHeading`, `title`, `year`, `docType`, and `content` fields.
2. WHEN `DocumentService` receives chunk candidates from a `.md` file, THE `DocumentService` SHALL group candidates by `parentHeading` and persist one `ParentChunk` row per unique `parentHeading` before persisting child `DocumentChunk` rows.
3. WHEN `DocumentService` persists child `DocumentChunk` rows for a `.md` file, THE `DocumentService` SHALL set `parentId` on each child to the `id` of the corresponding `ParentChunk` row created in the same transaction.
4. WHEN `DocumentService` ingests a `.md` file that was previously ingested, THE `DocumentService` SHALL delete existing `parent_chunks` rows with matching `source_file` before inserting new ones, and the cascade `ON DELETE SET NULL` SHALL clear `parent_id` on orphaned child rows before they are also deleted.
5. WHEN `DocumentService` ingests a `.pdf` or other non-`.md` file, THE `DocumentService` SHALL follow the existing ingest flow without creating any `parent_chunks` rows, and `parent_id` SHALL remain NULL for all resulting child chunks.
6. WHEN `DocumentService` embeds child chunks from a `.md` file, THE `DocumentService` SHALL send only the child chunk content (with prepended context string) to `POST /v1/embeddings`, not the full parent content.
7. IF `AI_Service` returns zero chunks for a `.md` file, THEN THE `DocumentService` SHALL throw an exception with message `"AI Service không trả về chunk nào"` and roll back the transaction.

---

### Requirement 5: RagService — Fetch Parent Content khi Search

**User Story:** As a student or staff member, I want the chatbot to answer questions using full section context, so that responses are accurate and complete rather than based on isolated small chunks.

#### Acceptance Criteria

1. WHEN `RagService.hybridSearch` returns child chunks that have a non-null `parentId`, THE `RagService` SHALL fetch the corresponding `ParentChunk` rows by their `id` values using a single batch query.
2. WHEN `RagService` builds the LLM context string, THE `RagService` SHALL use the `content` field of the fetched `ParentChunk` (full section text) instead of the child chunk `content` for chunks that have a parent.
3. WHEN a child chunk has `parentId = NULL` (document ingested before this feature), THE `RagService` SHALL fall back to using the child chunk `content` directly in the LLM context string.
4. WHEN multiple child chunks share the same `parentId`, THE `RagService` SHALL deduplicate and include the parent content only once in the LLM context string.
5. WHEN `RagService` builds the LLM context string, THE `RagService` SHALL format each context entry as `"[Nguồn: {sourceFile} | Năm: {year}]\n{parentContent}"` where `parentContent` is the full parent section text.
6. WHEN `RagService` builds the source list for `ChatResponse`, THE `RagService` SHALL continue to deduplicate sources by `sourceFile` as in the existing implementation.

---

### Requirement 6: Reingest Script — Hỗ trợ file `.md` từ `md_rag` directory

**User Story:** As a system administrator, I want to bulk-reingest all `_clean.md` files from the `md_rag` directory, so that I can populate the knowledge base with parent-child structured data in a single command.

#### Acceptance Criteria

1. WHEN `reingest.py` is invoked with `--ext .md`, THE `reingest_script` SHALL discover all files with extension `.md` in the specified `--pdf-dir` directory and submit each to `POST /api/documents/ingest`.
2. THE `reingest_script` SHALL already support `--ext .md` via the existing `--ext` argument without code changes, as the current implementation filters by `args.ext.lower()`.
3. WHEN `docker-compose.yml` is updated, THE `fbuai` service SHALL mount `./postgres/pdf_output/md_rag` as a read-only volume at `/app/md_rag` inside the container so that `reingest.py` can access `_clean.md` files from within the container.
4. WHEN `reingest.py` is run from the host machine pointing to `postgres/pdf_output/md_rag`, THE `reingest_script` SHALL accept any valid directory path via `--pdf-dir` without requiring the files to be inside the container.
5. WHEN `reingest.py` processes a `.md` file and the Spring API returns HTTP 200, THE `reingest_script` SHALL log the filename and the `chunkCount` from the response.
6. IF `reingest.py` encounters an HTTP error for a `.md` file, THEN THE `reingest_script` SHALL log the error and continue processing remaining files without aborting.

---

### Requirement 7: Xóa Document — Cascade Delete `parent_chunks`

**User Story:** As a system administrator, I want deleting a document to also remove its parent chunks, so that the database does not accumulate orphaned parent records.

#### Acceptance Criteria

1. WHEN `DELETE /api/documents/{filename}` is called, THE `DocumentService` SHALL delete all `parent_chunks` rows with `source_file = {filename}` before or in the same transaction as deleting `document_chunks` rows.
2. WHEN `DocumentService.deleteDocument` deletes `parent_chunks` rows for a given `source_file`, THE `ParentChunkRepository.deleteBySourceFile` method SHALL be called with the same `source_file` value used for `DocumentChunkRepository.deleteBySourceFile`.
3. WHEN `DocumentService.deleteDocument` is called for a filename that has no `parent_chunks` rows, THE `DocumentService` SHALL complete without error (idempotent behavior).
4. WHEN `DocumentService.deleteDocument` is called for a filename that has no `document_chunks` rows, THE `DocumentService` SHALL complete without error (idempotent behavior).
5. WHEN `DocumentService.deleteDocument` deletes both `parent_chunks` and `document_chunks` rows, THE operation SHALL be wrapped in a single `@Transactional` boundary so that partial deletion does not occur on failure.
