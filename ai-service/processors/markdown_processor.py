import os
import re
from typing import Optional
import yaml
from .base import BaseProcessor
from langchain_text_splitters import RecursiveCharacterTextSplitter

_MIN_CHILD_CHARS = 50
_MAX_CHILD_CHARS = 800
_FALLBACK_CHUNK_SIZE = 600
_FALLBACK_OVERLAP = 0  # Với văn bản cấu trúc/pháp lý, hạn chế overlap bừa bãi gây nhiễu


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

                # Prepend context sạch để embedding chuẩn
                context_prefix = f"[Tài liệu: {meta['title']}] [{parent_heading}]\n"
                full_content = context_prefix + child_text

                # ĐỒNG NHẤT KEY VỚI JAVA DTO (camelCase)
                results.append({
                    "content": full_content,
                    "chunkIndex": chunk_idx,
                    "pageNumber": 1,
                    "parentHeading": parent_heading,
                    "parentContent": parent_content.strip(),
                    "title": meta["title"],
                    "year": meta["year"],
                    "docType": meta["type"],
                    "sourceFile": meta["source"],
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

    def _smart_text_splitter(self, text: str) -> list[str]:
        """Bóc tách bảng ra riêng biệt, phần text thường thì băm theo ký tự."""
        # Tách text thành các khối dựa trên dòng kẻ bảng để bảo vệ bảng không bị chặt đôi
        blocks = re.split(r"(\n(?:\|[^\n]+\|\n)+)", f"\n{text}\n")
        final_chunks = []

        for block in blocks:
            block_clean = block.strip()
            if not block_clean:
                continue
            
            # Nếu khối này là bảng -> Giữ nguyên làm 1 chunk độc lập
            if block_clean.startswith("|") and block_clean.endswith("|"):
                final_chunks.append(block_clean)
            else:
                # Text thường -> Tiến hành băm nhỏ bằng LangChain
                if len(block_clean) <= _MAX_CHILD_CHARS:
                    final_chunks.append(block_clean)
                else:
                    splitter = RecursiveCharacterTextSplitter(
                        chunk_size=_FALLBACK_CHUNK_SIZE,
                        chunk_overlap=_FALLBACK_OVERLAP,
                        separators=["\nĐiều ", "\nKhoản ", "\n- ", "\n\n", "\n", ". ", " "],
                    )
                    final_chunks.extend([p.strip() for p in splitter.split_text(block_clean) if p.strip()])

        return final_chunks