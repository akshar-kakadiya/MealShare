// ---- Enums as union types (erasableSyntaxOnly) ----

export type UserRole = 'Admin' | 'Donor' | 'Receiver_Org' | 'Receiver_Vol';

export type RegisterableRole = 'Donor' | 'Receiver_Org' | 'Receiver_Vol';

export type UserStatus = 'Active' | 'Pending_Verification' | 'Restricted' | 'Suspended';

// ---- Domain models ----

export interface User {
  id: string;
  email: string;
  phone: string;
  role: UserRole;
  status: UserStatus;
  createdAt?: string;
  donorProfile?: Record<string, unknown> | null;
  receiverOrg?: Record<string, unknown> | null;
  volunteerProfile?: Record<string, unknown> | null;
}

// ---- Request DTOs ----

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  phone: string;
  password: string;
  name: string;
  role: RegisterableRole;
}

// ---- Response DTOs (unwrapped from envelope) ----

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    phone: string;
    role: UserRole;
    status: UserStatus;
  };
}

export interface RegisterResponse {
  userId: string;
  email: string;
  phone: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
}

export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

// ---- API envelope types ----

export interface ApiEnvelopeSuccess<T> {
  success: true;
  data: T;
  meta: { requestId: string; timestamp: string };
}

export interface ApiEnvelopeError {
  success: false;
  error: {
    code: string;
    message: string;
    details?: string[] | unknown;
  };
  meta: { requestId: string; timestamp: string };
}
