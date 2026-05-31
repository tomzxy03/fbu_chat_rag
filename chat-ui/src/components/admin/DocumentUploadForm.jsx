import { useState } from 'react';
import { Upload } from 'lucide-react';
import { DOC_TYPES } from '../../constants/appConstants';

export function DocumentUploadForm({ onUpload, status }) {
  const [file, setFile] = useState(null);
  const [year, setYear] = useState('2026');
  const [docType, setDocType] = useState('quy_che');
  const [uploading, setUploading] = useState(false);

  const submitUpload = async (event) => {
    event.preventDefault();
    if (!file) return;
    setUploading(true);

    try {
      await onUpload({ file, year, docType });
      setFile(null);
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
          <p>PDF, Word, text, markdown, CSV, JSON hoặc hình ảnh</p>
        </div>
      </div>

      <label>
        File
        <input
          accept=".pdf,.docx,.doc,.txt,.json,.md,.csv,.png,.jpg,.jpeg"
          onChange={(event) => setFile(event.target.files?.[0] || null)}
          required
          type="file"
        />
      </label>

      <div className="field-row">
        <label>
          Năm
          <input min="2020" max="2030" onChange={(event) => setYear(event.target.value)} type="number" value={year} />
        </label>
        <label>
          Loại tài liệu
          <select onChange={(event) => setDocType(event.target.value)} value={docType}>
            {DOC_TYPES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <button className="primary-button" disabled={uploading || !file} type="submit">
        {uploading ? 'Đang upload...' : 'Upload'}
      </button>

      {status?.text && <p className={`status-text ${status.type}`}>{status.text}</p>}
    </form>
  );
}
