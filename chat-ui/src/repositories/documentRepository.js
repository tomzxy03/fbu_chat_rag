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

export function deleteDocument(filename, token, onUnauthorized) {
  return requestJson(`/api/documents/${encodeURIComponent(filename)}`, {
    method: 'DELETE',
    token,
    onUnauthorized
  });
}
