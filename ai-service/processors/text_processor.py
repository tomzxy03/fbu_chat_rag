"""Plain text processor (.txt, .csv)."""

from .base import BaseProcessor


class TextProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".txt", ".csv", ".log"]

    def extract_text(self, file_path: str) -> str:
        with open(file_path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
