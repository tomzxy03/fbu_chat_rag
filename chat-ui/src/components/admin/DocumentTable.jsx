import { FileText, Trash2 } from 'lucide-react';

export function DocumentTable({ docs, docsLoading, onDelete }) {
  return (
    <section className="admin-panel documents-panel">
      <div className="panel-heading">
        <FileText size={20} />
        <div>
          <h3>Tài liệu đã ingest</h3>
          <p>{docs.length} tài liệu trong hệ thống</p>
        </div>
      </div>

      {docsLoading ? (
        <p className="empty-state">Đang tải...</p>
      ) : docs.length === 0 ? (
        <p className="empty-state">Chưa có tài liệu nào</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Tên file</th>
                <th>Năm</th>
                <th>Loại</th>
                <th>Chunks</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {docs.map((doc) => (
                <tr key={doc.filename}>
                  <td title={doc.filename}>{doc.filename}</td>
                  <td>{doc.year ?? '-'}</td>
                  <td>{doc.docType ?? '-'}</td>
                  <td>{doc.chunkCount ?? '-'}</td>
                  <td>
                    <button className="danger-icon" type="button" onClick={() => onDelete(doc.filename)} title="Xóa">
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
