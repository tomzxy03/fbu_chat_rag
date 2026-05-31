const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function request(path, options = {}) {
  const { token, onUnauthorized, headers, ...fetchOptions } = options;
  const requestHeaders = { ...(headers || {}) };

  if (token) requestHeaders.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers: requestHeaders
  });

  if (res.status === 401 || res.status === 403) {
    onUnauthorized?.();
    throw new ApiError('Phiên đăng nhập hết hạn', res.status);
  }

  return res;
}

export async function requestJson(path, options = {}) {
  const res = await request(path, options);
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new ApiError(data.message || `HTTP ${res.status}`, res.status);
  }

  return data;
}
