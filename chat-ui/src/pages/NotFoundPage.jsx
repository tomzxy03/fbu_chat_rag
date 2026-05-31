import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <main className="admin-main narrow-state">
      <h2>Không tìm thấy trang</h2>
      <Link className="primary-button route-link" to="/">
        Về trang chat
      </Link>
    </main>
  );
}
