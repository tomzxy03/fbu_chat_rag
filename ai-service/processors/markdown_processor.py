import os
import re
from typing import Optional
import yaml
from .base import BaseProcessor
from langchain_text_splitters import RecursiveCharacterTextSplitter

_MIN_CHILD_CHARS = 50
_MAX_CHILD_CHARS = 800
_FALLBACK_CHUNK_SIZE = 600
_FALLBACK_OVERLAP = 80   # Đủ để giữ 1-2 câu context khi buộc phải cắt giữa paragraph

# Separators theo thứ tự ưu tiên đúng cho văn bản tiếng Việt có cấu trúc:
# 1. Paragraph break (tự nhiên nhất)
# 2. Dòng heading/danh sách có số thứ tự (thường gặp trong QĐ, TB)
# 3. Danh sách chữ cái a), b), c)
# 4. Danh sách La Mã I., II., III.
# 5. Danh sách gạch đầu dòng
# 6. Câu (dấu chấm + khoảng trắng)
# 7. Dòng đơn (last resort — tránh cắt giữa câu)
_SEPARATORS = [
    "\n\n",          # paragraph break — tự nhiên nhất, ưu tiên cao nhất
    "\nĐiều ",       # điều khoản văn bản pháp lý
    "\nKhoản ",
    "\nMục ",
    "\nChương ",
    "\n[0-9]+\\. ",  # danh sách số: 1. 2. 3. — NOTE: RecursiveCharacterTextSplitter hỗ trợ regex từ v0.3+
    "\na) ", "\nb) ", "\nc) ", "\nd) ", "\ne) ",  # danh sách chữ cái
    "\nI\\. ", "\nII\\. ", "\nIII\\. ", "\nIV\\. ", "\nV\\. ",  # La Mã
    "\n- ",          # gạch đầu dòng
    "\n",            # dòng đơn
    ". ",            # câu (cuối cùng trước khi cắt từ)
    " ",
]


class MarkdownProcessor(BaseProcessor):
    """Processor tối ưu cho file _clean.md cấu trúc phân cấp trường học."""

    SUPPORTED_EXTENSIONS = [".md"]

    def extract_text(self, file_path: str) -> str:
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read()

    def process(self, file_path: str) -> list:
        raw = self.extract_text(file_path)
        if not raw or not raw.strip():
            return []

        filename = os.path.basename(file_path)
        meta = self._parse_front_matter(raw, filename)
        body = self._strip_front_matter(raw)

        parents = self._split_parents(body)
        if not parents:
            parents = [("Nội dung chính", body)]

        results = []
        chunk_idx = 0

        for parent_heading, parent_content in parents:
            children = self._split_children(parent_content)

            for child_text in children:
                if len(child_text.strip()) < _MIN_CHILD_CHARS:
                    continue

                # text_to_embed: child text thuần — không có prefix để embedding
                # chính xác về semantic, không bị kéo về hướng tên tài liệu.
                # content_to_store: giữ context prefix để hiển thị/debug.
                context_prefix = f"[Tài liệu: {meta['title']}] [{parent_heading}]\n"

                results.append({
                    "content": context_prefix + child_text,   # lưu DB để display
                    "textToEmbed": child_text,                 # embed thuần — field mới
                    "chunkIndex": chunk_idx,
                    "pageNumber": 1,
                    "parentHeading": parent_heading,
                    "parentContent": parent_content.strip(),
                    "title": meta["title"],
                    "year": meta["year"],
                    "docType": meta["type"],
                    "sourceFile": filename,
                })
                chunk_idx += 1

        return results

    # ── Các hàm xử lý private chuyển đổi mạch lạc hơn ──────────────────

    @staticmethod
    def _parse_front_matter(raw: str, filename: str) -> dict:
        defaults = {
            "source": filename,
            "year": 2026,
            "type": "general",
            "title": os.path.splitext(filename)[0],
        }
        match = re.search(r"^---\s*\n(.*?)\n---", raw, re.DOTALL | re.MULTILINE)
        if not match:
            return defaults
        try:
            parsed = yaml.safe_load(match.group(1))
            if not isinstance(parsed, dict):
                return defaults
            return {
                "source": parsed.get("source", defaults["source"]),
                "year": int(parsed.get("year", defaults["year"])),
                "type": parsed.get("type", defaults["type"]),
                "title": parsed.get("title", defaults["title"]),
            }
        except Exception:
            return defaults

    @staticmethod
    def _strip_front_matter(raw: str) -> str:
        return re.sub(r"^---\s*\n(.*?)\n---\s*\n", "", raw, flags=re.DOTALL | re.MULTILINE)

    @staticmethod
    def _split_parents(body: str) -> list[tuple[str, str]]:
        pattern = re.compile(r"^##\s+(.+)$", re.MULTILINE)
        matches = list(pattern.finditer(body))
        if not matches:
            return []

        sections = []
        for i, m in enumerate(matches):
            heading = m.group(1).strip()
            start = m.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(body)
            sections.append((heading, body[start:end].strip()))
        return sections

    def _split_children(self, parent_content: str) -> list[str]:
        """Tách nhỏ parent dựa trên ###/#### và xử lý cô lập Bảng."""
        # Bước 1: Trích xuất các khối nhỏ theo Heading cấp 3, 4
        pattern = re.compile(r"^(#{3,4})\s+(.+)$", re.MULTILINE)
        matches = list(pattern.finditer(parent_content))

        if not matches:
            return self._smart_text_splitter(parent_content)

        children = []
        preamble = parent_content[:matches[0].start()].strip()
        if preamble:
            children.extend(self._smart_text_splitter(preamble))

        for i, m in enumerate(matches):
            sub_heading = m.group(0).strip() # Giữ lại text "### Tiêu đề phụ" để làm context
            start = m.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(parent_content)
            sub_content = parent_content[start:end].strip()

            # Nếu sub-section quá dài, băm nhỏ nội dung nội bộ nhưng luôn đính kèm sub_heading ở đầu mỗi mảnh
            if len(sub_content) > _MAX_CHILD_CHARS:
                fragments = self._smart_text_splitter(sub_content)
                for frag in fragments:
                    children.append(f"{sub_heading}\n{frag}")
            else:
                children.append(f"{sub_heading}\n{sub_content}")

        return children

    # Regex nhận diện bảng Markdown hợp lệ:
    # - Dòng header: |...|
    # - Dòng separator: |---|, |:---:|, | --- |, v.v.
    # - Ít nhất 1 dòng data
    _TABLE_RE = re.compile(
        r"(?:(?:\|[^\n]+\|\n)"         # header row
        r"\|[\s|:\-]+\|\n"             # separator row (|---|)
        r"(?:\|[^\n]+\|\n?)+)",        # 1+ data rows
        re.MULTILINE,
    )

    @staticmethod
    def _make_splitter() -> RecursiveCharacterTextSplitter:
        """
        Factory tạo splitter dùng chung — tránh lặp code và đảm bảo
        tất cả các nhánh dùng cùng cấu hình.

        is_separator_regex=True cho phép dùng pattern như \n[0-9]+\\. 
        mà không cần enumerate từng số.
        """
        return RecursiveCharacterTextSplitter(
            chunk_size=_FALLBACK_CHUNK_SIZE,
            chunk_overlap=_FALLBACK_OVERLAP,
            separators=_SEPARATORS,
            is_separator_regex=True,
        )

    def _smart_text_splitter(self, text: str) -> list[str]:
        """Bóc tách bảng ra riêng biệt, phần text thường thì băm theo ký tự."""
        final_chunks = []
        last_end = 0

        for match in self._TABLE_RE.finditer(text):
            before = text[last_end:match.start()].strip()
            if before:
                if len(before) <= _MAX_CHILD_CHARS:
                    final_chunks.append(before)
                else:
                    final_chunks.extend(
                        [p.strip() for p in self._make_splitter().split_text(before) if p.strip()]
                    )

            table = match.group(0).strip()
            if table:
                final_chunks.append(table)

            last_end = match.end()

        remainder = text[last_end:].strip()
        if remainder:
            if len(remainder) <= _MAX_CHILD_CHARS:
                final_chunks.append(remainder)
            else:
                final_chunks.extend(
                    [p.strip() for p in self._make_splitter().split_text(remainder) if p.strip()]
                )

        if not final_chunks:
            if len(text.strip()) <= _MAX_CHILD_CHARS:
                return [text.strip()]
            return [p.strip() for p in self._make_splitter().split_text(text.strip()) if p.strip()]

        return [c for c in final_chunks if c]