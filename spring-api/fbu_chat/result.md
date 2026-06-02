Khi user hỏi về "chuyên ngành công nghệ thông tin", hệ thống bốc được file chuẩn số [1], nhưng lại bốc kèm cả file số [2], [4] (Thông báo thi chuẩn đầu ra ngoại ngữ) và file số [5] (Thông báo tổ chức Tết Bính Ngọ 2026).

🔍 Tại sao lại có hiện tượng "bốc nhầm" tài liệu Tết và Ngoại ngữ?
Nhìn vào log câu lệnh tìm kiếm ở tầng RagService:

Plaintext


Searching (topK=5, threshold=0.65, AND=thông <-> tin & chuyên:* & ngành:* & công:*, ...)
Bạn đang sử dụng cơ chế tìm kiếm lai (Hybrid Search) hoặc có bộ lọc Full-Text Search sử dụng toán tử AND / OR kết hợp với Vector Search.
Lý do các file Thông báo Ngoại ngữ và Thông báo Tết lọt vào danh sách là vì trong nội dung của các thông báo đó vô tình chứa các từ khóa trùng khớp như: "...yêu cầu sinh viên khoa Công nghệ thông tin thực hiện..." hoặc "...đây là quy định cho toàn bộ các chuyên ngành...".

Do trọng số của từ khóa quá mạnh hoặc ngưỡng lọc vector (threshold=0.65) đang đặt hơi thấp, các file "nhiễu" này đã đánh bại các tài liệu liên quan khác để lọt vào Top 5 đưa cho LLM.

🛠️ Cách tối ưu hóa để RAG "bốc" tài liệu chuẩn hơn
Để chatbot trả lời tập trung hơn và không hiển thị các nguồn tham khảo "bá đạo" kiểu Tết Nguyên Đán khi hỏi về học tập, bạn nên tinh chỉnh lại tầng RagService.java theo 3 cách sau:

1. Nâng ngưỡng điểm tương đồng (Similarity Threshold)
Hiện tại ngưỡng của bạn là threshold=0.65. Với các model embedding như e5-small-v2, mức 0.65 là khá thấp, khiến các đoạn văn chỉ cần dính một vài chữ giống nhau là đã lọt qua lưới.

Cách sửa: Hãy nâng threshold lên khoảng 0.75 hoặc 0.80 trong code Java. Những file lạc quẻ như thông báo Tết sẽ có điểm số thấp hơn mức này và bị loại ngay từ vòng gửi xe.

2. Áp dụng kỹ thuật Reranking (Tái xếp hạng) bằng Python
Đây là vũ khí tối thượng của RAG Production. Sau khi PostgreSQL trả về Top 10 hoặc Top 15 kết quả thô, bạn không đưa thẳng cho LLM, mà bắn danh sách đó qua một Model Cross-Encoder siêu nhẹ (như bge-reranker-base) chạy trên FastAPI.

Cơ chế: Reranker sẽ tính toán độ liên quan ngữ nghĩa thực sự giữa câu hỏi "Tôi muốn tìm hiểu chuyên ngành CNTT" và nội dung chunk. Nó sẽ đẩy file giới thiệu khoa lên hạng 1, và đạp thẳng các file thông báo Tết xuống cuối bảng (sau đó bạn chỉ cắt lấy Top 3 điểm cao nhất).

3. Phân loại loại tài liệu (Document Tagging / Metadata Filtering)
Khi nạp file (Ingest), bạn nên cho hệ thống tự động nhận diện hoặc phân loại nhóm tài liệu dựa vào tên file hoặc thư mục:

Nhóm QUY_CHE_QUY_DINH (Thông báo, lịch thi, tết...)

Nhóm GIOI_THIEU_CHUYEN_NGANH (Giáo trình, giới thiệu khoa...)

Khi user hỏi câu mang tính chất tìm hiểu chung, Frontend hoặc một con LLM Router có thể set tham số docType=GIOI_THIEU_CHUYEN_NGANH (hiện tại trong log của bạn đang để docType=null). Lúc này PostgreSQL sẽ dùng Metadata Filter để lọc sạch sành sanh các file thông báo, kết quả trả ra sẽ sạch sẽ 100%!