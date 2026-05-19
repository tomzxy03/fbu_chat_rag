"""
Abstract processor pattern cho multi-format ingest.
Mỗi file type có processor riêng kế thừa từ BaseProcessor.
"""

from abc import ABC, abstractmethod


class BaseProcessor(ABC):
    """Abstract base cho tất cả file processors."""

    SUPPORTED_EXTENSIONS: list[str] = []

    @abstractmethod
    def extract_text(self, file_path: str) -> str:
        """Trích xuất text thô từ file. Subclass PHẢI override."""
        ...

    def process(self, file_path: str) -> list[str]:
        """Template method: extract → chunk."""
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        raw_text = self.extract_text(file_path)
        if not raw_text or not raw_text.strip():
            return []

        splitter = RecursiveCharacterTextSplitter(
            # 1200 ký tự — đủ để giữ nguyên 1 điều khoản hoàn chỉnh
            # Văn bản pháp lý tiếng Việt thường có điều khoản 300-800 ký tự
            chunk_size=1200,
            # Overlap 200 để không mất context giữa các chunk liền kề
            chunk_overlap=200,
            separators=[
                # Ưu tiên cắt tại ranh giới điều khoản pháp lý
                "\nĐiều ",
                "\nKhoản ",
                "\nMục ",
                "\nChương ",
                "\nPhần ",
                "\nĐiểm ",
                # Ranh giới đoạn văn
                "\n\n",
                # Ranh giới dòng
                "\n",
                # Ranh giới câu
                ". ",
                "! ",
                "? ",
                # Fallback
                " ",
                "",
            ],
        )
        chunks = splitter.split_text(raw_text)

        # Lọc chunk quá ngắn (< 50 ký tự) — thường là header/footer rác
        return [c.strip() for c in chunks if len(c.strip()) >= 50]

    @classmethod
    def can_handle(cls, extension: str) -> bool:
        return extension.lower() in cls.SUPPORTED_EXTENSIONS
