import { useState } from 'react';
import { Upload } from 'lucide-react';
import { DOC_TYPES } from '../../constants/appConstants';

export function DocumentUploadForm({ onUpload, status }) {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);

  const submitUpload = async (event) => {
    event.preventDefault();
    if (files.length === 0) return;
    setUploading(true);

    try {
      await onUpload({ files });
      setFiles([]);
      event.target.reset();
    } finally {
      setUploading(false);
    }
  };

  return (
    <form className="admin-panel upload-panel" onSubmit={submitUpload}>
      <div className="panel-heading">
        <Upload size={20} />
        <div>
          <h3>Upload tài liệu</h3>
          <p>Chọn một hoặc nhiều file Markdown</p>
        </div>
      </div>

      <label>
        Chọn files (.md)
        <input
          accept=".md"
          multiple
          onChange={(event) => setFiles(Array.from(event.target.files || []))}
          required
          type="file"
        />
      </label>

      {files.length > 0 && (
        <div className="file-preview">
          <p>Đã chọn {files.length} file:</p>
          <ul>
            {files.slice(0, 5).map((f, i) => <li key={i}>{f.name}</li>)}
            {files.length > 5 && <li>... và {files.length - 5} file khác</li>}
          </ul>
        </div>
      )}

      <button className="primary-button" disabled={uploading || files.length === 0} type="submit">
        {uploading ? 'Đang upload...' : `Upload ${files.length} files`}
      </button>

      {status?.text && <p className={`status-text ${status.type}`}>{status.text}</p>}
    </form>
  );
}
