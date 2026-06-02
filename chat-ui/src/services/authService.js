import * as authRepository from '../repositories/authRepository';

export async function login(credentials) {
  const data = await authRepository.login(credentials);
  return {
    token: data.token,
    user: {
      username: data.username,
      role: data.role
    }
  };
}

export async function registerAndLogin(credentials) {
  await authRepository.register(credentials);
  return login(credentials);
}

export async function logout() {
  try {
    await authRepository.logout();
  } catch (err) {
    // Ignore error on logout
  }
}
