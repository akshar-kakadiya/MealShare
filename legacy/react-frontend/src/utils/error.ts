import axios from 'axios';
import type { ApiEnvelopeError } from '../types/auth.types';

/**
 * Extract a user-friendly error message from any thrown value.
 *
 * Handles:
 *  1. Backend envelope   { success:false, error:{ message } }
 *  2. Raw NestJS         { statusCode, message }
 *  3. Validation arrays  message: string[]
 *  4. Network errors
 */
export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response) {
    const data = error.response.data as ApiEnvelopeError | Record<string, unknown> | undefined;

    if (data && typeof data === 'object') {
      // Envelope format: { success: false, error: { message } }
      if ('error' in data && typeof data.error === 'object' && data.error !== null) {
        const errObj = data.error as Record<string, unknown>;
        if (typeof errObj.message === 'string') return errObj.message;
        if (Array.isArray(errObj.details)) return (errObj.details as string[]).join('. ');
      }

      // Raw NestJS format: { message }
      if ('message' in data) {
        const msg = data.message;
        if (Array.isArray(msg)) return (msg as string[]).join('. ');
        if (typeof msg === 'string') return msg;
      }
    }

    return `Server error (${error.response.status})`;
  }

  // Network / connection error
  if (axios.isAxiosError(error) && !error.response) {
    return 'Unable to connect to the server. Please check your connection and try again.';
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'An unexpected error occurred. Please try again.';
}
