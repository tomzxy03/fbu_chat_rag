import * as chatRepository from '../repositories/chatRepository';

export async function getConversations(token, onUnauthorized) {
  const data = await chatRepository.fetchConversations(token, onUnauthorized);
  return Array.isArray(data) ? data : [];
}

export async function getConversationMessages(conversationId, token, onUnauthorized) {
  const data = await chatRepository.fetchConversationMessages(conversationId, token, onUnauthorized);
  return Array.isArray(data) ? data : [];
}

export async function askQuestion({ query, conversationId, token, onUnauthorized }) {
  const payload = { query };
  if (conversationId) payload.conversationId = conversationId;

  const data = await chatRepository.sendChatMessage(payload, token, onUnauthorized);

  return {
    conversationId: data.conversationId,
    answer: appendSources(data.answer || '', data.sources)
  };
}

export function createConversationTitle(text) {
  return text.length > 40 ? `${text.slice(0, 37)}...` : text;
}

function appendSources(answer, sources) {
  if (!Array.isArray(sources) || sources.length === 0) return answer;

  const uniqueFiles = [...new Set(sources.map((source) => source.file).filter(Boolean))];
  if (uniqueFiles.length === 0) return answer;

  return `${answer}\n\nNguồn: ${uniqueFiles.join(', ')}`;
}
