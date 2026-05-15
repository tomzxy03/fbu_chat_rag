"""
Abstract processor pattern cho multi-format ingest.
Mỗi file type có processor riêng kế thừa từ BaseProcessor.
"""

from abc import ABC, abstractmethod
from typing import Optional


class BaseProcessor(ABC):
    """Abstract base cho tất cả file processors."""

    # Danh sách extensions mà processor này hỗ trợ
    SUPPORTED_EXTENSIONS: list[str] = []

    @abstractmethod
    def extract_text(self, file_path: str) -> str:
        """Trích xuất text thô từ file. Subclass PHẢI override."""
        ...

    def process(self, file_path: str) -> list[str]:
        """Template method: extract → chunk. Override nếu cần custom chunking."""
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        raw_text = self.extract_text(file_path)
        if not raw_text or not raw_text.strip():
            return []

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=600,
            chunk_overlap=120,
            separators=["\nĐiều", "\nKhoản", "\nMục", "\n\n", "\n", ". ", " ", ""],
        )
        chunks = splitter.split_text(raw_text)
        return [c.strip() for c in chunks if c.strip()]

    @classmethod
    def can_handle(cls, extension: str) -> bool:
        return extension.lower() in cls.SUPPORTED_EXTENSIONS
