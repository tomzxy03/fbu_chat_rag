"""Image processor – OCR bằng pytesseract."""

from PIL import Image
import pytesseract
from .base import BaseProcessor


class ImageProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tiff"]

    def extract_text(self, file_path: str) -> str:
        image = Image.open(file_path)
        # OCR tiếng Việt + tiếng Anh
        text = pytesseract.image_to_string(image, lang="vie+eng")
        return text
