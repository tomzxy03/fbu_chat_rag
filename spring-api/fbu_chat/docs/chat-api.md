# Chat API Notes For Frontend

## POST `/api/chat`

Endpoint nay ho tro ca user da dang nhap va user an danh.

### Request body

```json
{
  "query": "Con nganh ke toan thi sao?",
  "conversationId": "optional-conversation-uuid",
  "history": [
    {
      "role": "user",
      "content": "Hoc phi nganh CNTT bao nhieu?"
    },
    {
      "role": "assistant",
      "content": "Hoc phi nganh CNTT la ..."
    }
  ],
  "year": 2025,
  "docType": "quy_dinh",
  "topK": 5
}
```

### Fields

- `query`: cau hoi hien tai, bat buoc.
- `conversationId`: chi gui khi user da dang nhap va tiep tuc mot conversation cu. Bo trong de backend tao conversation moi.
- `history`: chi can gui cho user an danh. Day la cac message truoc do dang luu trong React state.
- `year`, `docType`, `topK`: bo loc tai lieu, optional.

### History contract

Frontend gui `history` dang mang object theo thu tu thoi gian cu den moi:

```ts
type ChatHistoryMessage = {
  role: "user" | "assistant";
  content: string;
};
```

Chi gui cac message da xay ra truoc cau hoi hien tai. Khong dua `query` moi nhat vao `history`, vi backend se tu dong dua `query` vao user prompt cuoi.

Backend chi nhan `role` la `user` hoac `assistant`; cac role khac se bi bo qua. Backend chi su dung toi da 8 message gan nhat de bao ve token.

### Flow

- User da dang nhap: frontend khong can gui `history`. Backend lay history tu PostgreSQL theo `conversationId`, sau do luu message moi.
- User an danh: backend khong luu DB. Frontend can giu messages trong React state va gui cac message truoc do qua `history`.
- Cau hoi xa giao ro rang nhu `xin chao`, `cam on`, `ban la ai`: backend tra loi truc tiep, khong tim chunk tai lieu, va `sources` se la mang rong.
- Cau hoi can tra cuu nhung he thong khong tim thay chunk tai lieu dang tin cay: backend tra fallback, khong goi LLM sinh cau tra loi tu context rong, va `sources` se la mang rong.

### Anonymous example

Request lan 1:

```json
{
  "query": "Hoc phi nganh CNTT bao nhieu?",
  "history": []
}
```

Request lan 2:

```json
{
  "query": "Con nganh ke toan thi sao?",
  "history": [
    {
      "role": "user",
      "content": "Hoc phi nganh CNTT bao nhieu?"
    },
    {
      "role": "assistant",
      "content": "Hoc phi nganh CNTT la ..."
    }
  ]
}
```

### Logged-in example

```json
{
  "query": "Con nganh ke toan thi sao?",
  "conversationId": "0d4df6b7-8c8b-4c8d-bf46-bc3eb80bb719"
}
```

For logged-in chat, do not send `history`; backend ignores client-side history and trusts database history.

### Casual message response

```json
{
  "conversationId": null,
  "messageId": null,
  "query": "xin chao",
  "answer": "Chao ban. Toi la tro ly AI cua FBU...",
  "sources": []
}
```

Frontend nen hien thi answer binh thuong va khong render khu vuc nguon khi `sources` rong.

### No-data fallback response

```json
{
  "conversationId": "0d4df6b7-8c8b-4c8d-bf46-bc3eb80bb719",
  "messageId": "ea7a2d34-921c-4b6e-a342-901bc0931234",
  "query": "Bao gio co lich thi lai tot nghiep nam 2026?",
  "answer": "Hien tai he thong du lieu cua minh chua co thong tin chinh thuc ve cau hoi: \"Bao gio co lich thi lai tot nghiep nam 2026?\"...",
  "sources": []
}
```

Voi response nay, frontend xu ly giong casual response: hien thi `answer` va khong render khu vuc nguon.

### Response Body (Dữ liệu trả về)

Khi API xử lý thành công, hệ thống trả về cấu trúc JSON sau:

```json
{
  "conversationId": "0d4df6b7-8c8b-4c8d-bf46-bc3eb80bb719",
  "messageId": "ea7a2d34-921c-4b6e-a342-901bc0931234",
  "query": "Con nganh ke toan thi sao?",
  "answer": "- Học phí ngành Kế toán tại FBU năm 2025 là...\n- Sinh viên có thể nộp học phí theo...",
  "sources": [
    {
      "file": "Quy_dinh_hoc_phi_2025.pdf",
      "year": 2025,
      "docType": "quy_dinh"
    }
  ]
}

Lưu ý cho Frontend khi xử lý Response:
Cập nhật conversationId: - Với user đã đăng nhập, nếu request gửi lên có conversationId bằng null, Backend sẽ tự động tạo một cuộc hội thoại mới trong DB và trả về UUID của nó trong Response.

Frontend phải bắt lấy conversationId này để set vào React State, mục đích là để các request (câu hỏi) tiếp theo thuộc cùng phiên chat đó sẽ gửi kèm ID này lên, tránh việc Backend tạo ra nhiều cuộc hội thoại trùng lặp.

Với user ẩn danh (Anonymous), conversationId trả về sẽ luôn là null.

Render câu trả lời (answer):

Nội dung answer được trả về dưới dạng chuỗi có chứa định dạng Markdown (dấu gạch đầu dòng -, chữ in đậm ).

Frontend nên dùng thư viện như react-markdown để render chuỗi này thành giao diện HTML đẹp đẽ thay vì dùng thẻ <div> thông thường.

Hiển thị Nguồn trích dẫn (sources):

Danh sách sources chứa thông tin các file tài liệu nội bộ FBU mà AI đã đọc để trích xuất câu trả lời.

Frontend sử dụng mảng này để render một khu vực riêng biệt phía dưới câu trả lời (ví dụ: các thẻ Badge nhỏ hiển thị tên file trích dẫn), giúp tăng tính minh bạch của thông tin. Tuyệt đối không tự chế chữ "Nguồn:" vào giao diện chat.
