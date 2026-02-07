import http from '@/http';

export const authService = {
  async login(account, password) {
    const response = await http.post('/api/auth/login', {
      account,
      password
    });
    return response.data;
  },

  async register(account, password, confirmPassword, nickname = '') {
    const response = await http.post('/api/auth/register', {
      account,
      password,
      confirmPassword,
      nickname
    });
    return response.data;
  },

  async logout() {
    const token = localStorage.getItem('token');
    const response = await http.post('/api/auth/logout', {}, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  },

  setAuthHeader() {
    const token = localStorage.getItem('token');
    if (token) {
      http.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  },

  clearAuth() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete http.defaults.headers.common['Authorization'];
  },

  getToken() {
    return localStorage.getItem('token');
  },

  getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  isAuthenticated() {
    return !!this.getToken();
  }
};

export default authService;
