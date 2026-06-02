import * as documentRepository from '../repositories/documentRepository';

export async function getDocuments(token, onUnauthorized) {
  const data = await documentRepository.fetchDocuments(token, onUnauthorized);
  return Array.isArray(data) ? data : [];
}

export async function ingestDocument(payload, token, onUnauthorized) {
  const data = await documentRepository.uploadDocument(payload, token, onUnauthorized);
  return data.message || 'Upload thành công';
}

export async function ingestDocumentImage(payload, token, onUnauthorized) {
  const data = await documentRepository.uploadDocumentImage(payload, token, onUnauthorized);
  return data.message || 'Upload ảnh thành công';
}

export function removeDocument(filename, token, onUnauthorized) {
  return documentRepository.deleteDocument(filename, token, onUnauthorized);
}
