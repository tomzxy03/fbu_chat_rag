import { useState } from 'react';
import { getDocuments, ingestDocument, removeDocument } from '../services/documentService';

export function useDocuments({ token, onUnauthorized }) {
  const [docs, setDocs] = useState([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [status, setStatus] = useState(null);

  const loadDocuments = async () => {
    if (!token) return;
    setDocsLoading(true);

    try {
      setDocs(await getDocuments(token, onUnauthorized));
    } catch {
      setStatus({ type: 'error', text: 'Không thể tải danh sách tài liệu' });
    } finally {
      setDocsLoading(false);
    }
  };

  const uploadDocument = async (payload) => {
    setStatus({ type: 'muted', text: 'Đang upload...' });
    const message = await ingestDocument(payload, token, onUnauthorized);
    setStatus({ type: 'success', text: message });
    await loadDocuments();
  };

  const deleteDocument = async (filename) => {
    await removeDocument(filename, token, onUnauthorized);
    setStatus({ type: 'success', text: `Đã xóa: ${filename}` });
    await loadDocuments();
  };

  return {
    docs,
    docsLoading,
    loadDocuments,
    setStatus,
    status,
    uploadDocument,
    deleteDocument
  };
}
