export function getStoredToken() {
  return localStorage.getItem('token');
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
}

export function setStoredSession(token, user) {
  localStorage.setItem('token', token);
  localStorage.setItem('user', JSON.stringify(user));
}

export function clearStoredSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}
