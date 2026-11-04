import api from './api';

export const authService = {
  async login(username, password) {
    const response = await api.post('/auth/login', { username, password });
    if (!response.data || typeof response.data !== 'object' || !response.data.token) {
      throw new Error(
        typeof response.data === 'string' && response.data.includes('<!doctype')
          ? 'API returned HTML instead of JSON. Ensure VITE_API_BASE_URL is configured in Vercel.'
          : 'Invalid login response from server.'
      );
    }
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('user', JSON.stringify(response.data));
    return response.data;
  },

  async register(username, email, password) {
    const response = await api.post('/auth/register', {
      username,
      email,
      password,
    });
    if (!response.data || typeof response.data !== 'object' || !response.data.token) {
      throw new Error(
        typeof response.data === 'string' && response.data.includes('<!doctype')
          ? 'API returned HTML instead of JSON. Ensure VITE_API_BASE_URL is configured in Vercel.'
          : 'Invalid registration response from server.'
      );
    }
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('user', JSON.stringify(response.data));
    return response.data;
  },

  async forgotPassword(usernameOrEmail) {
    const response = await api.post('/auth/forgot-password', { usernameOrEmail });
    return response.data;
  },

  async resetPassword(resetToken, newPassword) {
    const response = await api.post('/auth/reset-password', { resetToken, newPassword });
    return response.data;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  getToken() {
    return localStorage.getItem('token');
  },
};
