import { createContext, useContext, useEffect, useState, useRef, type ReactNode } from 'react';
import {
  type User,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged,
} from 'firebase/auth';
import { auth } from '../config/firebase';
import apiClient, { executeWithRetry, isTransientError } from '../api/client';

export interface MealShareUser {
  id: string;
  firebaseUid: string;
  email: string;
  name: string;
  role: string;
  status: string;
}

export interface ConnectionStatus {
  isConnecting: boolean;
  attempt: number;
  maxAttempts: number;
  message: string;
}

interface AuthContextType {
  firebaseUser: User | null;
  mealshareUser: MealShareUser | null;
  loading: boolean;
  authError: string | null;
  connectionStatus: ConnectionStatus | null;
  signIn: (email: string, password: string) => Promise<MealShareUser>;
  signOut: () => Promise<void>;
  refreshProfile: () => Promise<MealShareUser | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [mealshareUser, setMealshareUser] = useState<MealShareUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus | null>(null);

  const inFlightPromiseRef = useRef<{
    promise: Promise<MealShareUser>;
    uid: string;
    requestId: number;
  } | null>(null);

  const latestRequestIdRef = useRef<number>(0);

  const fetchMealShareProfile = async (
    isBackgroundCheck = false,
    userOverride?: User | null
  ): Promise<MealShareUser> => {
    const targetUser = userOverride ?? auth.currentUser;
    if (!targetUser) {
      setMealshareUser(null);
      setLoading(false);
      throw new Error('No authenticated user found.');
    }

    // Join in-flight request if one is already running for the exact same Firebase user
    if (inFlightPromiseRef.current && inFlightPromiseRef.current.uid === targetUser.uid) {
      try {
        return await inFlightPromiseRef.current.promise;
      } catch (err: any) {
        if (!isBackgroundCheck && err.message) {
          setAuthError(err.message);
        }
        throw err;
      }
    }

    const requestId = ++latestRequestIdRef.current;

    const executeFetch = async (): Promise<MealShareUser> => {
      if (requestId === latestRequestIdRef.current) {
        setConnectionStatus({
          isConnecting: true,
          attempt: 1,
          maxAttempts: 4,
          message: 'Connecting to MealShare backend...',
        });
      }

      try {
        // Ensure token is acquired before sending protected request
        const token = await targetUser.getIdToken();
        if (!token) {
          throw new Error('Unable to acquire authentication token.');
        }

        const response = await executeWithRetry(
          () =>
            apiClient.get('/users/me', {
              headers: { Authorization: `Bearer ${token}` },
              timeout: 20000,
            }),
          {
            maxRetries: 3,
            initialDelayMs: 2000,
            backoffFactor: 1.5,
            onRetry: (attempt, maxRetries, delayMs) => {
              if (requestId !== latestRequestIdRef.current) return;
              const nextAttempt = attempt + 1;
              const total = maxRetries + 1;
              setConnectionStatus({
                isConnecting: true,
                attempt: nextAttempt,
                maxAttempts: total,
                message: `Server may be waking up. Reconnecting in ${(delayMs / 1000).toFixed(1)}s (Attempt ${nextAttempt} of ${total})...`,
              });
            },
          }
        );

        const profile: MealShareUser = response.data;

        if (profile.role !== 'ADMIN') {
          const errorMsg = 'Access denied. Only ADMIN users can access this portal.';
          if (requestId === latestRequestIdRef.current) {
            setMealshareUser(null);
            setConnectionStatus(null);
            setAuthError(errorMsg);
          }
          await firebaseSignOut(auth).catch(() => {});
          const err = new Error(errorMsg);
          (err as any).isRoleError = true;
          throw err;
        }

        if (profile.status === 'SUSPENDED') {
          const errorMsg = 'Access denied. Your administrator account has been suspended.';
          if (requestId === latestRequestIdRef.current) {
            setMealshareUser(null);
            setConnectionStatus(null);
            setAuthError(errorMsg);
          }
          await firebaseSignOut(auth).catch(() => {});
          const err = new Error(errorMsg);
          (err as any).isRoleError = true;
          throw err;
        }

        // Only commit state if this request is still the most recent request
        if (requestId === latestRequestIdRef.current) {
          setMealshareUser(profile);
          setAuthError(null);
          setConnectionStatus(null);
        }
        return profile;
      } catch (err: any) {
        if (err.isRoleError) {
          throw err;
        }
        if (requestId === latestRequestIdRef.current) {
          setConnectionStatus(null);
          setMealshareUser(null);
        }

        const status = err.response?.status;
        let message =
          err.response?.data?.message || err.message || 'Failed to verify admin access';

        if (status === 404) {
          // True 404: Account exists in Firebase Auth but has no row in PostgreSQL
          message = 'No administrator profile found for this account. Please verify provisioned admin access.';
          // Do not call firebaseSignOut so that the user can retry or re-verify
        } else if (status === 401) {
          message = 'Authentication verification failed or session expired. Please retry or sign in again.';
        } else if (status === 403) {
          message = 'Access denied. You do not have administrator permissions.';
        } else if (isTransientError(err)) {
          message = 'The MealShare backend is waking up or temporarily unavailable. Please retry in a few moments.';
        }

        if (requestId === latestRequestIdRef.current) {
          if (isBackgroundCheck && (status === 404 || status === 401 || status === 403)) {
            setAuthError(null);
          } else {
            setAuthError(message);
          }
        }

        throw new Error(message);
      } finally {
        if (inFlightPromiseRef.current?.requestId === requestId) {
          inFlightPromiseRef.current = null;
        }
      }
    };

    const promise = executeFetch();
    inFlightPromiseRef.current = { promise, uid: targetUser.uid, requestId };
    return promise;
  };

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setFirebaseUser(user);

      if (user) {
        setLoading(true);
        try {
          await fetchMealShareProfile(true, user);
        } catch {
          // Handled inside fetchMealShareProfile
        } finally {
          setLoading(false);
        }
      } else {
        inFlightPromiseRef.current = null;
        latestRequestIdRef.current++;
        setMealshareUser(null);
        setAuthError(null);
        setConnectionStatus(null);
        setLoading(false);
      }
    });
    return unsubscribe;
  }, []);

  const signIn = async (email: string, password: string): Promise<MealShareUser> => {
    setAuthError(null);
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      setFirebaseUser(userCredential.user);
      return await fetchMealShareProfile(false, userCredential.user);
    } catch (err: any) {
      let friendlyMessage = err.message || 'Authentication failed. Please check credentials.';
      if (
        err.code === 'auth/invalid-credential' ||
        err.code === 'auth/wrong-password' ||
        err.code === 'auth/user-not-found'
      ) {
        friendlyMessage = 'Invalid administrator email or password.';
      } else if (err.code === 'auth/invalid-email') {
        friendlyMessage = 'Please enter a valid email address.';
      } else if (err.code === 'auth/user-disabled') {
        friendlyMessage = 'This account has been disabled. Please contact system administration.';
      } else if (err.code === 'auth/too-many-requests') {
        friendlyMessage = 'Access temporarily blocked due to many failed login attempts. Please try again later.';
      } else if (err.code === 'auth/network-request-failed') {
        friendlyMessage = 'Network error connecting to Firebase Authentication. Please check your connection.';
      }
      setAuthError(friendlyMessage);
      throw new Error(friendlyMessage);
    }
  };

  const signOut = async () => {
    inFlightPromiseRef.current = null;
    latestRequestIdRef.current++;
    setMealshareUser(null);
    setAuthError(null);
    setConnectionStatus(null);
    await firebaseSignOut(auth).catch(() => {});
  };

  const refreshProfile = async (): Promise<MealShareUser | null> => {
    const user = auth.currentUser;
    if (user) {
      setLoading(true);
      setAuthError(null);
      try {
        return await fetchMealShareProfile(false, user);
      } catch {
        return null;
      } finally {
        setLoading(false);
      }
    }
    return null;
  };

  return (
    <AuthContext.Provider
      value={{
        firebaseUser,
        mealshareUser,
        loading,
        authError,
        connectionStatus,
        signIn,
        signOut,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
