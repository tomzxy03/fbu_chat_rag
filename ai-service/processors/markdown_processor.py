"""
MarkdownProcessor — Xử lý file _clean.md (đã được DeepSeek cấu trúc hóa).

Chiến lược Parent-Child Chunking:
  - Parent chunk: mỗi section ## (heading level 2)
  - Child chunk : mỗi sub-section ###/#### bên trong parent
  - Nếu parent không có sub-heading → fallback recursive split (500 chars / 100 overlap)
  - Nếu child > 800 chars và không phải bảng → recursive split
  - Bảng Markdown (|---|) được giữ nguyên trong 1 chunk

Return format: list[dict] với keys:
  content, chunkIndex, parentHeading, title, year, docType, sourceFile
"""

import os
import re
from typing import Optional

import yaml
from .base import BaseProcessor


# Regex khớp thẻ heading Markdown
_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+)$", re.MULTILINE)

# Threshold
_MIN_CHILD_CHARS = 50
_MAX_CHILD_CHARS = 800
_FALLBACK_CHUNK_SIZE = 500
_FALLBACK_OVERLAP = 100


class MarkdownProcessor(BaseProcessor):
    """Processor cho file _clean.md có cấu trúc ## / ### / ####."""

    SUPPORTED_EXTENSIONS = [".md"]

    # ── public interface ──────────────────────────────────────────────

    def extract_text(self, file_path: str) -> str:
        """Đọc raw text từ file .md."""
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read()

    def process(self, file_path: str) -> list:
        """
        Override hoàn toàn template method của BaseProcessor.
        Trả về list[dict] thay vì list[str] để mang thêm metadata.
        """
        raw = self.extract_text(file_path)
        if not raw or not raw.strip():
            return []

        filename = os.path.basename(file_path)

        # 1. Parse YAML front matter
        meta = self._parse_front_matter(raw, filename)
        body = self._strip_front_matter(raw)

        # 2. Split theo ## → parent sections
        parents = self._split_parents(body)
        if not parents:
            # Toàn bộ file không có ## → coi như 1 parent
            parents = [("Nội dung chính", body)]

        # 3. Tạo child chunks
        results = []
        chunk_idx = 0

        for parent_heading, parent_content in parents:
            children = self._split_children(parent_content, parent_heading, meta)

            for child_text in children:
                # Prepend context
                context_prefix = f"[Tài liệu: {meta['title']}] [{parent_heading}]\n"
                full_content = context_prefix + child_text

                # Filter quá ngắn (tính theo text gốc, không tính prefix)
                if len(child_text.strip()) < _MIN_CHILD_CHARS:
                    continue

                results.append({
                    "content": full_content,
                    "chunkIndex": chunk_idx,   # Java expects camelCase for this
                    "pageNumber": 1,
                    "parent_heading": parent_heading,
                    "parent_content": parent_content.strip(),
                    "title": meta["title"],
                    "year": meta["year"],
                    "doc_type": meta["type"],
                    "source_file": meta["source"],
                })
                chunk_idx += 1

        return results

    # ── front matter ──────────────────────────────────────────────────

    @staticmethod
    def _parse_front_matter(raw: str, filename: str) -> dict:
        """Parse YAML front matter hoặc derive từ filename."""
        defaults = {
            "source": filename,
            "year": 2026,
            "type": "general",
            "title": os.path.splitext(filename)[0],
            "issued_by": "",
        }

        # Dùng re.search thay vì re.match để bỏ qua BOM hoặc whitespace ở đầu file
        match = re.search(r"^---\s*\n(.*?)\n---", raw, re.DOTALL | re.MULTILINE)
        if not match:
            return defaults

        try:
            content = match.group(1)
            parsed = yaml.safe_load(content)
            if not isinstance(parsed, dict):
                return defaults

            # Normalize values
            res = {
                "source": parsed.get("source", defaults["source"]),
                "year": parsed.get("year", defaults["year"]),
                "type": parsed.get("type", defaults["type"]),
                "title": parsed.get("title", defaults["title"]),
                "issued_by": parsed.get("issued_by", defaults["issued_by"]),
            }
            # Cố gắng ép kiểu year về int nếu có thể
            try:
                res["year"] = int(res["year"])
            except (ValueError, TypeError):
                res["year"] = defaults["year"]
            
            return res
        except Exception as e:
            # log or print error here if needed
            return defaults

    @staticmethod
    def _strip_front_matter(raw: str) -> str:
        """Loại bỏ YAML front matter khỏi nội dung."""
        return BaseProcessor._strip_yaml_front_matter(raw)

    # ── split parents (## heading) ────────────────────────────────────

    @staticmethod
    def _split_parents(body: str) -> list[tuple[str, str]]:
        """
        Split theo ## heading (level 2 chính xác).
        Trả về list[(heading_text, section_content)].
        """
        # Tìm tất cả ## heading (chính xác 2 dấu #, không phải ### hay #)
        pattern = re.compile(r"^##\s+(.+)$", re.MULTILINE)
        matches = list(pattern.finditer(body))

        if not matches:
            return []

        sections = []
        for i, m in enumerate(matches):
            heading = m.group(1).strip()
            start = m.end()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(body)
            content = body[start:end].strip()
            sections.append((heading, content))

        return sections

    # ── split children (### / ####) ───────────────────────────────────

    def _split_children(
        self,
        parent_content: str,
        parent_heading: str,
        meta: dict,
    ) -> list[str]:
        """
        Split parent content theo ### / #### heading.
        Fallback recursive split nếu không có sub-heading hoặc child quá dài.
        """
        # Tìm ### hoặc #### headings
        pattern = re.compile(r"^(#{3,4})\s+(.+)$", re.MULTILINE)
        matches = list(pattern.finditer(parent_content))

        if not matches:
            # Không có sub-heading → fallback split toàn bộ parent
            return self._fallback_split(parent_content)

        children = []

        # Text trước heading đầu tiên (nếu có)
        preamble = parent_content[: matches[0].start()].strip()
        if preamble and len(preamble) >= _MIN_CHILD_CHARS:
            children.extend(self._fallback_split(preamble))

        for i, m in enumerate(matches):
            start = m.start()
            end = matches[i + 1].start() if i + 1 < len(matches) else len(parent_content)
            section_text = parent_content[start:end].strip()

            # Kiểm tra nếu section chứa bảng Markdown → giữ nguyên
            if self._contains_table(section_text):
                children.append(section_text)
            elif len(section_text) > _MAX_CHILD_CHARS:
                children.extend(self._fallback_split(section_text))
            else:
                children.append(section_text)

        return children

    # ── fallback recursive split ──────────────────────────────────────

    @staticmethod
    def _fallback_split(text: str) -> list[str]:
        """
        Recursive character split cho text quá dài.
        Sử dụng separators phù hợp với văn bản pháp lý tiếng Việt.
        """
        if len(text) <= _MAX_CHILD_CHARS:
            return [text]

        from langchain_text_splitters import RecursiveCharacterTextSplitter

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=_FALLBACK_CHUNK_SIZE,
            chunk_overlap=_FALLBACK_OVERLAP,
            separators=[
                "\nĐiều ",
                "\nKhoản ",
                "\n\n",
                "\n",
                ". ",
                " ",
                "",
            ],
        )
        parts = splitter.split_text(text)
        return [p.strip() for p in parts if len(p.strip()) >= _MIN_CHILD_CHARS]

    # ── table detection ───────────────────────────────────────────────

    @staticmethod
    def _contains_table(text: str) -> bool:
        """Detect bảng Markdown (có ít nhất 1 dòng separator |---|)."""
        return bool(re.search(r"\|[\s\-:]+\|", text))
