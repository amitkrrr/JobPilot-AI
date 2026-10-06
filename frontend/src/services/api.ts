import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

// Create a custom axios instance
const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Configure a request interceptor to automatically append the Bearer JWT token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Configure a response interceptor to handle authorization expiration (401 Unauthorized errors)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If the server returns a 401 Unauthorized, we have an expired or invalid session token
    if (error.response && error.response.status === 401) {
      // Clear token and logout
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default api;
