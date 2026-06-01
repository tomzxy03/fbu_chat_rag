import * as chatRepository from '../repositories/chatRepository';

export async function getConversations(token, onUnauthorized) {
  const data = await chatRepository.fetchConversations(token, onUnauthorized);
  return Array.isArray(data) ? data : [];
}

export async function getConversationMessages(conversationId, token, onUnauthorized) {
  const data = await chatRepository.fetchConversationMessages(conversationId, token, onUnauthorized);
  return Array.isArray(data) ? data : [];
}

export async function askQuestion({ query, conversationId, history, token, onUnauthorized }) {
  const payload = { query };
  if (conversationId) payload.conversationId = conversationId;
  if (Array.isArray(history)) payload.history = history;

  const data = await chatRepository.sendChatMessage(payload, token, onUnauthorized);

  return {
    conversationId: data.conversationId,
    messageId: data.messageId,
    query: data.query,
    answer: data.answer || '',
    sources: Array.isArray(data.sources) ? data.sources : []
  };
}

export function createConversationTitle(text) {
  return text.length > 40 ? `${text.slice(0, 37)}...` : text;
}
