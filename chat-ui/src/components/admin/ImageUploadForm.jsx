import { useState } from 'react';
import { ImageUp } from 'lucide-react';

const IMAGE_CATEGORIES = [
  { value: 'co_so_vat_chat', label: 'Cơ sở vật chất' },
  { value: 'khuon_vien', label: 'Khuôn viên' },
  { value: 'giang_duong', label: 'Giảng đường' },
  { value: 'thu_vien', label: 'Thư viện' },
  { value: 'phong_thuc_hanh', label: 'Phòng thực hành' },
  { value: 'the_thao', label: 'Thể thao' },
  { value: 'su_kien', label: 'Sự kiện' },
  { value: 'logo', label: 'Logo / nhận diện' },
  { value: 'tai_lieu', label: 'Tài liệu / thông báo' },
  { value: 'khac', label: 'Khác' }
];

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
    if (trimmedCaption.length < 10) {
      setFormError('Mô tả cần có ít nhất 10 ký tự.');
      return;
    }
    if (!trimmedTags) {
      setFormError('Cần nhập ít nhất một từ khóa.');
      return;
    }
    if (!category) {
      setFormError('Cần chọn category cho ảnh.');
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
          required
          rows={3}
          value={caption}
        />
      </label>

      <label>
        Từ khóa
        <textarea
          onChange={(event) => setTags(event.target.value)}
          placeholder="thư viện, cơ sở vật chất, đọc sách, Mê Linh"
          required
          rows={3}
          value={tags}
        />
      </label>

      <label>
        Category
        <select
          onChange={(event) => setCategory(event.target.value)}
          required
          value={category}
        >
          {IMAGE_CATEGORIES.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </label>

      <button
        className="primary-button"
        disabled={uploading || !file || !caption.trim() || !tags.trim() || !category}
        type="submit"
      >
        {uploading ? 'Đang upload ảnh...' : 'Upload ảnh'}
      </button>

      {formError && <p className="status-text error">{formError}</p>}
      {status?.text && <p className={`status-text ${status.type}`}>{status.text}</p>}
    </form>
  );
}
