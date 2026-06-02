import { requestJson } from '../api/httpClient';

export function fetchDocuments(token, onUnauthorized) {
  return requestJson('/api/documents', { token, onUnauthorized });
}

export function uploadDocument(payload, token, onUnauthorized) {
  const formData = new FormData();
  formData.append('file', payload.file);
  formData.append('year', payload.year);
  formData.append('docType', payload.docType);

  return requestJson('/api/documents/ingest', {
    method: 'POST',
    token,
    onUnauthorized,
    body: formData
  });
}

export function uploadDocumentImage(payload, token, onUnauthorized) {
  const formData = new FormData();
  formData.append('file', payload.file);
  formData.append('caption', payload.caption || '');
  formData.append('tags', payload.tags || '');
  formData.append('category', payload.category || '');

  return requestJson('/api/documents/images', {
    method: 'POST',
    token,
    onUnauthorized,
    body: formData
  });
}

export function deleteDocument(filename, token, onUnauthorized) {
  return requestJson(`/api/documents/${encodeURIComponent(filename)}`, {
    method: 'DELETE',
    token,
    onUnauthorized
  });
}
