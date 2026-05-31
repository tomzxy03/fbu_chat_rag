import { Bot, FileText, LogIn, LogOut, MessageSquare, Plus } from 'lucide-react';
import { NavLink, useNavigate } from 'react-router-dom';

export function Sidebar({ auth, chat, isAdmin, onLoginClick }) {
  const navigate = useNavigate();

  const handleNewChat = () => {
    chat.resetChat();
    if (auth.token) chat.loadConversations();
    navigate('/');
  };

  const handleConversationClick = (conversation) => {
    chat.loadConversation(conversation.id, conversation.title);
    navigate('/');
  };

  const handleAuthClick = () => {
    if (!auth.token) {
      onLoginClick();
      return;
    }

    auth.logout();
    chat.setConversations([]);
    chat.resetChat();
    navigate('/');
  };

  return (
    <aside className="sidebar">
      <div className="brand-row">
        <div className="brand-mark">
          <Bot size={20} />
        </div>
        <div>
          <h1>FBU Chat</h1>
          <p>Trợ lý AI sinh viên</p>
        </div>
      </div>

      <nav className="main-nav" aria-label="Điều hướng chính">
        <NavLink to="/" end>
          <MessageSquare size={17} />
          Chat
        </NavLink>
        {isAdmin && (
          <NavLink to="/admin">
            <FileText size={17} />
            Quản trị
          </NavLink>
        )}
      </nav>

      <div className="section-title">
        <span>Lịch sử</span>
        <button className="icon-button" type="button" title="Cuộc trò chuyện mới" onClick={handleNewChat}>
          <Plus size={17} />
        </button>
      </div>

      <div className="conversation-list">
        {!auth.token && <p className="empty-state">Đăng nhập để xem lịch sử</p>}
        {auth.token && chat.conversations.length === 0 && <p className="empty-state">Chưa có cuộc trò chuyện</p>}
        {chat.conversations.map((conversation) => (
          <button
            className={`conversation-item ${conversation.id === chat.currentConvId ? 'active' : ''}`}
            key={conversation.id}
            onClick={() => handleConversationClick(conversation)}
            title={conversation.title || 'Cuộc trò chuyện'}
            type="button"
          >
            {conversation.title || 'Cuộc trò chuyện'}
          </button>
        ))}
      </div>

      <div className="sidebar-footer">
        <div>
          <strong>{auth.user?.username || 'Khách'}</strong>
          <span>{auth.user?.role || 'Chưa đăng nhập'}</span>
        </div>
        <button className="auth-button" type="button" onClick={handleAuthClick}>
          {auth.token ? <LogOut size={16} /> : <LogIn size={16} />}
          {auth.token ? 'Đăng xuất' : 'Đăng nhập'}
        </button>
      </div>
    </aside>
  );
}
