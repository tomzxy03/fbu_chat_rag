#!/usr/bin/env python3
"""
reingest.py — Re-ingest all PDF files from a directory via the API.

Usage:
    python reingest.py --token <ADMIN_JWT> [--base-url http://localhost:8080] [--pdf-dir ./pdf_input]
    ADMIN_TOKEN=<jwt> python reingest.py
"""

import argparse
import os
import sys
from pathlib import Path

import requests


def parse_args():
    parser = argparse.ArgumentParser(
        description="Re-ingest PDF files into the FBU Chat knowledge base."
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("ADMIN_TOKEN"),
        help="Admin JWT token (or set ADMIN_TOKEN env var)",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="Base URL of the Spring API (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--pdf-dir",
        default="./pdf_input",
        help="Directory containing PDF files to ingest (default: ./pdf_input)",
    )
    return parser.parse_args()


def main():
    args = parse_args()

    # Validate token
    if not args.token:
        print(
            "❌ Thiếu token xác thực.\n"
            "Cung cấp token bằng một trong hai cách:\n"
            "  1. --token <ADMIN_JWT>\n"
            "  2. Biến môi trường: ADMIN_TOKEN=<ADMIN_JWT>"
        )
        sys.exit(1)

    pdf_dir = Path(args.pdf_dir)
    if not pdf_dir.is_dir():
        print(f"❌ Thư mục không tồn tại: {pdf_dir}")
        sys.exit(1)

    # Case-insensitive PDF glob
    pdf_files = sorted(
        p for p in pdf_dir.iterdir()
        if p.is_file() and p.suffix.lower() == ".pdf"
    )
    if not pdf_files:
        print(f"⚠️  Không tìm thấy file PDF nào trong: {pdf_dir}")
        sys.exit(0)

    ingest_url = f"{args.base_url.rstrip('/')}/api/documents/ingest"
    headers = {"Authorization": f"Bearer {args.token}"}

    success_count = 0
    failure_count = 0

    for pdf_path in pdf_files:
        filename = pdf_path.name

        # Attempt to open the file
        try:
            file_bytes = pdf_path.read_bytes()
        except OSError:
            print(f"❌ {filename} — Không thể đọc file")
            failure_count += 1
            continue

        # Send multipart POST request
        try:
            response = requests.post(
                ingest_url,
                headers=headers,
                files={"file": (filename, file_bytes, "application/pdf")},
                timeout=120,
            )
        except requests.exceptions.ConnectionError as e:
            print(f"❌ {filename} — Lỗi kết nối: {e}")
            failure_count += 1
            continue

        # Handle HTTP errors
        if not response.ok:
            # Try to extract a message from the response body
            try:
                body = response.json()
                message = body.get("message") or body.get("error") or response.text
            except Exception:
                message = response.text or response.reason
            print(f"❌ {filename} — HTTP {response.status_code}: {message}")
            failure_count += 1
            continue

        # Parse chunk count from successful response
        try:
            data = response.json()
            chunk_count = data.get("chunkCount", data.get("chunk_count", "?"))
        except Exception:
            chunk_count = "?"

        print(f"✅ {filename} — {chunk_count} chunks")
        success_count += 1

    print(f"\nKết quả: ✅ Thành công: {success_count} | ❌ Thất bại: {failure_count}")


if __name__ == "__main__":
    main()
