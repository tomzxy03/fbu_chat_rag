import json
import os
import glob
import subprocess
import tempfile
import pdfplumber
import pytesseract
from pdf2image import convert_from_path
from pypdf import PdfReader
from datetime import datetime
from .base import BaseProcessor

class PdfProcessor(BaseProcessor):
    SUPPORTED_EXTENSIONS = [".pdf"]

    def __init__(self):
        super().__init__()

    def _is_scan_pdf(self, file_path: str) -> bool:
        """Kiểm tra PDF có phải file scan (không có text layer) không."""
        try:
            with pdfplumber.open(file_path) as pdf:
                # Lấy mẫu 3 trang đầu để check
                sample_pages = pdf.pages[:3]
                total_text = "".join(
                    (p.extract_text() or "") for p in sample_pages
                )
                return len(total_text.strip()) < 50
        except Exception:
            return True  # Nếu không đọc được, coi là scan

    def _ocrmypdf_preprocess(self, file_path: str) -> str:
        """
        Dùng OCRmyPDF để tạo PDF có text layer từ PDF scan.
        Trả về path của file PDF mới (temp file).
        """
        tmp_out = tempfile.mktemp(suffix=".pdf")
        try:
            result = subprocess.run(
                [
                    "ocrmypdf",
                    "--language", "vie+eng",
                    "--output-type", "pdf",
                    "--optimize", "0",      # Không optimize để nhanh hơn
                    "--skip-text",          # Bỏ qua trang đã có text
                    "--quiet",
                    file_path,
                    tmp_out,
                ],
                capture_output=True,
                text=True,
                timeout=300,  # 5 phút max
            )
            if result.returncode == 0 and os.path.exists(tmp_out):
                return tmp_out
            else:
                # OCRmyPDF fail → fallback về Tesseract cũ
                print(f"    [!] OCRmyPDF failed: {result.stderr[:200]}")
                if os.path.exists(tmp_out):
                    os.unlink(tmp_out)
                return file_path
        except subprocess.TimeoutExpired:
            print(f"    [!] OCRmyPDF timeout")
            if os.path.exists(tmp_out):
                os.unlink(tmp_out)
            return file_path
        except FileNotFoundError:
            # ocrmypdf chưa được cài → fallback
            print(f"    [!] ocrmypdf not found, falling back to Tesseract")
            return file_path

    def extract_text(self, file_path: str) -> str:
        """Thỏa mãn interface BaseProcessor để tích hợp vào FastAPI /ingest"""
        original_filename = os.path.basename(file_path)

        processed_path = file_path
        tmp_created = False
        if self._is_scan_pdf(file_path):
            print(f"    [i] Scan PDF detected, running OCRmyPDF...")
            processed_path = self._ocrmypdf_preprocess(file_path)
            tmp_created = (processed_path != file_path)

        try:
            data = self.extract_all(processed_path, original_filename=original_filename)
        finally:
            if tmp_created and os.path.exists(processed_path):
                os.unlink(processed_path)

        # Build text: KHÔNG thêm header "## TRANG X" vào content để tránh nhiễu embedding
        # Chỉ dùng tên tài liệu làm context đầu mỗi page block
        doc_name = data['metadata']['source_file']
        lines = []

        for page in data["pages"]:
            page_lines = []

            if page["content"]:
                page_lines.append(page["content"])

            if page["tables"]:
                for idx, table in enumerate(page["tables"]):
                    if not table:
                        continue
                    header = table[0]
                    page_lines.append("| " + " | ".join(header) + " |")
                    page_lines.append("| " + " | ".join(["---"] * len(header)) + " |")
                    for row in table[1:]:
                        row_padded = row + [""] * (len(header) - len(row))
                        page_lines.append("| " + " | ".join(row_padded) + " |")

            if page_lines:
                lines.append("\n".join(page_lines))

        return "\n\n".join(lines)

    def extract_all(self, file_path: str, original_filename: str = None):
        """Trích xuất text và bảng từ một file PDF.
        
        original_filename: tên file gốc để lưu vào metadata,
        dùng khi file_path là temp file từ OCRmyPDF.
        """
        source_name = original_filename or os.path.basename(file_path)
        results = {
            "metadata": {
                "source_file": source_name,
                "processed_at": datetime.now().isoformat(),
                "total_pages": 0
            },
            "pages": []
        }

        with pdfplumber.open(file_path) as pdf:
            results["metadata"]["total_pages"] = len(pdf.pages)
            
            for i, page in enumerate(pdf.pages):
                text = page.extract_text()
                tables = page.extract_tables()
                
                # Làm sạch dữ liệu bảng
                clean_tables = []
                if tables:
                    for table in tables:
                        clean_table = [
                            [(cell.replace('\n', ' ').strip() if cell else "") for cell in row]
                            for row in table if any(row)
                        ]
                        if clean_table:
                            clean_tables.append(clean_table)

                results["pages"].append({
                    "page_number": i + 1,
                    "content": text.strip() if text else "",
                    "tables": clean_tables
                })

        return results

    def save_to_json(self, data, output_path):
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def export_to_markdown_for_rag(self, data, output_path):
        """Chuyển JSON thành Markdown TXT để AI dễ đọc quan hệ bảng biểu"""
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(f"# TÀI LIỆU: {data['metadata']['source_file']}\n")
            for page in data["pages"]:
                f.write(f"\n## TRANG {page['page_number']}\n")
                f.write(page["content"] + "\n\n")
                
                if page["tables"]:
                    for idx, table in enumerate(page["tables"]):
                        f.write(f"### Bảng {idx + 1}:\n")
                        if not table: continue
                        
                        # Xử lý Header và Separator cho Markdown
                        header = table[0]
                        f.write("| " + " | ".join(header) + " |\n")
                        f.write("| " + " | ".join(["---"] * len(header)) + " |\n")
                        
                        # Các hàng dữ liệu
                        for row in table[1:]:
                            # Đảm bảo số cột khớp với header
                            row_padded = row + [""] * (len(header) - len(row))
                            f.write("| " + " | ".join(row_padded) + " |\n")
                        f.write("\n")

# ================== PHẦN QUÉT FOLDER TỰ ĐỘNG ==================
def process_pdf_folder(input_folder, output_folder):
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    processor = PdfProcessor()
    
    # Quét tất cả file .pdf
    pdf_files = glob.glob(os.path.join(input_folder, "*.pdf"))
    
    if not pdf_files:
        print(f"[-] Không tìm thấy file PDF nào trong: {input_folder}")
        return

    print(f"[*] Tìm thấy {len(pdf_files)} file. Đang xử lý...")

    for pdf_path in pdf_files:
        file_name = os.path.basename(pdf_path)
        print(f"==> Đang xử lý: {file_name}")
        
        try:
            # 1. Trích xuất toàn bộ
            data = processor.extract_all(pdf_path)
            
            # 2. Tạo đường dẫn xuất file
            base_name = os.path.splitext(file_name)[0]
            json_out = os.path.join(output_folder, f"{base_name}.json")
            txt_out = os.path.join(output_folder, f"{base_name}_rag.txt")
            
            # 3. Lưu JSON (cho dữ liệu cấu trúc)
            processor.save_to_json(data, json_out)
            
            # 4. Lưu Markdown TXT (cho RAG)
            processor.export_to_markdown_for_rag(data, txt_out)
            
            print(f"    [v] Hoàn thành: {base_name}")
        except Exception as e:
            print(f"    [x] Lỗi khi xử lý {file_name}: {e}")

if __name__ == "__main__":
    # Cấu hình đường dẫn folder tại đây
    INPUT_DIR = "pdf_input"      # Thư mục chứa các file PDF thô
    OUTPUT_DIR = "pdf_output"  # Thư mục chứa JSON và TXT sau khi xử lý
    
    process_pdf_folder(INPUT_DIR, OUTPUT_DIR)