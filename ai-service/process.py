from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader


def process_pdf(file_path: str) -> list[str]:
    """Đọc PDF và chia nhỏ thành chunks có overlap, hỗ trợ văn bản quy chế tiếng Việt."""
    reader = PdfReader(file_path)
    full_text = []

    for page_num, page in enumerate(reader.pages):
        text = page.extract_text()
        if text and text.strip():
            full_text.append(f"[Trang {page_num + 1}] {text}")

    combined_text = "\n".join(full_text)

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=600,
        chunk_overlap=120,
        separators=["\nĐiều", "\nKhoản", "\nMục", "\n\n", "\n", ". ", " ", ""],
    )

    chunks = text_splitter.split_text(combined_text)
    return [c.strip() for c in chunks if c.strip()]
