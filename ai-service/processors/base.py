"""
Abstract processor pattern cho multi-format ingest.
Mỗi file type có processor riêng kế thừa từ BaseProcessor.
"""

import re
from abc import ABC, abstractmethod


class BaseProcessor(ABC):
    """Abstract base cho tất cả file processors."""

    SUPPORTED_EXTENSIONS: list[str] = []

    @abstractmethod
    def extract_text(self, file_path: str) -> str:
        """Trích xuất text thô từ file. Subclass PHẢI override."""
        ...

    @staticmethod
    def _clean_ocr_noise(text: str) -> str:
        """
        Làm sạch noise phổ biến từ OCR văn bản hành chính tiếng Việt.
        Không sửa nội dung — chỉ normalize whitespace và bỏ ký tự rác.
        """
        # Bỏ ký tự control và null bytes
        text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]', '', text)

        # Normalize dấu gạch ngang (em dash, en dash → -)
        text = re.sub(r'[—–]', '-', text)

        # Bỏ các dòng chỉ có ký tự đặc biệt/số ngắn (header/footer rác)
        # Ví dụ: "eae Ngoc Anh", "- 1 -", "Page 1 of 5"
        lines = text.split('\n')
        clean_lines = []
        for line in lines:
            stripped = line.strip()
            # Bỏ dòng quá ngắn (< 3 ký tự) hoặc chỉ có số/ký tự đặc biệt
            if len(stripped) < 3:
                continue
            # Bỏ dòng chỉ có số và dấu (page numbers, separators)
            if re.match(r'^[\d\s\-_=./*|]+$', stripped):
                continue
            clean_lines.append(line)

        text = '\n'.join(clean_lines)

        # Normalize nhiều dòng trống liên tiếp → tối đa 2 dòng trống
        text = re.sub(r'\n{3,}', '\n\n', text)

        # Normalize khoảng trắng thừa trong dòng (giữ indent)
        text = re.sub(r'[ \t]{2,}', ' ', text)

        return text.strip()

    def process(self, file_path: str) -> list[str]:
        """Template method: extract → clean → chunk."""
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        raw_text = self.extract_text(file_path)
        if not raw_text or not raw_text.strip():
            return []

        # Clean OCR noise trước khi chunk
        cleaned_text = self._clean_ocr_noise(raw_text)

        splitter = RecursiveCharacterTextSplitter(
            # 1200 ký tự — đủ để giữ nguyên 1 điều khoản hoàn chỉnh
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
        chunks = splitter.split_text(cleaned_text)

        # Lọc chunk quá ngắn (< 50 ký tự) — thường là header/footer rác
        return [c.strip() for c in chunks if len(c.strip()) >= 50]

    @classmethod
    def can_handle(cls, extension: str) -> bool:
        return extension.lower() in cls.SUPPORTED_EXTENSIONS
