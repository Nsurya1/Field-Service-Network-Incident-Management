import axios from 'axios';

// Automatically normalize baseURL to ensure /api/v1 is present
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL;
let resolvedBaseUrl = '/api/v1';

if (rawBaseUrl && rawBaseUrl.trim() !== '') {
  const clean = rawBaseUrl.trim().replace(/\/+$/, '');
  resolvedBaseUrl = clean.endsWith('/api/v1') ? clean : `${clean}/api/v1`;
}

const api = axios.create({
  baseURL: resolvedBaseUrl,
  timeout: 45000, // 45s timeout to handle Render cold starts gracefully without hanging forever
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Attach JWT token if present
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Avoid redirecting if the 401 occurred on auth endpoints (login, register)
      if (!error.config?.url?.includes('/auth/')) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.reload();
      }
    }
    return Promise.reject(error);
  }
);

export default api;
