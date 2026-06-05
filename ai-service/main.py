import asyncio
import os
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

# Semaphore: chỉ cho phép 1 chunk request tại một thời điểm
# Máy yếu (i5-5200U, 8GB RAM) — tránh OOM khi nhiều PDF lớn xử lý đồng thời
chunk_semaphore = asyncio.Semaphore(1)


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

    # Đọc file vào memory trước khi acquire semaphore
    content = await file.read()

    async with chunk_semaphore:
        # Ghi ra temp file và xử lý
        with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as tmp:
            tmp.write(content)
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
    
    logger.info(f"🔍 SAMPLE CHUNK TỪ PROCESSOR: {chunks[0]}")

    # MarkdownProcessor trả về list[dict], các processor khác trả về list[str]
    if chunks and isinstance(chunks[0], dict):
        # Luôn override sourceFile bằng tên file gốc từ upload request
        # (processor nhận temp path nên basename sẽ là tên temp)
        for chunk in chunks:
            chunk["sourceFile"] = filename
        return chunks
    # else:
    #     return [
    #         {"content": text, "pageNumber": 1, "chunkIndex": idx}
    #         for idx, text in enumerate(chunks)
    #     ]
    else:
        results = []
        for idx, text in enumerate(chunks):
            # Tự động dò tìm Heading từ nội dung file
            parent_heading = "Nội dung chính"
            for line in text.split("\n"):
                # Ưu tiên lấy dòng tiêu đề Markdown có dấu #
                if line.strip().startswith("#"):
                    parent_heading = line.replace("#", "").strip()
                    break
                # Nếu không có #, thử lấy từ thuộc tính title: "..." ở đầu file
                elif line.strip().startswith("title:"):
                    parent_heading = line.replace("title:", "").replace('"', '').strip()
            
            results.append({
                "content": text,
                "pageNumber": 1,
                "chunkIndex": idx,
                "parent_heading": parent_heading,
                "parent_content": text  # Gán toàn bộ text của chunk làm ngữ cảnh lớn (Parent Content)
            })
        return results


@app.post("/v1/embeddings")
async def get_embeddings(payload: dict = Body(...)):
    inputs = payload.get("input", payload.get("texts", []))
    if isinstance(inputs, str):
        inputs = [inputs]

    embeddings = []
    for text in inputs:
        prefix = "passage: " if payload.get("mode") == "passage" else "query: "
        vector = model.encode(f"{prefix}{text}").tolist()
        embeddings.append(vector)

    return {"embeddings": embeddings}
