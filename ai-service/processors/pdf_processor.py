"""PDF processor – sử dụng pypdf với page markers."""

from pypdf import PdfReader
from .base import BaseProcessor


class PdfProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".pdf"]

    def extract_text(self, file_path: str) -> str:
        reader = PdfReader(file_path)
        pages = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text and text.strip():
                pages.append(f"[Trang {i + 1}] {text}")
        return "\n".join(pages)
