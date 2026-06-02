export function getStoredToken() {
  return localStorage.getItem('accessToken');
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
}

export function setStoredSession(accessToken, user) {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('user', JSON.stringify(user));
}

export function clearStoredSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('user');
}
