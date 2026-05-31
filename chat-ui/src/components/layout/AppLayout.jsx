import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

export function AppLayout({ auth, chat, isAdmin, onLoginClick }) {
  return (
    <div className="app-shell">
      <Sidebar auth={auth} chat={chat} isAdmin={isAdmin} onLoginClick={onLoginClick} />
      <Outlet />
    </div>
  );
}
