import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import { AdminPage } from '../pages/AdminPage';
import { ChatPage } from '../pages/ChatPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export function AppRouter({ auth, chat, documents, isAdmin, onLoginClick }) {
  return (
    <Routes>
      <Route element={<AppLayout auth={auth} chat={chat} isAdmin={isAdmin} onLoginClick={onLoginClick} />}>
        <Route index element={<ChatPage chat={chat} token={auth.token} onLogin={onLoginClick} />} />
        <Route
          path="admin"
          element={<AdminPage documents={documents} isAdmin={isAdmin} onLogin={onLoginClick} token={auth.token} />}
        />
        <Route path="chat" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
