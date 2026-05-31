import { useEffect, useRef, useState } from 'react';
import { askQuestion, createConversationTitle, getConversationMessages, getConversations } from '../services/chatService';

const DEFAULT_TITLE = 'Cuộc trò chuyện mới';

export function useChat({ token, onUnauthorized }) {
  const [conversations, setConversations] = useState([]);
  const [currentConvId, setCurrentConvId] = useState(null);
  const [chatTitle, setChatTitle] = useState(DEFAULT_TITLE);
  const [messages, setMessages] = useState([]);
  const [query, setQuery] = useState('');
  const [isSending, setIsSending] = useState(false);
  const messagesRef = useRef(null);

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
  };

  const loadConversation = async (conversationId, title) => {
    setCurrentConvId(conversationId);
    setChatTitle(title || 'Cuộc trò chuyện');

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

    setMessages((items) => [...items, { role: 'user', content: text }]);
    setQuery('');
    setIsSending(true);

    try {
      const result = await askQuestion({
        query: text,
        conversationId: currentConvId,
        token,
        onUnauthorized
      });

      if (result.conversationId) setCurrentConvId(result.conversationId);
      setChatTitle(createConversationTitle(text));
      setMessages((items) => [...items, { role: 'assistant', content: result.answer }]);
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
