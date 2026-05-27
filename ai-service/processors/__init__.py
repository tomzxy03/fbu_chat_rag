"""
Processor registry — tự động detect file type và chọn processor phù hợp.
Thêm processor mới chỉ cần: tạo file, import ở đây, append vào REGISTRY.
"""

import os
from typing import Optional

from .base import BaseProcessor
from .pdf_processor import PdfProcessor
from .docx_processor import DocxProcessor
from .image_processor import ImageProcessor
from .text_processor import TextProcessor
from .json_processor import JsonProcessor
from .markdown_processor import MarkdownProcessor

# Đăng ký tất cả processors — thứ tự không quan trọng
REGISTRY: list[type[BaseProcessor]] = [
    PdfProcessor,
    DocxProcessor,
    ImageProcessor,
    TextProcessor,
    JsonProcessor,
    MarkdownProcessor,
]

# Tập hợp tất cả extensions được hỗ trợ
SUPPORTED_EXTENSIONS: set[str] = set()
for proc_cls in REGISTRY:
    SUPPORTED_EXTENSIONS.update(proc_cls.SUPPORTED_EXTENSIONS)


def get_processor(filename: str) -> Optional[BaseProcessor]:
    """Trả về processor instance phù hợp dựa vào file extension. None nếu không hỗ trợ."""
    ext = os.path.splitext(filename)[1].lower()
    for proc_cls in REGISTRY:
        if proc_cls.can_handle(ext):
            return proc_cls()
    return None


def get_supported_extensions() -> list[str]:
    """Trả về danh sách tất cả extensions được hỗ trợ."""
    return sorted(SUPPORTED_EXTENSIONS)
