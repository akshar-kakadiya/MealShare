import axios from 'axios';
import { auth } from '../config/firebase';

const resolveApiBaseUrl = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl && envUrl.trim() !== '') {
    const trimmed = envUrl.trim();
    // Safety check: if running in browser outside localhost but configured with localhost URL, use cloud backend
    const isBrowserOnLocalhost =
      typeof window !== 'undefined' &&
      (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');
    if (!isBrowserOnLocalhost && (trimmed.includes('localhost') || trimmed.includes('127.0.0.1'))) {
      return 'https://mealshare-backend.onrender.com/api/v1';
    }
    return trimmed.endsWith('/api/v1') ? trimmed : `${trimmed.replace(/\/+$/, '')}/api/v1`;
  }

  if (import.meta.env.PROD) {
    return 'https://mealshare-backend.onrender.com/api/v1';
  }

  return 'http://localhost:8081/api/v1';
};

export const API_BASE_URL = resolveApiBaseUrl();

export const getBackendAssetUrl = (path: string | null | undefined): string => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const backendHost = API_BASE_URL.replace(/\/api\/v1\/?$/, '');
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  return `${backendHost}${cleanPath}`;
};

export const isTransientError = (error: any): boolean => {
  if (!error) return false;
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return (
        error.code === 'ERR_NETWORK' ||
        error.code === 'ECONNABORTED' ||
        error.code === 'ETIMEDOUT' ||
        Boolean(error.request)
      );
    }
    const status = error.response.status;
    return status === 408 || status === 502 || status === 503 || status === 504;
  }
  if (error.code === 'ECONNABORTED' || error.message?.includes('timeout') || error.message?.includes('Network Error')) {
    return true;
  }
  return false;
};

export interface RetryOptions {
  maxRetries?: number;
  initialDelayMs?: number;
  backoffFactor?: number;
  onRetry?: (attempt: number, maxRetries: number, delayMs: number, error: any) => void;
}

export async function executeWithRetry<T>(
  requestFn: () => Promise<T>,
  options: RetryOptions = {}
): Promise<T> {
  const maxRetries = options.maxRetries ?? 3;
  const initialDelay = options.initialDelayMs ?? 1500;
  const factor = options.backoffFactor ?? 2;

  let attempt = 0;
  while (true) {
    try {
      return await requestFn();
    } catch (err: any) {
      attempt++;
      if (attempt > maxRetries || !isTransientError(err)) {
        throw err;
      }
      const delayMs = initialDelay * Math.pow(factor, attempt - 1);
      if (options.onRetry) {
        options.onRetry(attempt, maxRetries, delayMs, err);
      }
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 25000,
});

// Attach fresh Firebase ID token to every request
apiClient.interceptors.request.use(async (config) => {
  if (config.headers?.Authorization) {
    return config;
  }
  const user = auth.currentUser;
  if (user) {
    try {
      const token = await user.getIdToken();
      config.headers.Authorization = `Bearer ${token}`;
    } catch (err) {
      console.error('Failed to get Firebase ID token:', err);
      return Promise.reject(err);
    }
  }
  return config;
});

// Handle responses and format errors distinctly
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      error.message = 'Connection timed out. The server may be waking up. Please retry.';
    } else if (!error.response) {
      error.message = 'Unable to connect to the MealShare backend. The server may be waking up or offline.';
    } else if (error.response.status === 401) {
      error.message = 'Your session has expired. Please sign in again.';
    } else if (error.response.status === 403) {
      error.message = 'Access denied. You do not have permission to access this resource.';
    } else if (error.response.status === 404) {
      if (error.config?.url?.includes('/users/me')) {
        error.message = 'No administrator profile found for this account.';
      } else {
        error.message = error.response.data?.message || 'Requested resource not found.';
      }
    } else if (error.response.status === 502 || error.response.status === 503 || error.response.status === 504) {
      error.message = 'The MealShare backend is currently waking up or unavailable. Please wait a moment.';
    } else if (error.response.data?.message) {
      error.message = error.response.data.message;
    }
    return Promise.reject(error);
  }
);

export default apiClient;
