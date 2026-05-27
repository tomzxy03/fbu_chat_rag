# top 5 level chunk:
Giải mã "5 Levels of Text Splitting"
Level 1: Character Splitting (Cắt mù)

Cách làm: Cứ đủ 500 ký tự là chặt cái rụp.

Đánh giá: Thảm họa. Cắt đứt đôi một câu, đứt đôi một từ.

Level 2: Recursive Character Text Splitting (Cắt đệ quy - Langchain Default)

Cách làm: Cố gắng cắt theo cụm văn bản lớn trước (đoạn văn \n\n), nếu vẫn to quá thì cắt theo câu (. ), to nữa thì cắt theo từ (khoảng trắng).

Đánh giá: Khá an toàn cho text thông thường, nhưng sẽ làm vỡ tan tành các bảng biểu học phí (Tầng 4) của bạn.

Level 3: Document Specific Splitting (Cắt theo cấu trúc - Chính là cái chúng ta vừa chốt!)

Cách làm: Tôn trọng định dạng gốc (Markdown, HTML). Dùng thẻ Heading (#, ##) làm ranh giới.

Đánh giá: Đây chính là kỹ thuật Hierarchical Hybrid Chunking mà mình khuyên bạn dùng cho tài liệu trường học/pháp lý. Nó giữ nguyên được cấu trúc Điều khoản, Chương mục.

Level 4: Semantic Chunking (Cắt theo toán học/ngữ nghĩa)

Cách làm: Đây là kỹ thuật cực kỳ "Big Brain". Thay vì tìm dấu chấm, dấu phẩy. Hệ thống sẽ đem từng câu đi làm Embedding (biến thành số). Sau đó tính khoảng cách vector giữa câu 1 và câu 2. Nếu hai câu đang nói về "Cơ sở vật chất", khoảng cách sẽ rất gần. Tự nhiên câu 3 chuyển sang nói về "Học phí", khoảng cách vector sẽ bị dội lên (Spike). Thuật toán sẽ lấy cái "Spike" đó làm ranh giới cắt!

Đánh giá: Tuyệt đỉnh cho các bài báo, tiểu thuyết, văn xuôi (Prose) vì không có thẻ Heading. Nhưng tốn tiền chạy API embedding 2 lần.

Level 5: Agentic Chunking (Cắt bằng AI)

Cách làm: Prompt cho LLM: "Mày đọc đoạn văn này đi, tự hiểu ý nghĩa rồi tự chặt ra thành các mẩu độc lập cho tao".

Đánh giá: Tiêu tốn cực kỳ nhiều Token và thời gian (Latency), chỉ dùng cho các startup nhà giàu.

🎯 Chiến thuật tối ưu nhất cho dự án của bạn hiện tại
Khi kết hợp repo của Greg Kamradt và paper của Anthropic (sơ đồ bạn gửi trước đó), kiến trúc hoàn hảo và ít tốn kém nhất cho hệ thống RAG văn bản trường học của bạn sẽ là:

Sử dụng Level 3 (Markdown Splitter) + Kẹp thêm Contextual Retrieval của Anthropic.

Nghĩa là:

Dùng quy tắc cắt theo Heading # để gom các đoạn văn có chung cấu trúc logic lại (Tránh mất gốc và vỡ bảng).

Chấp nhận các chunk có thể hơi thiếu một chút ngữ cảnh vĩ mô.

Bù đắp cái thiếu đó bằng cách gọi hàm cho LLM sinh ra một câu Context Hint (như sơ đồ Anthropic) dán vào đầu mỗi chunk.

Bằng cách này, bạn lấy được ưu điểm của Level 3 (rẻ, nhanh, giữ bảng biểu tốt) và đạt được sức mạnh ngữ nghĩa của Level 5 (LLM tự hiểu bối cảnh) mà không bị "đốt tiền" API quá nhiều.

# giải pháp của vấn đề semantic chunk bị mất nội dung:
Sử dụng Parent-Child Chunking không những ĐƯỢC mà còn là BẮT BUỘC nếu bạn muốn vừa có độ chính xác toán học của Semantic Chunking, vừa giữ được cấu trúc phân cấp # và ## của tài liệu trường học.🧠 Cơ chế hoạt động của Parent-Child ChunkingÝ tưởng cốt lõi của kỹ thuật này là: Tách biệt dữ liệu dùng để TÌM KIẾM (Search) và dữ liệu dùng để TRẢ LỜI/HIỂN THỊ (Generation/UI).Hệ thống của bạn sẽ quản lý 2 loại chunk lồng nhau:Parent Chunk (Chunk Cha): Toàn bộ phân đoạn lớn, trọn vẹn cấu trúc logic nằm dưới thẻ ## Điều 5. Học phí. Nó giữ nguyên tất cả các bảng biểu Markdown, văn bản đi kèm và đặc biệt là các thẻ ảnh (Image tags).Child Chunks (Chunk Con): Cục Parent ở trên sẽ được băm nhỏ ra thành nhiều mẩu tí hon (khoảng 100-200 ký tự) bằng toán học Semantic Chunking hoặc Recursive Splitting.🔑 Mối quan hệ: Mỗi Child Chunk trong Database sẽ giữ một khóa ngoại parent_id trỏ ngược về Parent Chunk của nó.🔄 Luồng chạy thực tế: Giải quyết xung đột # và ## như thế nào?Khi áp dụng mô hình này vào dự án của bạn, luồng Ingest và Query sẽ chạy cực kỳ mượt mà:1. Lúc nạp dữ liệu (Ingestion Pipeline)Bạn dùng cấu trúc dữ liệu của Claude: Cắt file theo ## để tạo ra các Parent Chunks.Với mỗi Parent, bạn lưu vào bảng parent_documents (hoặc lưu file Display .md lên S3).Bạn lấy Text của Parent đó, băm nhỏ tiếp thành các Child Chunks (Level 4 - Semantic).Bạn chỉ làm Embedding cho các Child Chunks này rồi lưu vào pgvector.2. Lúc Người dùng hỏi (Query Pipeline)User hỏi: "Học phí ngành CNTT năm 2026 bao nhiêu?"Hệ thống quét vector trên bảng Child Chunks. Vì chunk con rất nhỏ và tập trung, nó sẽ bắt trúng ngay từ khóa "CNTT" và "Học phí".Bước ngoặt Backend: Khi pgvector trả về Child Chunk có điểm số cao nhất, Code Python/Laravel của bạn không thèm lấy text của Child này ném cho LLM. Thay vào đó, nó nhìn vào parent_id của Child đó để bốc toàn bộ nội dung trọn vẹn của Parent Chunk (hoặc kéo file Display từ S3 về).Backend gửi cục Parent sạch sẽ, đầy đủ cấu trúc #, ## cho LLM đọc để sinh câu trả lời.🤝 Nó khớp nối hoàn hảo với 4 sơ đồ bạn đã có như thế nào?Nếu bạn nhìn kỹ lại sơ đồ hybrid_chunking_strategy.svg mà bạn đã tải lên, thực chất Claude đang vẽ cho bạn chính là một kiến trúc Parent-Child lai (Hybrid):Bước 2 trong sơ đồ: Split theo ## heading để tạo ra các Sections $\rightarrow$ Đây chính là bước xác định Parent Chunk.Bước 3 trong sơ đồ: Nếu dài quá 800 ký tự thì cắt nhỏ tiếp bằng \n + overlap $\rightarrow$ Đây chính là bước tạo ra các Child Chunks.