# 🎓 FBU Chat — Trợ lý AI nội bộ Trường Đại học Tài chính - Ngân hàng Hà Nội

> Hệ thống chatbot RAG (Retrieval-Augmented Generation) hỗ trợ sinh viên và giảng viên tra cứu quy chế, thông báo, và tài liệu nội bộ của FBU.

**Live:** [chat.bustomzxy.org](https://chat.bustomzxy.org) · **API:** [fbu.bustomzxy.org](https://fbu.bustomzxy.org)

---

## Kiến trúc hệ thống

```
Internet
    │
    ▼
Cloudflare Tunnel (fbu.bustomzxy.org)
    │
    ▼
Nginx :80  ──────────────────────────────────────────────
    │               │                    │
    ▼               ▼                    ▼
React SPA      /api/ → Spring Boot   /minio/ → MinIO
(Vercel)          :8080                 :9000
                   │
              ┌────┴────┐
              ▼         ▼
         PostgreSQL   FastAPI
         :5432 (host)  :8001
         + pgvector   (e5-small-v2)
                       │
                       ▼
                  Groq API (Llama-3.3-70b)
```

---

## Tech Stack

| Layer | Công nghệ |
|---|---|
| **Backend** | Java 21 · Spring Boot 3.5 · Spring Security · JPA/Hibernate · Flyway |
| **AI Service** | Python · FastAPI · sentence-transformers (e5-small-v2) · OCRmyPDF · Tesseract |
| **LLM** | Groq API (llama-3.3-70b-versatile) |
| **Database** | PostgreSQL 16 · pgvector (HNSW) · Full-text search (unaccent) |
| **Storage** | MinIO (S3-compatible) |
| **Frontend** | React 19 · Vite · React Router · Vercel |
| **Infra** | Docker Compose · Nginx · Cloudflare Tunnel · Bucket4j |
| **Vietnamese NLP** | CocCoc Tokenizer (JNI) |

---

## Tính năng chính

### RAG Pipeline
- **3-pass Hybrid Search**: AND tsquery → OR tsquery → pure vector, với RRF (Reciprocal Rank Fusion) reranking
- **Parent-Child Chunking**: embed child chunk nhỏ để tăng precision, fetch parent section đầy đủ cho LLM context
- **Intent Classification**: phân loại FBU_INFO vs GENERAL_CHAT + slot-filling docType trong 1 Groq call
- **Scope filtering**: tự động thu hẹp tìm kiếm theo viện/khoa từ query

### Document Processing
- PDF (có OCR fallback cho file scan), DOCX, Markdown (YAML front matter), JSON, Image
- Structured Markdown với heading hierarchy (`##`/`###`/`####`) cho Parent-Child chunking

### Image Search
- Vector similarity search trên `document_images.tag_embedding`
- Intent detection riêng (isImageRequest / isImageOnlyRequest)
- Lưu ảnh vào MinIO, persist URL trong conversation history

### Authentication & Security
- JWT stateless với access token (7 ngày) + refresh token trong HttpOnly Cookie (30 ngày)
- RBAC: USER và ADMIN roles
- Per-user conversation ownership enforcement
- Rate limiting: 10 req/phút (anonymous) / 30 req/phút (authenticated) / 5 register/giờ/IP

### Conversation
- Lưu history với sources và images, persist qua reload
- URL-based conversation routing (`/chat/:id`)
- Anonymous: lịch sử qua client-side, không persist

---

## Cấu trúc thư mục

```
fbu_chat/
├── ai-service/              # FastAPI + embedding + document processors
│   ├── processors/          # PDF, DOCX, Markdown, Image, JSON processors
│   ├── main.py              # FastAPI app
│   └── reingest.py          # CLI script để bulk re-ingest tài liệu
│
├── chat-ui/                 # React 19 + Vite frontend
│   └── src/
│       ├── components/      # UI components (chat, layout, admin, auth)
│       ├── hooks/           # Custom hooks (useChat, useAuth, useIsMobile...)
│       ├── pages/           # ChatPage, AdminPage, FeedbackPage
│       └── services/        # API service layer
│
├── spring-api/fbu_chat/     # Spring Boot 3.5 backend
│   └── src/main/java/com/tomzxy/fbu_chat/
│       ├── config/          # WebClientConfig, JacksonConfig
│       ├── controller/      # ChatController, DocumentController, AuthController
│       ├── dto/             # Request/Response DTOs
│       ├── entity/          # JPA entities
│       ├── repository/      # Spring Data repositories
│       ├── security/        # JWT, RateLimitFilter, SecurityConfig
│       ├── service/         # RagService, DocumentService, StorageService
│       └── util/            # TsQueryBuilder, VietnameseTokenizerService
│
├── postgres/
│   ├── init.sql             # pgvector extension + document_chunks schema
│   └── pdf_output/md_rag/  # Structured Markdown files (DeepSeek-processed)
│
└── docker-compose.yml       # Orchestration: fbuai, api, chatui, minio
```

---

## Setup & Deployment

### Yêu cầu
- Docker & Docker Compose
- PostgreSQL 16 với pgvector extension (chạy trên host)
- Java 21 JDK (trong container)
- CocCoc Tokenizer native library (mount từ host)

### Biến môi trường (`.env`)

```env
# PostgreSQL (host)
POSTGRES_DB=ragdb
POSTGRES_USER=raguser
POSTGRES_PASSWORD=<password>
POSTGRES_PORT=5432

# MinIO
MINIO_ROOT_USER=<user>
MINIO_ROOT_PASSWORD=<password>
MINIO_ENDPOINT=http://minio:9000
MINIO_EXTERNAL_URL=https://your-domain.com/minio

# CORS
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,https://your-domain.com

# Groq API
GROQ_API_KEY=<groq-api-key>
```

### Chạy

```bash
# Khởi động toàn bộ stack
docker compose up -d

# Kiểm tra trạng thái
docker ps
docker logs spring_api --tail 20
```

### Ingest tài liệu

```bash
# Lấy ADMIN token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your_password"}'

# Re-ingest tất cả file Markdown
cd ai-service
python3 reingest.py \
  --token <ADMIN_TOKEN> \
  --base-url http://localhost:8080 \
  --pdf-dir ../postgres/pdf_output/md_rag \
  --ext .md \
  --delay 2
```

### Deploy Frontend (Vercel)

```bash
cd chat-ui
# Set environment variable trên Vercel dashboard:
# VITE_API_BASE_URL = https://your-tunnel-domain.com

vercel --prod
```

---

## Database Schema

```sql
-- Vector chunks (child)
document_chunks (id, content, embedding VECTOR(384), source_file,
                 parent_id, doc_type, year, ts_content tsvector, ...)

-- Parent sections
parent_chunks (id, source_file, heading, content, year, doc_type, title)

-- Conversation storage
conversations (id, user_id, title, created_at, updated_at)
messages (id, conversation_id, role, content, sources JSONB, images JSONB)

-- Image search
document_images (id, minio_url, caption, tags, tag_embedding VECTOR(384), category)

-- Users
users (id, username, password, role)
```

---

## API Endpoints

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Đăng nhập, trả JWT |
| `POST` | `/api/auth/register` | Public (5/giờ/IP) | Đăng ký tài khoản |
| `POST` | `/api/chat` | Public | Chat với RAG |
| `GET` | `/api/chat/conversations` | User | Lịch sử conversations |
| `GET` | `/api/chat/conversations/:id/messages` | Owner | Messages của conversation |
| `GET` | `/api/documents` | Admin | Danh sách tài liệu đã ingest |
| `POST` | `/api/documents/ingest` | Admin | Upload & ingest tài liệu |
| `DELETE` | `/api/documents/:filename` | Admin | Xóa tài liệu |

---

## Tác giả

**Đạt · tomzxy** — Công ty 1 mình tao 🐧

- 📧 trinhdat24102003@gmail.com
- 🔗 [facebook.com/trinh.at.293350](https://web.facebook.com/trinh.at.293350)

> *"Don't die"* — Châm ngôn sống trong dự án này

---

## License

MIT — Dùng thoải mái, nhớ credit nhoa =))
