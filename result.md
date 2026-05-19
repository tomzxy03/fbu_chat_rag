 2026-05-19T08:19:06.607Z  INFO 208 --- [fbu_chat] [           main] com.tomzxy.fbu_chat.FbuChatApplication   : Started FbuChatApplication in 5.07 seconds (process running for 5.413)
fbuai       | INFO:     127.0.0.1:46524 - "GET /health HTTP/1.1" 200 OK
fbu_chatui  | 172.18.0.1 - - [19/May/2026:08:19:19 +0000] "GET / HTTP/1.1" 304 0 "-" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36" "-"
spring_api  | 2026-05-19T08:19:21.516Z  INFO 208 --- [fbu_chat] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
spring_api  | 2026-05-19T08:19:21.625Z  INFO 208 --- [fbu_chat] [nio-8080-exec-1] com.tomzxy.fbu_chat.service.RagService   : Encoding query using AI Service...
Batches: 100%|██████████| 1/1 [00:00<00:00, 39.42it/s]
fbuai       | INFO:     172.18.0.3:50902 - "POST /v1/embeddings HTTP/1.1" 200 OK
spring_api  | 2026-05-19T08:19:21.720Z  INFO 208 --- [fbu_chat] [nio-8080-exec-1] com.tomzxy.fbu_chat.service.RagService   : Searching pgvector database (topK=5, year=null, docType=null)...
spring_api  | 2026-05-19T08:19:21.799Z ERROR 208 --- [fbu_chat] [nio-8080-exec-1] c.t.f.exception.GlobalExceptionHandler   : Runtime exception: Could not set value of type [com.pgvector.PGvector]: 'com.tomzxy.fbu_chat.entity.DocumentChunk.embedding' (setter)


review of gemini: 
Vấn đề cốt lõi ở đây là sự lệch pha giữa Driver JDBC (PostgreSQL), Thư viện pgvector và Hibernate 6.

Mặc dù bạn dùng @JdbcTypeCode(SqlTypes.VECTOR), nhưng Hibernate 6.x chưa có cơ chế mặc định để chuyển đổi đối tượng com.pgvector.PGvector sang java.util.List<Float>. Khi ứng dụng chạy, Driver trả về một đối tượng PGvector, Hibernate cố gắng đẩy nó vào List<Float> và gây ra lỗi PropertyAccessException.