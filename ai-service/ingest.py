import os
import uuid
import psycopg2
from sentence_transformers import SentenceTransformer
from process import process_pdf

model = SentenceTransformer("intfloat/e5-small-v2")

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "host.docker.internal"),
    "dbname": os.getenv("DB_NAME", "ragdb"),
    "user": os.getenv("DB_USER", "raguser"),
    "password": os.getenv("DB_PASSWORD", "ragpass"),
    "port": int(os.getenv("DB_PORT", 5432)),
}


def start_ingest(pdf_path: str, year: int, doc_type: str):
    chunks = process_pdf(pdf_path)
    filename = pdf_path.split("/")[-1]

    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()

    for i, chunk in enumerate(chunks):
        embedding = model.encode(f"passage: {chunk}").tolist()
        cur.execute(
            """
            INSERT INTO document_chunks
                (id, content, embedding, source_file, chunk_index, doc_type, year)
            VALUES (%s, %s, %s::vector, %s, %s, %s, %s)
            """,
            (str(uuid.uuid4()), chunk, str(embedding), filename, i, doc_type, year),
        )

    conn.commit()
    cur.close()
    conn.close()
    print(f"Hoàn thành nạp {len(chunks)} đoạn văn bản từ {filename}")
