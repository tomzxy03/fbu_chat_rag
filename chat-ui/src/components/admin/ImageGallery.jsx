import { Image, Trash2 } from 'lucide-react';

export function ImageGallery({ images, imagesLoading, onDelete }) {
  return (
    <section className="admin-panel images-panel">
      <div className="panel-heading">
        <Image size={20} />
        <div>
          <h3>Ảnh đã upload</h3>
          <p>{images.length} ảnh dùng cho trả lời có ngữ cảnh</p>
        </div>
      </div>

      {imagesLoading ? (
        <p className="empty-state">Đang tải ảnh...</p>
      ) : images.length === 0 ? (
        <p className="empty-state">Chưa có ảnh nào</p>
      ) : (
        <div className="image-admin-grid">
          {images.map((image) => (
            <article className="image-admin-card" key={image.id}>
              <a href={image.url} target="_blank" rel="noreferrer" title="Mở ảnh">
                <img src={image.url} alt={image.caption || 'Ảnh tài liệu'} loading="lazy" />
              </a>
              <div className="image-admin-meta">
                <div>
                  <strong title={image.caption}>{image.caption || 'Không có mô tả'}</strong>
                  <span>{image.category || 'khac'}</span>
                </div>
                <button className="danger-icon" type="button" onClick={() => onDelete(image)} title="Xóa ảnh">
                  <Trash2 size={16} />
                </button>
              </div>
              {image.tags && <p className="image-tags" title={image.tags}>{image.tags}</p>}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
