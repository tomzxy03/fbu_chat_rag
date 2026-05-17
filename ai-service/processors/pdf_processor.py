"""PDF processor – sử dụng pypdf với page markers và OCR (pytesseract) cho file scan."""

from pypdf import PdfReader
import pytesseract
from pdf2image import convert_from_path
from .base import BaseProcessor

class PdfProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".pdf"]

    def extract_text(self, file_path: str) -> str:
        reader = PdfReader(file_path)
        pages = []
        needs_ocr = False
        
        # Thử đọc bằng pypdf trước
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text and text.strip():
                pages.append(f"[Trang {i + 1}] {text.strip()}")
                
        # Nếu không có text nào được trích xuất -> có thể là PDF dạng ảnh (scan)
        text_str = "".join(pages).strip()
        if not text_str:
            needs_ocr = True

        # OCR Fallback
        if needs_ocr:
            try:
                images = convert_from_path(file_path)
                pages = []
                for i, image in enumerate(images):
                    text = pytesseract.image_to_string(image, lang='vie+eng')
                    if text and text.strip():
                        pages.append(f"[Trang {i + 1}] {text.strip()}")
            except Exception as e:
                print(f"Lỗi OCR cho file {file_path}: {e}")

        # Basic noise reduction for tabular data
        final_text = "\n".join(pages)
        # Bỏ bớt khoảng trắng dư thừa
        final_text = "\n".join([line.strip() for line in final_text.splitlines() if line.strip()])
        return final_text
