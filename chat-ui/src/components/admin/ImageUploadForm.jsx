import { useState } from 'react';
import { ImageUp } from 'lucide-react';

export function ImageUploadForm({ onUpload, status }) {
  const [file, setFile] = useState(null);
  const [caption, setCaption] = useState('');
  const [tags, setTags] = useState('');
  const [category, setCategory] = useState('co_so_vat_chat');
  const [uploading, setUploading] = useState(false);
  const [formError, setFormError] = useState('');

  const submitUpload = async (event) => {
    event.preventDefault();
    const trimmedCaption = caption.trim();
    const trimmedTags = tags.trim();

    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setFormError('File phải là ảnh.');
      return;
    }
    if (!trimmedCaption && !trimmedTags) {
      setFormError('Cần nhập ít nhất mô tả hoặc từ khóa.');
      return;
    }

    setUploading(true);
    setFormError('');

    try {
      await onUpload({
        file,
        caption: trimmedCaption,
        tags: trimmedTags,
        category: category.trim()
      });
      setFile(null);
      setCaption('');
      setTags('');
      setCategory('co_so_vat_chat');
      event.target.reset();
    } finally {
      setUploading(false);
    }
  };

  return (
    <form className="admin-panel upload-panel" onSubmit={submitUpload}>
      <div className="panel-heading">
        <ImageUp size={20} />
        <div>
          <h3>Upload ảnh ngữ cảnh</h3>
          <p>Ảnh sẽ được tìm bằng caption, tags và category khi người dùng hỏi liên quan</p>
        </div>
      </div>

      <label>
        Ảnh
        <input
          accept="image/*"
          onChange={(event) => {
            setFile(event.target.files?.[0] || null);
            setFormError('');
          }}
          required
          type="file"
        />
      </label>

      <label>
        Mô tả
        <textarea
          onChange={(event) => setCaption(event.target.value)}
          placeholder="Thư viện tầng 3 cơ sở Mê Linh"
          rows={3}
          value={caption}
        />
      </label>

      <label>
        Từ khóa
        <textarea
          onChange={(event) => setTags(event.target.value)}
          placeholder="thư viện, cơ sở vật chất, đọc sách, Mê Linh"
          rows={3}
          value={tags}
        />
      </label>

      <label>
        Category
        <input
          onChange={(event) => setCategory(event.target.value)}
          placeholder="co_so_vat_chat"
          type="text"
          value={category}
        />
      </label>

      <button className="primary-button" disabled={uploading || !file} type="submit">
        {uploading ? 'Đang upload ảnh...' : 'Upload ảnh'}
      </button>

      {formError && <p className="status-text error">{formError}</p>}
      {status?.text && <p className={`status-text ${status.type}`}>{status.text}</p>}
    </form>
  );
}
