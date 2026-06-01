Việc thiết lập kịch bản "Fallback thông minh" khi thiếu dữ liệu không chỉ giúp chatbot giữ được sự trung thực, tránh nói bừa (Hallucination), mà còn là một kênh tuyệt vời để bạn thu thập ngược lại nhu cầu thực tế của sinh viên nhằm bổ sung data sau này.Để triển khai tính năng này một cách mượt mà nhất, chúng ta sẽ kết hợp chỉ thị trong Code Backend và ép luật trong systemPrompt.🛠️ Giải pháp 1: Ép luật "Thú nhận" vào systemPrompt (Độ chính xác cao, AI tự viết mượt mà)Khi hệ thống bốc tài liệu từ Database lên nhưng điểm số (Threshold) không cao hoặc nội dung tài liệu hoàn toàn lệch pha với câu hỏi, ta cần ép LLM nhận diện sự bất hợp lý này và kích hoạt câu trả lời Fallback.Bạn hãy bổ sung đoạn cấu hình sau vào phần quy tắc của systemPrompt trong RagService.java:JavaString systemPrompt = "Bạn là trợ lý AI chuyên nghiệp của trường Đại học Tài chính - Ngân hàng Hà Nội (FBU).\n" +
    "...\n" +
    "QUY TẮC XỬ LÝ KHI THIẾU THÔNG TIN:\n" +
    "- Nếu CONTEXT được cung cấp KHÔNG CHỨA thông tin để trả lời cho CÂU HỎI HIỆN TẠI, hoặc thông tin quá sơ sài, bạn TUYỆT ĐỐI không được tự bịa ra câu trả lời.\n" +
    "- Thay vào đó, hãy trả lời chính xác theo mẫu sau hoặc biến tấu một cách thân thiện: 'Hiện tại hệ thống dữ liệu của mình chưa có thông tin chính thức về vấn đề [Tên chủ đề người dùng hỏi]. Nếu bạn biết hoặc có tài liệu về nội dung này, bạn có thể gửi phản hồi hoặc cung cấp thông tin qua Phòng Công tác Sinh viên (hoặc Fanpage/Email hỗ trợ của dự án: support-chatbot@fbu.edu.vn) để giúp mình hoàn thiện hơn nhé! Cảm ơn bạn rất nhiều.'\n" +
    "- Tuyệt đối không lấy kiến thức cũ trên Internet để đoán câu trả lời liên quan đến quy định nội bộ của FBU.";
🛠️ Giải pháp 2: Xử lý cứng ở tầng Code dựa trên kết quả Search (Chủ động hoàn toàn)Nếu bạn muốn kiểm soát tuyệt đối, không muốn tốn Token gửi sang Groq khi biết chắc chắn hệ thống không tìm thấy bất kỳ dòng tài liệu nào trong Database, bạn có thể chặn ngay sau bước Hybrid Search trong RagService.java.Nếu danh sách topContexts trả về trống rỗng (hoặc điểm tương đồng quá thấp dưới mức threshold), bạn có thể cấu hình ngắt luồng sớm:Java// --- TRONG HÀM CHAT CỦA RAGSERVICE ---

// Thực hiện tìm kiếm tài liệu
List<DocumentChunk> topContexts = hybridSearch(searchTargetQuery, request.getYear(), request.getDocType(), request.getTopK());

// BỔ SUNG LOGIC CHẶN CỨNG (Nếu câu hỏi tra cứu thông tin mà DB hoàn toàn trống rỗng)
if (isFbuInformationQuery(request.getQuery()) && (topContexts == null || topContexts.isEmpty())) {
    log.info("Không tìm thấy bất kỳ tài liệu nào khớp trong DB. Kích hoạt Fallback Response sớm.");
    
    String fallbackAnswer = String.format(
        "Hiện tại hệ thống dữ liệu của mình chưa có thông tin chính thức về câu hỏi: *\"%s\"*.\n\n" +
        "Nếu bạn biết hoặc có tài liệu chính thức về nội dung này, bạn có thể đóng góp và cung cấp thông tin qua **Hòm thư hỗ trợ dự án FBU Chatbot** hoặc gửi về **Phòng Công tác Sinh viên** để tụi mình cập nhật cho các bạn khóa sau nhé! Cảm ơn đóng góp của bạn rất nhiều. 🙏", 
        request.getQuery()
    );

    return ChatResponse.builder()
            .conversationId(conversation != null ? conversation.getId() : null)
            .query(request.getQuery())
            .answer(fallbackAnswer)
            .sources(new ArrayList<>()) // Trả về mảng nguồn rỗng
            .build();
}
💡 Mẹo nâng cấp hệ thống: Tạo bảng unanswered_questions để bắt lỗi dữ liệu (Data Gap)Để tận dụng tối đa những câu hỏi "bị thiếu data" này nhằm cải tiến Chatbot sau này, mỗi khi luồng Fallback trên được kích hoạt, bạn nên lưu câu hỏi đó vào một bảng riêng trong Database (ví dụ bảng unanswered_queries).Cách hoạt động: Khi sinh viên hỏi câu "Bao giờ có lịch thi lại tốt nghiệp năm 2026?" mà DB không có data $\rightarrow$ Bot trả lời câu Fallback xin thông tin $\rightarrow$ Đồng thời ghi câu hỏi này vào bảng unanswered_queries.Hiệu quả: Cuối tuần, bạn chỉ cần vào Postgres gõ SELECT query, COUNT(*) FROM unanswered_queries GROUP BY query ORDER BY count DESC; là bạn sẽ biết ngay sinh viên đang quan tâm đến vấn đề gì nhất mà hệ thống của mình chưa có tài liệu, từ đó chủ động đi xin file PDF của trường về nạp (Ingest) vào hệ thống!