debian-server@tomxzy:~/fbu_chat/ai-service$ docker ps
CONTAINER ID   IMAGE                           COMMAND                  CREATED          STATUS                   PORTS                                         NAMES
8dec1859400a   nginx:alpine                    "/docker-entrypoint.…"   13 minutes ago   Up 13 minutes            0.0.0.0:80->80/tcp, [::]:80->80/tcp           fbu_chatui
581cfb6ee623   eclipse-temurin:17-jdk-alpine   "/__cacert_entrypoin…"   13 minutes ago   Up 13 minutes            0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   spring_api
70930288797d   fbu_chat-fbuai                  "uvicorn main:app --…"   13 minutes ago   Up 2 minutes (healthy)   0.0.0.0:8001->8001/tcp, [::]:8001->8001/tcp   fbuai
debian-server@tomxzy:~/fbu_chat/ai-service$ docker logs fbuai --tail 30
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/preprocessor_config.json "HTTP/1.1 404 Not Found"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/video_preprocessor_config.json "HTTP/1.1 404 Not Found"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/preprocessor_config.json "HTTP/1.1 404 Not Found"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/tokenizer_config.json "HTTP/1.1 307 Temporary Redirect"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/tokenizer_config.json "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config.json "HTTP/1.1 307 Temporary Redirect"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/config.json "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config.json "HTTP/1.1 307 Temporary Redirect"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/config.json "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/tokenizer_config.json "HTTP/1.1 307 Temporary Redirect"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/tokenizer_config.json "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2/tree/main/additional_chat_templates?recursive=false&expand=false "HTTP/1.1 404 Not Found"
INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2/tree/main?recursive=true&expand=false "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/1_Pooling/config.json "HTTP/1.1 307 Temporary Redirect"
INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/1_Pooling%2Fconfig.json "HTTP/1.1 200 OK"
INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2 "HTTP/1.1 200 OK"
INFO:main:Ready ✅
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8001 (Press CTRL+C to quit)
INFO:     127.0.0.1:44442 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:47476 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:60978 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:55066 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:41616 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:59458 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:52532 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:39544 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:36184 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:56710 - "GET /health HTTP/1.1" 200 OK
INFO:     127.0.0.1:41942 - "GET /health HTTP/1.1" 200 OK


continue result: 
spring_api  | 2026-05-19T09:54:02.929Z  INFO 202 --- [fbu_chat] [nio-8080-exec-2] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: (QĐ 93) DANH SÁCH ĐIỂM RÈN LUYỆN SINH VIEN HK1 NĂM 2025 - 2026.pdf
spring_api  | 2026-05-19T09:55:36.726Z ERROR 202 --- [fbu_chat] [nio-8080-exec-2] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Unexpected end of file from server
spring_api  | 2026-05-19T09:55:36.766Z  INFO 202 --- [fbu_chat] [nio-8080-exec-3] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: (QĐ 94) DANH SÁCH KHEN THƯỞNG TT, XS, G.pdf
spring_api  | 2026-05-19T09:55:36.772Z ERROR 202 --- [fbu_chat] [nio-8080-exec-3] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:36.808Z  INFO 202 --- [fbu_chat] [nio-8080-exec-4] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: 3. Danh sách 1950 sinh viên đủ điều kiện đi thực tập đợt 1 theo QĐ 711.pdf
spring_api  | 2026-05-19T09:55:36.813Z ERROR 202 --- [fbu_chat] [nio-8080-exec-4] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:36.917Z  INFO 202 --- [fbu_chat] [nio-8080-exec-5] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: BỘ GIÁO DỤC VÀ ĐÀO TẠO.pdf
fbuai exited with code 137 (restarting)
spring_api  | 2026-05-19T09:55:37.964Z ERROR 202 --- [fbu_chat] [nio-8080-exec-5] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.241Z  INFO 202 --- [fbu_chat] [nio-8080-exec-6] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: LỊCH THI LẠI, CẢI THIỆN HỌC ĐỢT 2 KỲ 1 NĂM HỌC 2025-2026.pdf
spring_api  | 2026-05-19T09:55:38.291Z ERROR 202 --- [fbu_chat] [nio-8080-exec-6] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.419Z  INFO 202 --- [fbu_chat] [nio-8080-exec-7] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số  776.  Vv Ban hành quy định học bổng đối với sinh viên hệ đào tạo trình độ đại học chính quy theo hệ thống tín chỉ_0001.pdf
spring_api  | 2026-05-19T09:55:38.434Z ERROR 202 --- [fbu_chat] [nio-8080-exec-7] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.513Z  INFO 202 --- [fbu_chat] [nio-8080-exec-8] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 12. Vv Công nhận danh sách sinh viên hệ đại học chính quy khóa 11 đủ điều kiện đi thực tập cuối khóa_0001.pdf
spring_api  | 2026-05-19T09:55:38.536Z ERROR 202 --- [fbu_chat] [nio-8080-exec-8] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.611Z  INFO 202 --- [fbu_chat] [nio-8080-exec-9] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 196. Vv Công nhận sinh viên hệ đại học chính quy đủ điều kiện đạt chuẩn đầu ra Ngoại ngữ ( 138 SV)_0001.pdf
spring_api  | 2026-05-19T09:55:38.627Z ERROR 202 --- [fbu_chat] [nio-8080-exec-9] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.787Z  INFO 202 --- [fbu_chat] [io-8080-exec-10] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 115. Vv  Ban hành Quy trình xét kỷ luật sinh viên_0001 (1).pdf
spring_api  | 2026-05-19T09:55:38.814Z ERROR 202 --- [fbu_chat] [io-8080-exec-10] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:38.927Z  INFO 202 --- [fbu_chat] [nio-8080-exec-1] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 116. Vv  Ban hành Quy trình xác nhận sinh viên_0001.pdf
spring_api  | 2026-05-19T09:55:38.943Z ERROR 202 --- [fbu_chat] [nio-8080-exec-1] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.044Z  INFO 202 --- [fbu_chat] [nio-8080-exec-2] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 117. Vv  Ban hành Quy trình cấp mới và cấp lại Thẻ sinh viên_0001.pdf
spring_api  | 2026-05-19T09:55:39.063Z ERROR 202 --- [fbu_chat] [nio-8080-exec-2] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.209Z  INFO 202 --- [fbu_chat] [nio-8080-exec-3] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: QĐ số. 118. Vv  Ban hành Quy trình xét miễn, giảm học phí sinh viên_0001.pdf
spring_api  | 2026-05-19T09:55:39.231Z ERROR 202 --- [fbu_chat] [nio-8080-exec-3] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.267Z  INFO 202 --- [fbu_chat] [nio-8080-exec-4] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: Số đến 19. Vv Tổ chức tết nguyên đán Bính ngọ năm 2026_0001.pdf
spring_api  | 2026-05-19T09:55:39.273Z ERROR 202 --- [fbu_chat] [nio-8080-exec-4] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.332Z  INFO 202 --- [fbu_chat] [nio-8080-exec-5] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số 10 vv rà soát các điều kiện xét tốt nghiệp.pdf
spring_api  | 2026-05-19T09:55:39.342Z ERROR 202 --- [fbu_chat] [nio-8080-exec-5] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.376Z  INFO 202 --- [fbu_chat] [nio-8080-exec-6] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 01. Vv Thi chuẩn đầu ra ngoại ngữ cho sinh viên khóa 11 hệ đại học chính quy_0001.pdf
spring_api  | 2026-05-19T09:55:39.382Z ERROR 202 --- [fbu_chat] [nio-8080-exec-6] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.407Z  INFO 202 --- [fbu_chat] [nio-8080-exec-7] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 07. Vv Nghỉ tết nguyên đán_0001.pdf
spring_api  | 2026-05-19T09:55:39.413Z ERROR 202 --- [fbu_chat] [nio-8080-exec-7] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.449Z  INFO 202 --- [fbu_chat] [nio-8080-exec-8] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 14. Vv Nộp chứng chỉ xét điều kiện tốt nghiệp_0001.pdf
spring_api  | 2026-05-19T09:55:39.456Z ERROR 202 --- [fbu_chat] [nio-8080-exec-8] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.483Z  INFO 202 --- [fbu_chat] [nio-8080-exec-9] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 147. Vv Nộp chứng chỉ xét điều kiện tốt nghiệp_0001.pdf
spring_api  | 2026-05-19T09:55:39.489Z ERROR 202 --- [fbu_chat] [nio-8080-exec-9] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.510Z  INFO 202 --- [fbu_chat] [io-8080-exec-10] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 149. Vv nghỉ ngày lễ Giỗ tổ Hùng vương (mùng 10-3 âm lịch), Ngày Chiến thắng (30-4-2026) và ngày Quốc tế Lao động (01-5-2026)_0001.pdf
spring_api  | 2026-05-19T09:55:39.515Z ERROR 202 --- [fbu_chat] [io-8080-exec-10] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.575Z  INFO 202 --- [fbu_chat] [nio-8080-exec-1] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 528. Vv  mở lớp đăng ký học phần cho sinh viên các khóa đợt 1 học kỳ 4 (kỳ phụ) năm học 2025-2026_0001 (1).pdf
spring_api  | 2026-05-19T09:55:39.585Z ERROR 202 --- [fbu_chat] [nio-8080-exec-1] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.645Z  INFO 202 --- [fbu_chat] [nio-8080-exec-2] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 528. Vv  mở lớp đăng ký học phần cho sinh viên các khóa đợt 1 học kỳ 4 (kỳ phụ) năm học 2025-2026_0001.pdf
spring_api  | 2026-05-19T09:55:39.656Z ERROR 202 --- [fbu_chat] [nio-8080-exec-2] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.682Z  INFO 202 --- [fbu_chat] [nio-8080-exec-3] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 114. Vv Thi chuẩn đầu ra ngoại ngữ cho sinh viên khóa 11 hệ đại học chính quy_0001.pdf
spring_api  | 2026-05-19T09:55:39.687Z ERROR 202 --- [fbu_chat] [nio-8080-exec-3] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.751Z  INFO 202 --- [fbu_chat] [nio-8080-exec-4] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 127. Vv Lịch thi, hình thức thi và tổ chức quản lý thi kết thúc học phần Đợt 1, học kỳ 4 ( học kỳ 4) năm học 2025-2026_0001.pdf
spring_api  | 2026-05-19T09:55:39.760Z ERROR 202 --- [fbu_chat] [nio-8080-exec-4] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.796Z  INFO 202 --- [fbu_chat] [nio-8080-exec-5] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: TB số. 151. Vv rà soát thông tin của sinh viên tốt nghiệp tháng 6 năm 2026_0001.pdf
spring_api  | 2026-05-19T09:55:39.803Z ERROR 202 --- [fbu_chat] [nio-8080-exec-5] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
spring_api  | 2026-05-19T09:55:39.848Z  INFO 202 --- [fbu_chat] [nio-8080-exec-6] c.t.fbu_chat.service.DocumentService     : Xóa dữ liệu cũ cho file: Tb MGHP số 12.pdf
spring_api  | 2026-05-19T09:55:39.862Z ERROR 202 --- [fbu_chat] [nio-8080-exec-6] c.t.f.exception.GlobalExceptionHandler   : Service unreachable: I/O error on POST request for "http://fbuai:8001/chunk": Connection refused
fbuai       | INFO:     Started server process [1]
fbuai       | INFO:     Waiting for application startup.
fbuai       | INFO:main:Starting up: loading model...
fbuai       | INFO:sentence_transformers.base.model:No device provided, using cpu
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/modules.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/modules.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config_sentence_transformers.json "HTTP/1.1 404 Not Found"
fbuai       | Warning: You are sending unauthenticated requests to the HF Hub. Please set a HF_TOKEN to enable higher rate limits and faster downloads.
fbuai       | WARNING:huggingface_hub.utils._http:Warning: You are sending unauthenticated requests to the HF Hub. Please set a HF_TOKEN to enable higher rate limits and faster downloads.
fbuai       | INFO:sentence_transformers.base.model:Loading SentenceTransformer model from intfloat/e5-small-v2.
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config_sentence_transformers.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/README.md "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/README.md "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/modules.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/modules.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/sentence_bert_config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/sentence_bert_config.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/adapter_config.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/config.json "HTTP/1.1 200 OK"
Loading weights: 100%|██████████| 199/199 [00:00<00:00, 1738.44it/s]
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/processor_config.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/preprocessor_config.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/video_preprocessor_config.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/preprocessor_config.json "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/tokenizer_config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/tokenizer_config.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/config.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/config.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/tokenizer_config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/tokenizer_config.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2/tree/main/additional_chat_templates?recursive=false&expand=false "HTTP/1.1 404 Not Found"
fbuai       | INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2/tree/main?recursive=true&expand=false "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/intfloat/e5-small-v2/resolve/main/1_Pooling/config.json "HTTP/1.1 307 Temporary Redirect"
fbuai       | INFO:httpx:HTTP Request: HEAD https://huggingface.co/api/resolve-cache/models/intfloat/e5-small-v2/ffb93f3bd4047442299a41ebb6fa998a38507c52/1_Pooling%2Fconfig.json "HTTP/1.1 200 OK"
fbuai       | INFO:httpx:HTTP Request: GET https://huggingface.co/api/models/intfloat/e5-small-v2 "HTTP/1.1 200 OK"
fbuai       | INFO:main:Ready ✅
fbuai       | INFO:     Application startup complete.
fbuai       | INFO:     Uvicorn running on http://0.0.0.0:8001 (Press CTRL+C to quit)