import { Send } from 'lucide-react';

export function ChatComposer({ isSending, query, sendMessage, setQuery }) {
  return (
    <form className="chat-input-area" onSubmit={sendMessage}>
      <input
        aria-label="Nhập câu hỏi"
        autoComplete="off"
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Nhập câu hỏi..."
        value={query}
      />
      <button className="send-button" disabled={isSending || !query.trim()} type="submit" title="Gửi">
        <Send size={20} />
      </button>
    </form>
  );
}
