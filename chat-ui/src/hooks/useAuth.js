import { useState } from 'react';
import { login as loginService, registerAndLogin } from '../services/authService';
import { clearStoredSession, getStoredToken, getStoredUser, setStoredSession } from '../utils/storage';

export function useAuth() {
  const [token, setToken] = useState(getStoredToken);
  const [user, setUser] = useState(getStoredUser);

  const setSession = (session) => {
    setStoredSession(session.token, session.user);
    setToken(session.token);
    setUser(session.user);
  };

  const login = async (credentials) => {
    const session = await loginService(credentials);
    setSession(session);
    return session;
  };

  const register = async (credentials) => {
    const session = await registerAndLogin(credentials);
    setSession(session);
    return session;
  };

  const logout = () => {
    clearStoredSession();
    setToken(null);
    setUser(null);
  };

  return {
    token,
    user,
    login,
    register,
    logout
  };
}
