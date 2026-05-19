#!/usr/bin/env python3
"""
reingest.py — Re-ingest all PDF files from a directory via the Spring API.

Chạy tuần tự từng file với delay giữa các file để tránh OOM trên máy yếu.

Usage:
    python3 reingest.py --token <ADMIN_JWT>
    python3 reingest.py --token <ADMIN_JWT> --base-url http://localhost:8080 --pdf-dir ./pdf_input
    ADMIN_TOKEN=<jwt> python3 reingest.py
"""

import argparse
import os
import sys
import time
from pathlib import Path

import requests


def parse_args():
    parser = argparse.ArgumentParser(
        description="Re-ingest PDF files into the FBU Chat knowledge base."
    )
    parser.add_argument(
        "--token",
        default=os.environ.get("ADMIN_TOKEN"),
        help="Admin JWT token (hoặc set env ADMIN_TOKEN)",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="Base URL của Spring API (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--pdf-dir",
        default="./pdf_input",
        help="Thư mục chứa PDF (default: ./pdf_input)",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=5.0,
        help="Giây chờ giữa các file (default: 5.0) — tránh OOM trên AI service",
    )
    return parser.parse_args()


def main():
    args = parse_args()

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

    pdf_files = sorted(
        p for p in pdf_dir.iterdir()
        if p.is_file() and p.suffix.lower() == ".pdf"
    )
    if not pdf_files:
        print(f"⚠️  Không tìm thấy file PDF nào trong: {pdf_dir}")
        sys.exit(0)

    print(f"📂 Tìm thấy {len(pdf_files)} file PDF trong {pdf_dir}")
    print(f"⏱  Delay giữa các file: {args.delay}s\n")

    ingest_url = f"{args.base_url.rstrip('/')}/api/documents/ingest"
    headers = {"Authorization": f"Bearer {args.token}"}

    success_count = 0
    failure_count = 0

    for i, pdf_path in enumerate(pdf_files, 1):
        filename = pdf_path.name
        print(f"[{i}/{len(pdf_files)}] Đang xử lý: {filename}")

        # Đọc file
        try:
            file_bytes = pdf_path.read_bytes()
        except OSError as e:
            print(f"  ❌ Không thể đọc file: {e}\n")
            failure_count += 1
            continue

        # Gửi request
        try:
            response = requests.post(
                ingest_url,
                headers=headers,
                files={"file": (filename, file_bytes, "application/pdf")},
                timeout=180,  # 3 phút — PDF lớn cần thời gian
            )
        except requests.exceptions.ConnectionError as e:
            print(f"  ❌ Lỗi kết nối: {e}\n")
            failure_count += 1
            # Đợi lâu hơn nếu connection error — AI service có thể đang restart
            if i < len(pdf_files):
                print(f"  ⏳ Đợi 15s để AI service ổn định...")
                time.sleep(15)
            continue
        except requests.exceptions.Timeout:
            print(f"  ❌ Timeout sau 180s\n")
            failure_count += 1
            continue

        if not response.ok:
            try:
                body = response.json()
                message = body.get("message") or body.get("error") or response.text
            except Exception:
                message = response.text or response.reason
            print(f"  ❌ HTTP {response.status_code}: {message}\n")
            failure_count += 1
            # Nếu 503 (AI service down), đợi lâu hơn
            if response.status_code == 503 and i < len(pdf_files):
                print(f"  ⏳ AI service không khả dụng, đợi 20s...")
                time.sleep(20)
            continue

        try:
            data = response.json()
            chunk_count = data.get("chunkCount", data.get("chunk_count", "?"))
        except Exception:
            chunk_count = "?"

        print(f"  ✅ Thành công — {chunk_count} chunks\n")
        success_count += 1

        # Delay giữa các file để AI service giải phóng RAM
        if i < len(pdf_files):
            time.sleep(args.delay)

    print("─" * 50)
    print(f"Kết quả: ✅ Thành công: {success_count} | ❌ Thất bại: {failure_count}")


if __name__ == "__main__":
    main()
