import { useState } from 'react';
import {
  getDocumentImages,
  getDocuments,
  ingestDocument,
  ingestDocumentImage,
  removeDocument,
  removeDocumentImage
} from '../services/documentService';

export function useDocuments({ token, onUnauthorized }) {
  const [docs, setDocs] = useState([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [images, setImages] = useState([]);
  const [imagesLoading, setImagesLoading] = useState(false);
  const [status, setStatus] = useState(null);
  const [imageStatus, setImageStatus] = useState(null);

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

  const loadImages = async () => {
    if (!token) return;
    setImagesLoading(true);

    try {
      setImages(await getDocumentImages(token, onUnauthorized));
    } catch {
      setImageStatus({ type: 'error', text: 'Không thể tải danh sách ảnh' });
    } finally {
      setImagesLoading(false);
    }
  };

  const uploadDocument = async (payload) => {
    setStatus({ type: 'muted', text: 'Đang upload...' });
    const message = await ingestDocument(payload, token, onUnauthorized);
    setStatus({ type: 'success', text: message });
    await loadDocuments();
  };

  const uploadImage = async (payload) => {
    setImageStatus({ type: 'muted', text: 'Đang upload ảnh...' });
    const message = await ingestDocumentImage(payload, token, onUnauthorized);
    setImageStatus({ type: 'success', text: message });
    await loadImages();
  };

  const deleteDocument = async (filename) => {
    await removeDocument(filename, token, onUnauthorized);
    setStatus({ type: 'success', text: `Đã xóa: ${filename}` });
    await loadDocuments();
  };

  const deleteImage = async (id) => {
    await removeDocumentImage(id, token, onUnauthorized);
    setImageStatus({ type: 'success', text: 'Đã xóa ảnh' });
    await loadImages();
  };

  return {
    docs,
    docsLoading,
    images,
    imagesLoading,
    loadDocuments,
    loadImages,
    imageStatus,
    setImageStatus,
    setStatus,
    status,
    uploadDocument,
    uploadImage,
    deleteDocument,
    deleteImage
  };
}
