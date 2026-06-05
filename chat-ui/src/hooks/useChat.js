import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { askQuestion, createConversationTitle, getConversationMessages, getConversations } from '../services/chatService';

const DEFAULT_TITLE = 'Cuộc trò chuyện mới';
const HISTORY_WINDOW_MESSAGES = 8;

function buildAnonymousHistory(items) {
  return items
    .filter((message) => (
      (message.role === 'user' || message.role === 'assistant')
      && typeof message.content === 'string'
      && message.content.trim()
    ))
    .slice(-HISTORY_WINDOW_MESSAGES)
    .map(({ role, content }) => ({ role, content }));
}

export function useChat({ token, onUnauthorized }) {
  const navigate = useNavigate();
  const { conversationId: urlConvId } = useParams();

  const [conversations, setConversations] = useState([]);
  const [currentConvId, setCurrentConvId] = useState(null);
  const [chatTitle, setChatTitle] = useState(DEFAULT_TITLE);
  const [messages, setMessages] = useState([]);
  const [query, setQuery] = useState('');
  const [isSending, setIsSending] = useState(false);
  const messagesRef = useRef(null);

  // Khi URL có conversationId (reload hoặc paste link), load conversation đó
  useEffect(() => {
    if (!urlConvId || !token) return;
    if (urlConvId === currentConvId) return; // tránh load lại không cần thiết

    const conv = conversations.find((c) => c.id === urlConvId);
    loadConversation(urlConvId, conv?.title);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlConvId, token]);

  const loadConversations = async () => {
    if (!token) {
      setConversations([]);
      return;
    }
    try {
      setConversations(await getConversations(token, onUnauthorized));
    } catch (err) {
      console.error('Failed to load conversations', err);
    }
  };

  const resetChat = () => {
    setCurrentConvId(null);
    setChatTitle(DEFAULT_TITLE);
    setMessages([]);
    navigate('/', { replace: true });
  };

  const loadConversation = async (conversationId, title) => {
    setCurrentConvId(conversationId);
    setChatTitle(title || 'Cuộc trò chuyện');

    // Đổi URL → /chat/{id} để reload giữ được conversation
    if (conversationId) {
      navigate(`/chat/${conversationId}`, { replace: true });
    }

    try {
      setMessages(await getConversationMessages(conversationId, token, onUnauthorized));
      loadConversations();
    } catch (err) {
      console.error('Failed to load conversation', err);
    }
  };

  const sendMessage = async (event, suggestionText) => {
    event?.preventDefault();
    const text = (suggestionText ?? query).trim();
    if (!text || isSending) return;

    const history = token ? undefined : buildAnonymousHistory(messages);

    setMessages((items) => [...items, { role: 'user', content: text }]);
    setQuery('');
    setIsSending(true);

    try {
      const result = await askQuestion({
        query: text,
        conversationId: currentConvId,
        history,
        token,
        onUnauthorized
      });

      // Khi server tạo conversation mới → đổi URL
      if (result.conversationId && result.conversationId !== currentConvId) {
        setCurrentConvId(result.conversationId);
        navigate(`/chat/${result.conversationId}`, { replace: true });
      }

      setChatTitle(createConversationTitle(text));
      setMessages((items) => [
        ...items,
        {
          role: 'assistant',
          content: result.answer,
          messageId: result.messageId,
          sources: result.sources,
          images: result.images
        }
      ]);
      if (token) loadConversations();
    } catch (err) {
      setMessages((items) => [...items, { role: 'assistant', content: `Lỗi: ${err.message}` }]);
    } finally {
      setIsSending(false);
    }
  };

  useEffect(() => {
    if (token) loadConversations();
  }, [token]);

  useEffect(() => {
    messagesRef.current?.scrollTo({ top: messagesRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, isSending]);

  return {
    chatTitle,
    conversations,
    currentConvId,
    isSending,
    loadConversation,
    loadConversations,
    messages,
    messagesRef,
    query,
    resetChat,
    sendMessage,
    setConversations,
    setQuery
  };
}
