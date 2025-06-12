import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

const apiClient = axios.create();

apiClient.interceptors.request.use(
  config => {
    const authStore = useAuthStore();
    const token = authStore.getToken || localStorage.getItem('jwtToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Added Authorization header with token for request:', config.url);
    }
    return config;
  },
  error => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore();
      console.warn('401 Unauthorized, logging out');
      authStore.logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
