1. Bản chất của "LEFT JOIN thay vì FULL/INNER JOIN"Trong thiết kế Hybrid Search (FTS + Vector), sai lầm lớn nhất là dùng FTS như một cái "cổng chặn" (Gatekeeper). Nếu bạn bắt buộc câu query FTS phải trả về kết quả thì mới lấy (INNER JOIN), hoặc lấy cả hai nhưng triệt tiêu nếu một bên rỗng, hệ thống sẽ sụp đổ ngay khi dính Teencode.Khi đổi sang LEFT JOIN lấy vector_ranked làm gốc:Vector Search đóng vai trò bảo hiểm: Như đã phân tích, mô hình Vector (như e5-small hay AITeamVN/Vietnamese_Embedding) cực kỳ giỏi trong việc nhận diện ngữ nghĩa tổng thể kể cả câu dính teencode, viết tắt. Nó sẽ luôn tìm ra 5-10 chunks ứng viên có liên quan.FTS đóng vai trò "Kẻ ban phát Bonus": FTS lúc này không có quyền sinh quyền sát để loại bỏ kết quả nữa. Nếu FTS tìm thấy từ khóa chuẩn, nó cộng thêm điểm (boost rank) thông qua công thức RRF để đẩy chunk đó lên đầu. Nếu FTS bị "mù" do teencode (ví dụ chữ trg), giá trị trả về là COALESCE(..., 0) $\rightarrow$ Chunk đó vẫn được giữ lại nhờ điểm số của Vector.2. Giải pháp unaccent xử lý bao nhiêu phần trăm bài toán?Sử dụng unaccent trực tiếp trong GIN Index của PostgreSQL là một tuyệt chiêu giúp giải quyết 90% bài toán gõ thiếu dấu mà không tốn một chút tài nguyên CPU/RAM nào cho các model AI sửa từ:SQLCREATE EXTENSION IF NOT EXISTS unaccent;
Khi bạn cấu hình FTS Index bọc qua hàm unaccent:Văn bản trong DB: "Lịch nghỉ lễ" $\xrightarrow{\text{unaccent}}$ "lich nghi le"Query của User: "lich nghi le" $\xrightarrow{\text{unaccent}}$ "lich nghi le"Kết quả: Khớp chính xác 100%!3. Bản đồ phân chia trách nhiệm hoàn hảo cho hệ thốngBằng cách áp dụng kiến trúc này, bạn đã đưa hệ thống về đúng sơ đồ phân cấp trách nhiệm chuẩn của một hệ thống RAG thực tế:Thành phần câu hỏiGặp lỗi gì?Thằng nào giải quyết?Cơ chế hoạt độnglich nghi leThiếu dấuPostgreSQL (unaccent)Chuyển cả DB và Query về dạng không dấu rồi khớp chính xác.ok, vayTừ thừa, nhiễuJava (TsQueryBuilder STOPWORDS)Lọc sạch ở tầng Backend trước khi gửi xuống DB.trgTeencode/Viết tắtVector SearchHiểu bản chất ngữ nghĩa của câu để kéo đúng chunk về; FTS miss từ này cũng không sao nhờ LEFT JOIN.🛠️ Một tinh chỉnh nhỏ để SQL chạy tối ưu trên PostgreSQLĐoạn SQL Claude viết cho bạn về mặt logic là hoàn hảo, nhưng có một điểm nhỏ về mặt tối ưu hiệu năng (Performance) trên PostgreSQL bạn cần lưu ý:Trong block bm25_ranked:SQLSELECT id, ROW_NUMBER() OVER (...) AS rank
FROM document_chunks
WHERE ts_content @@ to_tsquery('simple', :tsQuery) -- Chỗ này chưa dùng unaccent
LIMIT :candidateK
Nếu bạn đã tạo Index Unaccent:SQLCREATE INDEX idx_chunks_fts_unaccent 
ON document_chunks USING GIN(to_tsvector('simple', unaccent(full_searchable_text)));
Thì trong câu truy vấn SQL của RagService, bạn bắt buộc phải viết phần WHERE trùng khớp hoàn toàn với cấu trúc của Index thì Postgres mới ăn được GIN Index (tránh bị Full Table Scan gây chậm DB):SQL-- Sửa lại block bm25_ranked cho chuẩn index unaccent
bm25_ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY ts_rank(to_tsvector('simple', unaccent(full_searchable_text)), 
                                to_tsquery('simple', unaccent(:tsQuery))) DESC
           ) AS rank
    FROM document_chunks
    WHERE to_tsvector('simple', unaccent(full_searchable_text)) @@ to_tsquery('simple', unaccent(:tsQuery))
    LIMIT :candidateK
)

🗺️ Quy trình Triển khai 4 Bước (Production Workflow)
Bước 1: Cấu hình và Đánh Index unaccent dưới PostgreSQL
Mục tiêu là chuẩn bị cho Postgres một bộ Index Full-Text Search có khả năng tự động "cào bằng" dấu tiếng Việt, biến lịch hay lich về chung một định dạng để đối chiếu.

Bạn mở Tool quản lý DB (DBeaver/pgAdmin) hoặc viết một file Migration mới để chạy các lệnh SQL sau:

SQL


-- 1. Bật extension unaccent nếu chưa có
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2. Thêm cột tổ hợp text tìm kiếm (nếu dự án của bạn dùng cơ chế Generated Column)
-- Hoặc nếu bạn đã có cột full_searchable_text, hãy đè Index này lên:
CREATE INDEX IF NOT EXISTS idx_chunks_fts_unaccent
ON document_chunks
USING GIN(to_tsvector('simple', unaccent(full_searchable_text)));
Bước 2: Tinh gọn TsQueryBuilder.java ở Backend
Bây giờ, do gánh nặng bẫy từ viết tắt, khôi phục dấu đã được chuyển giao cho Vector và unaccent dưới DB, bạn có thể vứt bỏ hoàn toàn các hàm map teencode cồng kềnh. TsQueryBuilder quay về đúng bản chất của nó: Một bộ lọc Stopwords và Ưu tiên từ đơn chất lượng cao.

Hãy giữ nguyên hàm buildSmart(query) v3 của bạn, nhưng loại bỏ các đoạn xử lý nắn chuỗi teencode phức tạp. Hàm chỉ cần làm 2 việc:

Quét cụm từ ghép chuẩn (để sinh <->).

Tách từ đơn còn lại, lọc STOPWORDS và định dạng :* (Không cần bận tâm user gõ có dấu hay không, vì lát nữa SQL sẽ bọc hàm unaccent bên ngoài).

Bước 3: Cập nhật Hàm hybridSearch trong RagService.java
Đây là trái tim của đợt refactor này. Bạn cần sửa lại câu lệnh Native Query gửi xuống Postgres, chuyển đổi điều kiện liên kết từ FULL OUTER JOIN / INNER JOIN sang LEFT JOIN lấy Vector làm gốc, đồng thời bọc hàm unaccent.

Đoạn SQL trong Repository / Service của bạn sẽ được cập nhật như sau:

SQL


WITH vector_ranked AS (
    -- 1. Luôn lấy ra top các ứng viên bằng Vector Search (Bảo hiểm ngữ nghĩa/teencode)
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY embedding <=> CAST(:queryVector AS vector)
           ) AS rank
    FROM document_chunks
    ORDER BY embedding <=> CAST(:queryVector AS vector)
    LIMIT :candidateK
),
bm25_ranked AS (
    -- 2. Tìm kiếm FTS chính xác có bọc unaccent (Chỉ dùng để cộng điểm thưởng)
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY ts_rank(to_tsvector('simple', unaccent(full_searchable_text)), 
                                to_tsquery('simple', unaccent(:tsQuery))) DESC
           ) AS rank
    FROM document_chunks
    WHERE to_tsvector('simple', unaccent(full_searchable_text)) @@ to_tsquery('simple', unaccent(:tsQuery))
    LIMIT :candidateK
),
rrf_scored AS (
    -- 3. LEFT JOIN: Kết quả Vector luôn có mặt, FTS match thì được cộng thêm điểm
    SELECT
        v.id,
        (1.0 / (60 + v.rank)) + COALESCE(1.0 / (60 + b.rank), 0.0) AS rrf_score
    FROM vector_ranked v
    LEFT JOIN bm25_ranked b ON v.id = b.id
)
-- 4. Bốc dữ liệu chi tiết trả về cho Spring Boot
SELECT dc.content, dc.source_file AS sourceFile,
       dc.year, dc.doc_type AS docType,
       dc.section, dc.parent_id AS parentId
FROM rrf_scored r
JOIN document_chunks dc ON dc.id = r.id
ORDER BY r.rrf_score DESC
LIMIT :topK;
Bước 4: Viết Integration Test kiểm nghiệm (Verification)
Để chứng minh giải pháp này hoạt động hoàn hảo trước hội đồng nhà trường, bạn hãy tạo một hàm Test trong Spring Boot nạp đúng câu query bị lỗi ở lượt trước:

Java


@Autowired
private RagService ragService;

@Test
void testTeencodeQuerySucceeds() {
    String noisyQuery = "ok vay lich nghi le cua trg ra sao?";
    
    // Chạy qua toàn bộ pipeline Hybrid Search mới
    List<SearchResult> results = ragService.search(noisyQuery);
    
    // Kiểm tra xem kết quả đầu tiên trả về có phải là tài liệu về Lịch nghỉ lễ không
    assertFalse(results.isEmpty(), "Hệ thống không được trả về kết quả rỗng!");
    assertTrue(results.get(0).getContent().contains("nghỉ lễ") 
               || results.get(0).getSourceFile().contains("Lịch nghỉ"), 
               "Lỗi: Không tìm thấy tài liệu lịch nghỉ lễ chuẩn!");
}