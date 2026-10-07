import axios from 'axios';
import { auth } from '../config/firebase';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach fresh Firebase ID token to every request
apiClient.interceptors.request.use(async (config) => {
  const user = auth.currentUser;
  if (user) {
    try {
      const token = await user.getIdToken();
      config.headers.Authorization = `Bearer ${token}`;
    } catch (err) {
      console.error('Failed to get Firebase ID token:', err);
    }
  }
  return config;
});

// Handle responses and format errors distinctly
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      error.message = 'Unable to connect to the MealShare backend.';
    } else if (error.response.status === 401) {
      error.message = 'Your session has expired. Please sign in again.';
      if (auth.currentUser) {
        auth.signOut();
      }
    } else if (error.response.status === 403) {
      error.message = 'You do not have permission to access this resource.';
    } else if (error.response.data?.message) {
      error.message = error.response.data.message;
    }
    return Promise.reject(error);
  }
);

export default apiClient;
