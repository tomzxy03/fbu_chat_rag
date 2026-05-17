import json
import os
import glob
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
        pass

    def extract_text(self, file_path: str) -> str:
        """Thỏa mãn interface BaseProcessor để tích hợp vào FastAPI /ingest"""
        data = self.extract_all(file_path)
        
        # Chuyển đổi dict thành markdown string cho RAG chunking
        lines = [f"# TÀI LIỆU: {data['metadata']['source_file']}"]
        for page in data["pages"]:
            lines.append(f"\n## TRANG {page['page_number']}")
            if page["content"]:
                lines.append(page["content"])
            
            if page["tables"]:
                for idx, table in enumerate(page["tables"]):
                    lines.append(f"### Bảng {idx + 1}:")
                    if not table: continue
                    
                    header = table[0]
                    lines.append("| " + " | ".join(header) + " |")
                    lines.append("| " + " | ".join(["---"] * len(header)) + " |")
                    
                    for row in table[1:]:
                        row_padded = row + [""] * (len(header) - len(row))
                        lines.append("| " + " | ".join(row_padded) + " |")
                    lines.append("\n")
                    
        return "\n".join(lines)

    def _perform_ocr(self, file_path, page_index):
        """Chỉ OCR đúng trang bị thiếu text để tiết kiệm tài nguyên máy"""
        try:
            images = convert_from_path(file_path, first_page=page_index+1, last_page=page_index+1)
            if images:
                # 'vie+eng' giúp nhận diện tốt cả tiếng Việt và các mã ngành tiếng Anh
                return pytesseract.image_to_string(images[0], lang='vie+eng')
        except Exception as e:
            print(f"    [!] Lỗi OCR trang {page_index+1}: {e}")
        return ""

    def extract_all(self, file_path: str):
        """Trích xuất text và bảng từ một file PDF"""
        results = {
            "metadata": {
                "source_file": os.path.basename(file_path),
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
                        # Lọc bỏ None, xóa xuống dòng thừa để giữ cấu trúc hàng
                        clean_table = [
                            [(cell.replace('\n', ' ').strip() if cell else "") for cell in row]
                            for row in table if any(row) # Chỉ lấy hàng có dữ liệu
                        ]
                        if clean_table:
                            clean_tables.append(clean_table)

                # Nếu không có text (file scan), kích hoạt OCR
                if not text or len(text.strip()) < 20:
                    text = self._perform_ocr(file_path, i)

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