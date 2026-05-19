import os
import uuid
import logging
import tempfile
import shutil
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, Body, HTTPException
from sentence_transformers import SentenceTransformer

from processors import get_processor, get_supported_extensions

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

model: SentenceTransformer = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    logger.info("Starting up: loading model...")
    model = SentenceTransformer("intfloat/e5-small-v2")
    logger.info("Ready ✅")
    yield
    logger.info("Shutdown complete.")

app = FastAPI(title="FBU Chat AI Service", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chunk")
async def chunk_file(file: UploadFile = File(...)):
    filename = file.filename or "unknown"
    processor = get_processor(filename)

    if processor is None:
        supported = ", ".join(get_supported_extensions())
        raise HTTPException(
            status_code=400,
            detail=f"Định dạng file không được hỗ trợ. Các định dạng hỗ trợ: {supported}",
        )

    ext = os.path.splitext(filename)[1].lower()
    with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    try:
        chunks = processor.process(tmp_path)
    except Exception as e:
        logger.error(f"Processor error for {filename}: {e}")
        raise HTTPException(status_code=422, detail=f"Lỗi xử lý file: {str(e)}")
    finally:
        os.unlink(tmp_path)

    if not chunks:
        raise HTTPException(status_code=422, detail="Không trích xuất được nội dung từ file")

    results = []
    # Simplified mapping to match ChunkCandidate DTO
    for idx, text in enumerate(chunks):
        results.append({
            "content": text,
            "pageNumber": 1,
            "chunkIndex": idx
        })
        
    return results


@app.post("/v1/embeddings")
async def get_embeddings(payload: dict = Body(...)):
    # Support "texts" key from Spring Boot custom request
    inputs = payload.get("input", payload.get("texts", []))
    if isinstance(inputs, str):
        inputs = [inputs]

    embeddings = []
    for i, text in enumerate(inputs):
        prefix = "passage: " if payload.get("mode") == "passage" else "query: "
        vector = model.encode(f"{prefix}{text}").tolist()
        embeddings.append(vector)

    return {"embeddings": embeddings}
