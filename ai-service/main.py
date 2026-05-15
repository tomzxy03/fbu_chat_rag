import os
import uuid
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, Body, HTTPException, Query
from fastapi.responses import StreamingResponse
from sentence_transformers import SentenceTransformer
from psycopg2 import pool
import pdfplumber
import json
from groq import Groq

from process import process_pdf

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ─── DB Connection Pool ──────────────────────────────────────────────────────
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "host.docker.internal"),
    "dbname": os.getenv("DB_NAME", "ragdb"),
    "user": os.getenv("DB_USER", "raguser"),
    "password": os.getenv("DB_PASSWORD", "ragpass"),
    "port": int(os.getenv("DB_PORT", 5432)),
}

db_pool: pool.ThreadedConnectionPool = None
model: SentenceTransformer = None
groq_client: Groq = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global db_pool, model, groq_client
    logger.info("Starting up: loading model and DB pool...")
    model = SentenceTransformer("intfloat/e5-small-v2")
    db_pool = pool.ThreadedConnectionPool(minconn=2, maxconn=10, **DB_CONFIG)
    groq_client = Groq(api_key=os.getenv("GROQ_API_KEY", ""))
    logger.info("Ready ✅")
    yield
    db_pool.closeall()
    logger.info("Shutdown complete.")


app = FastAPI(title="FBU Chat AI Service", lifespan=lifespan)


def get_conn():
    return db_pool.getconn()


def put_conn(conn):
    db_pool.putconn(conn)


# ─── Endpoints ───────────────────────────────────────────────────────────────

@app.get("/health")
def health_check():
    return {"status": "healthy", "model": "e5-small-v2"}


@app.post("/ingest")
async def ingest_pdf(
    file: UploadFile = File(...),
    year: int = Query(default=2026),
    doc_type: str = Query(default="general"),
):
    """Upload PDF và chia nhỏ thành chunks, embed rồi lưu vào DB."""
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Chỉ hỗ trợ file PDF")

    # Đọc PDF tạm bằng pdfplumber (stream-safe)
    import tempfile, shutil
    with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    chunks = process_pdf(tmp_path)
    os.unlink(tmp_path)

    if not chunks:
        raise HTTPException(status_code=422, detail="Không trích xuất được nội dung PDF")

    conn = get_conn()
    try:
        cur = conn.cursor()
        for idx, text in enumerate(chunks):
            embedding = model.encode(f"passage: {text}").tolist()
            cur.execute(
                """
                INSERT INTO document_chunks
                    (id, content, embedding, source_file, chunk_index, doc_type, year)
                VALUES (%s, %s, %s::vector, %s, %s, %s, %s)
                """,
                (str(uuid.uuid4()), text, str(embedding), file.filename, idx, doc_type, year),
            )
        conn.commit()
        cur.close()
    finally:
        put_conn(conn)

    logger.info(f"Ingested {len(chunks)} chunks from {file.filename}")
    return {"message": f"Nạp thành công {len(chunks)} đoạn từ {file.filename}"}


@app.get("/search")
async def search(
    query: str,
    top_k: int = Query(default=5, ge=1, le=20),
    year: int = Query(default=None),
    doc_type: str = Query(default=None),
):
    """Tìm kiếm vector similarity với filter tùy chọn theo year / doc_type."""
    query_embedding = model.encode(f"query: {query}").tolist()

    filters = []
    params = [str(query_embedding)]
    if year:
        filters.append("year = %s")
        params.append(year)
    if doc_type:
        filters.append("doc_type = %s")
        params.append(doc_type)

    where_clause = ("WHERE " + " AND ".join(filters)) if filters else ""
    params.append(top_k)

    conn = get_conn()
    try:
        from psycopg2.extras import RealDictCursor
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute(
            f"""
            SELECT content, source_file, chunk_index, doc_type, year,
                   (embedding <=> %s::vector) AS distance
            FROM document_chunks
            {where_clause}
            ORDER BY distance ASC
            LIMIT %s
            """,
            params,
        )
        results = cur.fetchall()
        cur.close()
    finally:
        put_conn(conn)

    return {"query": query, "results": [dict(r) for r in results]}


@app.post("/chat")
async def chat(payload: dict = Body(...)):
    """
    RAG Chat: nhận query → tìm context → Groq LLM → trả lời.
    Body: { "query": str, "year": int?, "doc_type": str?, "top_k": int? }
    """
    query = payload.get("query", "").strip()
    if not query:
        raise HTTPException(status_code=400, detail="query không được để trống")

    top_k = payload.get("top_k", 5)
    year = payload.get("year")
    doc_type = payload.get("doc_type")

    # 1. Tìm context liên quan
    search_result = await search(query=query, top_k=top_k, year=year, doc_type=doc_type)
    contexts = search_result.get("results", [])

    if not contexts:
        context_text = "Không tìm thấy tài liệu liên quan."
    else:
        context_text = "\n\n---\n\n".join(
            [f"[Nguồn: {c['source_file']} | Năm: {c['year']}]\n{c['content']}" for c in contexts]
        )

    # 2. Tạo prompt RAG
    system_prompt = """Bạn là trợ lý AI của trường FBU (Đại học FPT Bình Dương).
Nhiệm vụ của bạn là trả lời câu hỏi của sinh viên và giảng viên dựa trên tài liệu nội bộ được cung cấp.
Quy tắc:
- Chỉ trả lời dựa trên CONTEXT được cung cấp.
- Nếu không có thông tin liên quan trong context, hãy nói rõ là không tìm thấy thông tin.
- Trả lời bằng tiếng Việt, chính xác và đầy đủ.
- Trích dẫn nguồn tài liệu khi trả lời."""

    user_prompt = f"""CONTEXT từ tài liệu:
{context_text}

CÂU HỎI: {query}

Trả lời:"""

    # 3. Gọi Groq LLM
    if not groq_client.api_key:
        raise HTTPException(status_code=503, detail="GROQ_API_KEY chưa được cấu hình")

    try:
        completion = groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.3,
            max_tokens=1024,
        )
        answer = completion.choices[0].message.content
    except Exception as e:
        logger.error(f"Groq error: {e}")
        raise HTTPException(status_code=502, detail=f"LLM error: {str(e)}")

    return {
        "query": query,
        "answer": answer,
        "sources": [
            {"file": c["source_file"], "year": c["year"], "doc_type": c["doc_type"]}
            for c in contexts
        ],
    }


@app.post("/v1/embeddings")
async def get_embeddings(payload: dict = Body(...)):
    """OpenAI-compatible embeddings endpoint."""
    inputs = payload.get("input", [])
    if isinstance(inputs, str):
        inputs = [inputs]

    embeddings = []
    for i, text in enumerate(inputs):
        prefix = "passage: " if payload.get("mode") == "passage" else "query: "
        vector = model.encode(f"{prefix}{text}").tolist()
        embeddings.append({"object": "embedding", "embedding": vector, "index": i})

    return {"object": "list", "data": embeddings, "model": "e5-small-v2"}
