import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Menu } from 'lucide-react';
import { Sidebar } from './Sidebar';

export function AppLayout({ auth, chat, isAdmin, onLoginClick }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="app-shell">
      <Sidebar
        auth={auth}
        chat={chat}
        isAdmin={isAdmin}
        onLoginClick={onLoginClick}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      <div className="main-content">
        {/* Hamburger button — chỉ hiện trên mobile */}
        <button
          className="hamburger-btn icon-button"
          type="button"
          aria-label="Mở menu"
          onClick={() => setSidebarOpen(true)}
        >
          <Menu size={20} />
        </button>

        <Outlet />
      </div>
    </div>
  );
}
