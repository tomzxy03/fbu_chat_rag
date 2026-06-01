userPrompt phối hợp với systemPrompt để giúp mô hình AI (Groq LLM) nhận biết và duy trì được mạch câu chuyện, từ đó đưa ra câu trả lời logic dựa trên toàn bộ bối cảnh cuộc trò chuyện.

Tuy nhiên, có một điểm khác biệt kỹ thuật cực kỳ quan trọng giữa cách viết nối chuỗi vào userPrompt (như đoạn code cũ của bạn) và cách truyền mảng messages cấu trúc (như OpenAI/Groq API quy định) mà bạn cần phân biệt rõ để không bị lỗi khi chạy thực tế:

1. Bản chất của việc "ghi nhớ" qua userPrompt (Cách nối chuỗi)
Khi bạn viết:

Java


String userPrompt = "HỘI THOẠI TRƯỚC ĐÓ:\n" + history + "\n" + "CÂU HỎI HIỆN TẠI: " + query;
Bạn đang dùng kỹ thuật "Text Flattening" (Phẳng hóa văn bản). Nghĩa là bạn gom toàn bộ quá khứ và hiện tại thành một khối chữ duy nhất thuộc vai trò người dùng (role: "user").

Ưu điểm: Đơn giản, dễ viết khi bạn quản lý lịch sử dưới dạng một chuỗi String thô đổ từ Client lên (như trường hợp Chat ẩn danh mà bạn cấu hình).

Nhược điểm: AI đôi khi sẽ bị "lẫn lộn" giữa nội dung người dùng hỏi trong quá khứ và câu trả lời của chính con Bot, vì tất cả đều bị nhét chung vào một thẻ content của role: "user".

2. Cách "ghi nhớ" chuẩn của Groq/OpenAI (Cách truyền mảng messages)
Các mô hình LLM hiện đại như Llama 3 hay ChatGPT không đọc một cục chữ thô để nhớ lịch sử. Chúng đọc một mảng các Object được phân vai rõ ràng theo trình tự thời gian.

Hãy nhìn vào đoạn code này trong file RagService.java hiện tại của bạn:

Java


List<Map<String, Object>> groqMessages = new java.util.ArrayList<>();
groqMessages.add(groqMsg1); // 1. System Prompt

// 2. Vòng lặp bốc lịch sử từ DB và add theo đúng vai trò (role)
for (Message histMsg : recentHistory.subList(...)) {
    Map<String, Object> hm = new HashMap<>();
    hm.put("role", histMsg.getRole());       // "user" hoặc "assistant"
    hm.put("content", histMsg.getContent());   // Nội dung tin nhắn thô
    groqMessages.add(hm);
}
Đây mới chính là cơ chế ghi nhớ chuẩn mực và mạnh mẽ nhất của hệ thống. Khi mảng groqMessages này được gửi sang Groq, cấu trúc JSON của nó sẽ có dạng:

JSON


[
  {"role": "system", "content": "Bạn là trợ lý AI của FBU..."},
  {"role": "user", "content": "Học phí ngành CNTT bao nhiêu?"},
  {"role": "assistant", "content": "Học phí CNTT là 700k/tín chỉ."},
  {"role": "user", "content": "Còn ngành Kế toán thì sao?"} 
]
Nhìn vào mảng này, Groq sẽ hiểu ngay lập tức câu chuyện diễn tiến từ quá khứ đến hiện tại ra sao, ai nói câu nào, từ đó tự động suy luận "Kế toán" ở câu cuối là đang hỏi tiếp về học phí.

💡 Tóm lại, bạn nên xử lý thế nào cho đúng?
Vì trong code RagService.java của bạn đang dùng cả hai cách cùng một lúc, hệ thống sẽ bị thừa dữ liệu. Để tối ưu hóa, bạn hãy chia làm 2 trường hợp như tụi mình đã phân tích:

Nếu User đã đăng nhập (Dùng nhánh Database):

Bạn hãy để vòng lặp for bốc dữ liệu từ messageRepo lo phần ghi nhớ lịch sử (Nhét vào mảng groqMessages).

Trong chuỗi userPrompt ở cuối, bạn xóa bỏ dòng "HỘI THOẠI TRƯỚC ĐÓ" đi, chỉ giữ lại CONTEXT và CÂU HỎI HIỆN TẠI.

Nếu User ẩn danh (Dùng nhánh React gửi request.getHistory() lên):

Nếu bạn đổi history từ String sang List<Map<String, String>> như mình gợi ý, bạn cũng dùng vòng lặp để add vào mảng groqMessages giống hệt như nhánh Database.

Nếu bạn vẫn muốn giữ history là dạng String thô, thì lúc này bạn bắt buộc phải giữ lại dòng "HỘI THOẠI TRƯỚC ĐÓ" trong userPrompt để AI có cái mà nhớ, vì lúc này bạn không có mảng Object lịch sử nào để ném cho nó cả.



Dữ liệu hội thoại trước đó (lịch sử chat) sẽ được lấy từ hai nguồn hoàn toàn khác nhau, phụ thuộc vào việc người dùng lúc đó đã đăng nhập (đã login) hay đang dùng ẩn danh (chưa login).Trong chính file RagService.java của bạn, luồng lấy dữ liệu thực tế đang vận hành chính xác như sau:Trường hợp 1: Nếu User ĐÃ ĐĂNG NHẬP $\rightarrow$ Lấy từ Database (PostgreSQL)Khi người dùng đã đăng nhập, bạn có userId và hệ thống sẽ lấy lịch sử được lưu trữ kiên cố dưới Database.Hãy nhìn vào đoạn code này trong hàm chat của bạn:Javaif (conversation != null) {
    // 1. CHÍNH LÀ DÒNG NÀY: Backend chủ động vào DB bốc lịch sử ra
    List<Message> recentHistory = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
    
    // 2. Lấy cửa sổ 4 cặp câu hỏi/đáp gần nhất (tối đa 8 tin nhắn)
    int fromIdx = Math.max(0, recentHistory.size() - HISTORY_WINDOW * 2);
    for (Message histMsg : recentHistory.subList(fromIdx, recentHistory.size())) {
        Map<String, Object> hm = new HashMap<>();
        hm.put("role", histMsg.getRole());       // Lấy chữ "user" hoặc "assistant" trong DB
        hm.put("content", histMsg.getContent());   // Lấy nội dung tin nhắn thô trong DB
        groqMessages.add(hm);
    }
    // ...
}
Nguồn lấy: Bảng message trong database PostgreSQL của bạn thông qua messageRepo.Flow chạy: Mỗi lần User gửi câu hỏi mới $\rightarrow$ Spring Boot tự động chạy vào DB tìm các tin nhắn cũ của conversationId đó $\rightarrow$ Nén vào mảng gửi sang Groq. Frontend (React) lúc này không cần gửi bất kỳ dòng lịch sử nào lên cả.Trường hợp 2: Nếu User CHƯA ĐĂNG NHẬP (Ẩn danh) $\rightarrow$ Lấy từ RAM Trình duyệt (React State) gửi lênKhi sinh viên dùng ẩn danh, hệ thống Backend của bạn sẽ không tạo Conversation và cũng không lưu gì vào Database (conversation == null). Lúc này, Database hoàn toàn trống rỗng.Vậy lấy dữ liệu cũ ở đâu? Phải lấy từ chính bộ nhớ của ứng dụng Front-end (React) ở phía Client.Nguồn lấy: Mảng messages nằm trong useState của React. Trình duyệt của sinh viên tự giữ đống tin nhắn này trên RAM.Flow chạy: 1. Khi sinh viên bấm gửi câu hỏi câu thứ 3, React sẽ bốc cặp câu 1 và câu 2 đang lưu trên RAM của nó, nhét vào trường history của ChatRequest.2. React bắn API POST /api/chat lên Spring Boot.3. Spring Boot nhận được ChatRequest. Vì check thấy user ẩn danh, Backend sẽ bốc dữ liệu từ chính trường request.getHistory() (do React vừa mang tới) để nạp vào Prompt gửi cho Groq.📌 Tóm lại cho bạn dễ hình dung:User đã login: Lịch sử lấy từ PostgreSQL (Backend tự túc hoàn toàn, Frontend rảnh tay).User ẩn danh: Lịch sử lấy từ RAM Trình duyệt (Frontend phải tự gom lại rồi "ship" tận tay lên cho Backend qua Request).


PHÍA BACKEND (SPRING BOOT)
Bạn cần sửa đổi file ChatRequest.java và cấu trúc lại logic gom tin nhắn trong RagService.java để tiếp nhận mảng JSON lịch sử từ Client.

1. Sửa file ChatRequest.java
Đổi kiểu dữ liệu của history từ String sang List<Map<String, String>> để nhận dạng mảng tin nhắn có cấu trúc:

Java


package com.tomzxy.fbu_chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {

    @NotBlank(message = "Câu hỏi không được để trống")
    private String query;

    private String conversationId; // null → tạo conversation mới
    
    // ĐỔI THÀNH DANH SÁCH OBJECT: Phục vụ riêng cho chat ẩn danh
    private List<Map<String, String>> history; 

    private Integer year; 
    private String docType; 
    private Integer topK = 5; 
}
2. Tối ưu lại hàm chat trong RagService.java
Bạn tìm đến đoạn chuẩn bị danh sách tin nhắn gửi sang Groq và sửa lại. Ta sẽ xóa bỏ việc nối chuỗi lịch sử vào userPrompt, thay vào đó là nạp trực tiếp vào danh sách tin nhắn theo cấu trúc chuẩn:

Java


        // --- 1. Tạo System Message mặc định ---
        Map<String, Object> groqMsg1 = new HashMap<>();
        groqMsg1.put("role", "system");
        groqMsg1.put("content", systemPrompt);

        List<Map<String, Object>> groqMessages = new java.util.ArrayList<>();
        groqMessages.add(groqMsg1);

        // --- 2. XỬ LÝ LỊCH SỬ HỘI THOẠI BIẾN ĐỘNG ---
        if (conversation != null) {
            // NHÁNH A: NGƯỜI DÙNG ĐÃ ĐĂNG NHẬP -> Chủ động bốc từ DB
            List<Message> recentHistory = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            int fromIdx = Math.max(0, recentHistory.size() - HISTORY_WINDOW * 2);
            for (Message histMsg : recentHistory.subList(fromIdx, recentHistory.size())) {
                Map<String, Object> hm = new HashMap<>();
                hm.put("role", histMsg.getRole());
                hm.put("content", histMsg.getContent());
                groqMessages.add(hm);
            }

            // Lưu vết câu hỏi hiện tại vào DB
            Message userMsg = Message.builder()
                    .conversation(conversation)
                    .role("user")
                    .content(request.getQuery())
                    .build();
            messageRepo.save(userMsg);
            
        } else if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            // NHÁNH B: NGƯỜI DÙNG ẨN DANH -> Bốc trực tiếp từ cục "history" do React gửi lên
            // Giới hạn lấy tối đa 8 tin nhắn gần nhất để bảo vệ Token
            int fromIdx = Math.max(0, request.getHistory().size() - HISTORY_WINDOW * 2);
            List<Map<String, String>> clientHistory = request.getHistory().subList(fromIdx, request.getHistory().size());
            
            for (Map<String, String> histMsg : clientHistory) {
                Map<String, Object> hm = new HashMap<>();
                hm.put("role", histMsg.get("role"));
                hm.put("content", histMsg.get("content"));
                groqMessages.add(hm);
            }
        }

        // --- 3. ĐÓNG GÓI USER PROMPT (CHỈ chứa Context và Câu hỏi mới) ---
        // Tuyệt đối không chèn biến history thô vào đây nữa để tránh bị trùng lặp dữ liệu
        String userPrompt = "CONTEXT TỪ TÀI LIỆU FBU:\n" + contextText + "\n\n" +
            "CÂU HỎI HIỆN TẠI: " + request.getQuery() + "\n\n" +
            "Trả lời (Tuân thủ tuyệt đối quy tắc định dạng nguồn):";

        Map<String, Object> groqMsg2 = new HashMap<>();
        groqMsg2.put("role", "user");
        groqMsg2.put("content", userPrompt);
        groqMessages.add(groqMsg2);