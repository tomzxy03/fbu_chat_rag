import { ChatComposer } from '../components/chat/ChatComposer';
import { MessageBubble } from '../components/chat/MessageBubble';
import { TypingIndicator } from '../components/chat/TypingIndicator';
import { WelcomeState } from '../components/chat/WelcomeState';

export function ChatPage({ chat, token, onLogin }) {
  return (
    <main className="chat-main">
      <header className="topbar">
        <div>
          <p>Cuộc trò chuyện</p>
          <h2>{chat.chatTitle}</h2>
        </div>
        {!token && (
          <button className="link-button" type="button" onClick={onLogin}>
            Đăng nhập để lưu lịch sử
          </button>
        )}
      </header>

      <section className="messages" ref={chat.messagesRef}>
        {chat.messages.length === 0 && <WelcomeState onSuggestion={(text) => chat.sendMessage(null, text)} />}
        {chat.messages.map((message, index) => (
          <MessageBubble key={`${message.role}-${index}`} message={message} />
        ))}
        {chat.isSending && <TypingIndicator />}
      </section>

      <ChatComposer
        isSending={chat.isSending}
        query={chat.query}
        sendMessage={chat.sendMessage}
        setQuery={chat.setQuery}
      />
    </main>
  );
}
