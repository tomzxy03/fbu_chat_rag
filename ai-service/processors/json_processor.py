"""JSON processor – chuyển structured data thành text tự nhiên."""

import json
from .base import BaseProcessor


class JsonProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".json"]

    def extract_text(self, file_path: str) -> str:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        # Flatten JSON thành dạng readable
        return self._flatten(data)

    def _flatten(self, obj, prefix: str = "") -> str:
        """Đệ quy flatten JSON thành key: value dạng text."""
        lines = []

        if isinstance(obj, dict):
            for key, value in obj.items():
                full_key = f"{prefix}.{key}" if prefix else key
                if isinstance(value, (dict, list)):
                    lines.append(self._flatten(value, full_key))
                else:
                    lines.append(f"{full_key}: {value}")

        elif isinstance(obj, list):
            for i, item in enumerate(obj):
                full_key = f"{prefix}[{i}]"
                if isinstance(item, (dict, list)):
                    lines.append(self._flatten(item, full_key))
                else:
                    lines.append(f"{full_key}: {item}")
        else:
            lines.append(f"{prefix}: {obj}" if prefix else str(obj))

        return "\n".join(lines)
