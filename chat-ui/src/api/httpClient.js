
const isProduction = import.meta.env.PROD;

// Production (Vercel): dùng VITE_API_BASE_URL trỏ tới Cloudflare Tunnel
// Development: để trống → Vite proxy localhost:8080 xử lý
// Local Docker (Nginx): để trống → relative URL, Nginx proxy xử lý
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onTokenRefreshed(accessToken) {
  refreshSubscribers.map(cb => cb(accessToken));
  refreshSubscribers = [];
}

export async function request(path, options = {}) {
  const { token, onUnauthorized, headers, ...fetchOptions } = options;
  const requestHeaders = { ...(headers || {}) };

  // Note: Luôn dùng credentials include để gửi/nhận Cookie (Refresh Token)
  fetchOptions.credentials = 'include';

  const activeToken = localStorage.getItem('accessToken') || token;

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

  // Xử lý 401 - Thử refresh token
  if (res.status === 401 && !path.includes('/api/auth/refresh') && !path.includes('/api/auth/login')) {
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const refreshRes = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: 'POST',
          credentials: 'include'
        });

        if (refreshRes.ok) {
          const data = await refreshRes.json();
          const newAccessToken = data.token;
          localStorage.setItem('accessToken', newAccessToken);
          isRefreshing = false;
          onTokenRefreshed(newAccessToken);

          // Retry original request
          requestHeaders.Authorization = `Bearer ${newAccessToken}`;
          return fetch(`${API_BASE_URL}${path}`, {
            ...fetchOptions,
            headers: requestHeaders
          });
        }
      } catch (err) {
        // Refresh failed
      } finally {
        isRefreshing = false;
      }
    } else {
      // Đang refresh, đợi xong then retry
      return new Promise((resolve) => {
        subscribeTokenRefresh((newAccessToken) => {
          requestHeaders.Authorization = `Bearer ${newAccessToken}`;
          resolve(fetch(`${API_BASE_URL}${path}`, {
            ...fetchOptions,
            headers: requestHeaders
          }));
        });
      });
    }

    onUnauthorized?.();
    throw new ApiError('Phiên đăng nhập hết hạn', 401);
  }

  if (res.status === 403) {
    throw new ApiError('Không có quyền truy cập', 403);
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