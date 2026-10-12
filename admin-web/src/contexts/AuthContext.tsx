import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import {
  type User,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged,
} from 'firebase/auth';
import { auth } from '../config/firebase';
import apiClient from '../api/client';

export interface MealShareUser {
  id: string;
  firebaseUid: string;
  email: string;
  name: string;
  role: string;
  status: string;
}

interface AuthContextType {
  firebaseUser: User | null;
  mealshareUser: MealShareUser | null;
  loading: boolean;
  authError: string | null;
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

  const fetchMealShareProfile = async (isBackgroundCheck = false): Promise<MealShareUser> => {
    try {
      const response = await apiClient.get('/users/me', { timeout: 15000 });
      const profile: MealShareUser = response.data;

      if (profile.role !== 'ADMIN') {
        const errorMsg = 'Access denied. Only ADMIN users can access this portal.';
        setMealshareUser(null);
        await firebaseSignOut(auth).catch(() => {});
        if (!isBackgroundCheck) {
          setAuthError(errorMsg);
        }
        throw new Error(errorMsg);
      }

      if (profile.status === 'SUSPENDED') {
        const errorMsg = 'Access denied. Your administrator account has been suspended.';
        setMealshareUser(null);
        await firebaseSignOut(auth).catch(() => {});
        if (!isBackgroundCheck) {
          setAuthError(errorMsg);
        }
        throw new Error(errorMsg);
      }

      setMealshareUser(profile);
      setAuthError(null);
      return profile;
    } catch (err: any) {
      const status = err.response?.status;
      let message =
        err.response?.data?.message || err.message || 'Failed to verify admin access';

      if (status === 404) {
        message = 'No administrator profile found for this account.';
        await firebaseSignOut(auth).catch(() => {});
      } else if (status === 401) {
        message = 'Your session has expired. Please sign in again.';
        await firebaseSignOut(auth).catch(() => {});
      } else if (status === 403) {
        message = 'Access denied. You do not have administrator permissions.';
        await firebaseSignOut(auth).catch(() => {});
      } else if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
        message = 'Backend verification timed out. The server may be waking up. Please retry.';
      } else if (!err.response) {
        message = 'Unable to connect to the MealShare backend.';
      }

      setMealshareUser(null);

      // On initial background check on page load, if the session is invalid or not registered in DB
      // (e.g. 404, 401, 403), do not display a misleading error banner on the login screen.
      if (isBackgroundCheck && (status === 404 || status === 401 || status === 403)) {
        setAuthError(null);
      } else {
        setAuthError(message);
      }

      throw new Error(message);
    }
  };

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setFirebaseUser(user);

      if (user) {
        setLoading(true);
        try {
          await fetchMealShareProfile(true);
        } catch {
          // Handled inside fetchMealShareProfile
        } finally {
          setLoading(false);
        }
      } else {
        setMealshareUser(null);
        setAuthError(null);
        setLoading(false);
      }
    });
    return unsubscribe;
  }, []);

  const signIn = async (email: string, password: string): Promise<MealShareUser> => {
    setAuthError(null);
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    setFirebaseUser(userCredential.user);
    return await fetchMealShareProfile(false);
  };

  const signOut = async () => {
    setMealshareUser(null);
    setAuthError(null);
    await firebaseSignOut(auth);
  };

  const refreshProfile = async (): Promise<MealShareUser | null> => {
    if (auth.currentUser) {
      setLoading(true);
      setAuthError(null);
      try {
        return await fetchMealShareProfile(false);
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
