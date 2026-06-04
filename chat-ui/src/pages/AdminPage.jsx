import { useEffect } from 'react';
import { FileText } from 'lucide-react';
import { DocumentTable } from '../components/admin/DocumentTable';
import { DocumentUploadForm } from '../components/admin/DocumentUploadForm';
import { ImageGallery } from '../components/admin/ImageGallery';
import { ImageUploadForm } from '../components/admin/ImageUploadForm';

export function AdminPage({ documents, isAdmin, onLogin, token }) {
  useEffect(() => {
    if (token && isAdmin) {
      documents.loadDocuments();
      documents.loadImages();
    }
  }, [token, isAdmin]);

  const handleUpload = async (payload) => {
    try {
      await documents.uploadDocument(payload);
    } catch (err) {
      documents.setStatus({ type: 'error', text: err.message });
    }
  };

  const handleImageUpload = async (payload) => {
    try {
      await documents.uploadImage(payload);
    } catch (err) {
      documents.setImageStatus({ type: 'error', text: err.message });
    }
  };

  const handleDelete = async (filename) => {
    if (!confirm(`Xóa tài liệu "${filename}"?`)) return;

    try {
      await documents.deleteDocument(filename);
    } catch (err) {
      documents.setStatus({ type: 'error', text: `Xóa thất bại: ${err.message}` });
    }
  };

  const handleImageDelete = async (image) => {
    if (!confirm(`Xóa ảnh "${image.caption || image.id}"?`)) return;

    try {
      await documents.deleteImage(image.id);
    } catch (err) {
      documents.setImageStatus({ type: 'error', text: `Xóa ảnh thất bại: ${err.message}` });
    }
  };

  if (!token) {
    return (
      <main className="admin-main narrow-state">
        <FileText size={42} />
        <h2>Đăng nhập để vào trang quản trị</h2>
        <button className="primary-button" type="button" onClick={onLogin}>
          Đăng nhập
        </button>
      </main>
    );
  }

  if (!isAdmin) {
    return (
      <main className="admin-main narrow-state">
        <FileText size={42} />
        <h2>Tài khoản không có quyền quản trị</h2>
        <p>Chỉ tài khoản ADMIN mới được upload và quản lý tài liệu.</p>
      </main>
    );
  }

  return (
    <main className="admin-main">
      <header className="topbar">
        <div>
          <p>Quản trị</p>
          <h2>Tài liệu và hình ảnh tri thức</h2>
        </div>
        <button
          className="secondary-button"
          type="button"
          onClick={() => {
            documents.loadDocuments();
            documents.loadImages();
          }}
        >
          Làm mới
        </button>
      </header>

      <section className="admin-grid">
        <DocumentUploadForm onUpload={handleUpload} status={documents.status} />
        <ImageUploadForm onUpload={handleImageUpload} status={documents.imageStatus} />
        <ImageGallery
          images={documents.images}
          imagesLoading={documents.imagesLoading}
          onDelete={handleImageDelete}
        />
        <DocumentTable docs={documents.docs} docsLoading={documents.docsLoading} onDelete={handleDelete} />
      </section>
    </main>
  );
}
