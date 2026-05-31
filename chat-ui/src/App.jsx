import { useState } from 'react';
import { AuthModal } from './components/auth/AuthModal';
import { AUTH_MODE, ROLE } from './constants/appConstants';
import { useAuth } from './hooks/useAuth';
import { useChat } from './hooks/useChat';
import { useDocuments } from './hooks/useDocuments';
import { AppRouter } from './router/AppRouter';

function App() {
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState(AUTH_MODE.LOGIN);
  const [authError, setAuthError] = useState('');
  const auth = useAuth();
  const chat = useChat({ token: auth.token, onUnauthorized: auth.logout });
  const documents = useDocuments({ token: auth.token, onUnauthorized: auth.logout });
  const isAdmin = auth.user?.role === ROLE.ADMIN;

  const openAuth = () => {
    setAuthOpen(true);
    setAuthError('');
  };

  const closeAuth = () => {
    setAuthOpen(false);
    setAuthError('');
  };

  const changeAuthMode = (mode) => {
    setAuthMode(mode);
    setAuthError('');
  };

  return (
    <>
      <AppRouter auth={auth} chat={chat} documents={documents} isAdmin={isAdmin} onLoginClick={openAuth} />

      {authOpen && (
        <AuthModal
          auth={auth}
          authError={authError}
          authMode={authMode}
          onClose={closeAuth}
          onModeChange={changeAuthMode}
          setAuthError={setAuthError}
        />
      )}
    </>
  );
}

export default App;
