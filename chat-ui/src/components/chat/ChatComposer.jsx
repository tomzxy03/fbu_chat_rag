import { Send } from 'lucide-react';

export function ChatComposer({ isSending, query, sendMessage, setQuery }) {
  const handleKeyDown = (event) => {
    // Ctrl+Enter hoặc Cmd+Enter để gửi
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      if (!isSending && query.trim()) {
        sendMessage(event);
      }
    }
  };

  return (
    <form className="chat-input-area" onSubmit={sendMessage}>
      <input
        aria-label="Nhập câu hỏi"
        autoComplete="off"
        onChange={(event) => setQuery(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Nhập câu hỏi... (Ctrl+Enter để gửi)"
        value={query}
      />
      <button className="send-button" disabled={isSending || !query.trim()} type="submit" title="Gửi (Ctrl+Enter)">
        <Send size={20} />
      </button>
    </form>
  );
}
