spring_api  | 2026-05-19T08:30:16.765Z  INFO 211 --- [fbu_chat] [           main] com.tomzxy.fbu_chat.FbuChatApplication   : Started FbuChatApplication in 5.067 seconds (process running for 5.387)
fbuai       | INFO:     127.0.0.1:53304 - "GET /health HTTP/1.1" 200 OK
fbuai       | INFO:     127.0.0.1:45166 - "GET /health HTTP/1.1" 200 OK
fbu_chatui  | 172.18.0.1 - - [19/May/2026:08:30:44 +0000] "GET / HTTP/1.1" 304 0 "-" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36" "-"
spring_api  | 2026-05-19T08:30:46.489Z  INFO 211 --- [fbu_chat] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
spring_api  | 2026-05-19T08:30:46.587Z  INFO 211 --- [fbu_chat] [nio-8080-exec-1] com.tomzxy.fbu_chat.service.RagService   : Encoding query using AI Service...
Batches: 100%|██████████| 1/1 [00:00<00:00, 41.08it/s]
fbuai       | INFO:     172.18.0.3:41070 - "POST /v1/embeddings HTTP/1.1" 200 OK
spring_api  | 2026-05-19T08:30:46.672Z  INFO 211 --- [fbu_chat] [nio-8080-exec-1] com.tomzxy.fbu_chat.service.RagService   : Searching pgvector database (topK=5, year=null, docType=null)...
spring_api  | 2026-05-19T08:30:46.735Z  INFO 211 --- [fbu_chat] [nio-8080-exec-1] com.tomzxy.fbu_chat.service.RagService   : Calling Groq LLM Generator...
fbu_chatui  | 172.18.0.1 - - [19/May/2026:08:30:48 +0000] "POST /api/chat HTTP/1.1" 200 1170 "http://100.116.126.66/" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36" "-"


hello
Xin chào! Tôi là trợ lý AI của trường FBU. Tôi sẵn sàng giúp đỡ bạn với bất kỳ câu hỏi nào liên quan đến quy chế đào tạo của trường. Bạn có thể đặt câu hỏi cụ thể để tôi có thể hỗ trợ bạn tốt hơn.

📎 Nguồn: quyche_daotao_fbu_2026.txt

