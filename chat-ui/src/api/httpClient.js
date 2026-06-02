
const isProduction = import.meta.env.PROD;

// NẾU LÀ PRODUCTION: Dùng thẳng '/api' để Nginx điều phối sang Spring Boot
// NẾU LÀ DEVELOPMENT: Ưu tiên biến môi trường, không có thì để trống để Vite Proxy (cổng 5173) lo
const API_BASE_URL = isProduction 
  ? '' 
  : (import.meta.env.VITE_API_BASE_URL || '');

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

  const activeToken = token || localStorage.getItem('token') || localStorage.getItem('accessToken');
  
  if (activeToken) {
    requestHeaders.Authorization = `Bearer ${activeToken}`;
  }

  if (!requestHeaders['Content-Type'] && !(fetchOptions.body instanceof FormData)) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    headers: requestHeaders
  });

  if (res.status === 401 || res.status === 403) {
    onUnauthorized?.();
    throw new ApiError('Không có quyền truy cập hoặc phiên đăng nhập hết hạn', res.status);
  }

  return res;
}

export async function requestJson(path, options = {}) {
  const res = await request(path, options);
  
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new ApiError(data.message || `Lỗi hệ thống (HTTP ${res.status})`, res.status);
  }

  return data;
}