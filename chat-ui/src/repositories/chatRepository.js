import { requestJson } from '../api/httpClient';

export function fetchConversations(token, onUnauthorized) {
  return requestJson('/api/chat/conversations', { token, onUnauthorized });
}

export function fetchConversationMessages(conversationId, token, onUnauthorized) {
  return requestJson(`/api/chat/conversations/${conversationId}/messages`, { token, onUnauthorized });
}

export function sendChatMessage(payload, token, onUnauthorized) {
  const headers = { 'Content-Type': 'application/json' };

  return requestJson('/api/chat', {
    method: 'POST',
    headers,
    token,
    onUnauthorized,
    body: JSON.stringify(payload)
  });
}
