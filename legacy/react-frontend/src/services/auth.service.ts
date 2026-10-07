import api from '../api/client';
import { tokenService } from './token.service';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  User,
} from '../types/auth.types';

export const authService = {
  /** POST /auth/login — stores tokens on success */
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', data);
    const result = response.data;

    tokenService.setAccessToken(result.accessToken);
    tokenService.setRefreshToken(result.refreshToken);

    return result;
  },

  /** POST /auth/register — does NOT store tokens (backend returns no tokens) */
  async register(data: RegisterRequest): Promise<RegisterResponse> {
    const response = await api.post<RegisterResponse>('/auth/register', data);
    return response.data;
  },

  /** GET /users/me — returns current authenticated user's profile */
  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/users/me');
    return response.data;
  },

  /** POST /auth/logout — backend reads userId from JWT, no body needed */
  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      tokenService.clearTokens();
    }
  },
};