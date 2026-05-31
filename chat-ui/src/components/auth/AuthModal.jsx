import { useState } from 'react';
import { Bot, X } from 'lucide-react';
import { AUTH_MODE } from '../../constants/appConstants';

export function AuthModal({ auth, authError, authMode, onClose, onModeChange, setAuthError }) {
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [registerForm, setRegisterForm] = useState({ username: '', password: '' });
  const [submitting, setSubmitting] = useState(false);

  const submitLogin = async (event) => {
    event.preventDefault();
    setSubmitting(true);

    try {
      await auth.login(loginForm);
      onClose();
    } catch (err) {
      setAuthError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const submitRegister = async (event) => {
    event.preventDefault();
    setSubmitting(true);

    try {
      await auth.register(registerForm);
      onClose();
    } catch (err) {
      setAuthError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Đăng nhập">
      <div className="login-card">
        <button className="modal-close" type="button" onClick={onClose} title="Đóng">
          <X size={20} />
        </button>
        <div className="login-logo">
          <Bot size={36} />
        </div>
        <h2>FBU Chat</h2>
        <p>Đăng nhập để lưu lịch sử trò chuyện</p>

        <div className="tab-switch" role="tablist" aria-label="Chọn chế độ">
          <button
            className={authMode === AUTH_MODE.LOGIN ? 'active' : ''}
            type="button"
            onClick={() => onModeChange(AUTH_MODE.LOGIN)}
          >
            Đăng nhập
          </button>
          <button
            className={authMode === AUTH_MODE.REGISTER ? 'active' : ''}
            type="button"
            onClick={() => onModeChange(AUTH_MODE.REGISTER)}
          >
            Đăng ký
          </button>
        </div>

        {authMode === AUTH_MODE.LOGIN ? (
          <form className="auth-form" onSubmit={submitLogin}>
            <input
              autoComplete="username"
              onChange={(event) => setLoginForm((value) => ({ ...value, username: event.target.value }))}
              placeholder="Tên đăng nhập"
              required
              type="text"
              value={loginForm.username}
            />
            <input
              autoComplete="current-password"
              onChange={(event) => setLoginForm((value) => ({ ...value, password: event.target.value }))}
              placeholder="Mật khẩu"
              required
              type="password"
              value={loginForm.password}
            />
            <button className="primary-button" disabled={submitting} type="submit">
              Đăng nhập
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={submitRegister}>
            <input
              minLength={3}
              onChange={(event) => setRegisterForm((value) => ({ ...value, username: event.target.value }))}
              placeholder="Tên đăng nhập (3-50 ký tự)"
              required
              type="text"
              value={registerForm.username}
            />
            <input
              minLength={6}
              onChange={(event) => setRegisterForm((value) => ({ ...value, password: event.target.value }))}
              placeholder="Mật khẩu (tối thiểu 6 ký tự)"
              required
              type="password"
              value={registerForm.password}
            />
            <button className="primary-button" disabled={submitting} type="submit">
              Đăng ký
            </button>
          </form>
        )}

        {authError && <p className="error-msg">{authError}</p>}
      </div>
    </div>
  );
}
