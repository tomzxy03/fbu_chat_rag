import { requestJson } from '../api/httpClient';

export function fetchDocuments(token, onUnauthorized) {
  return requestJson('/api/documents', { token, onUnauthorized });
}

export function fetchDocumentImages(token, onUnauthorized) {
  return requestJson('/api/documents/images', { token, onUnauthorized });
}

export function uploadDocument(payload, token, onUnauthorized) {
  const formData = new FormData();

  if (Array.isArray(payload.files)) {
    payload.files.forEach(file => {
      formData.append('files', file);
    });
  } else if (payload.file) {
    formData.append('files', payload.file);
  }

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

export function deleteDocumentImage(id, token, onUnauthorized) {
  return requestJson(`/api/documents/images/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    token,
    onUnauthorized
  });
}
