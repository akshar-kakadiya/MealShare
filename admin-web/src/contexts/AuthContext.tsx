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

  const fetchMealShareProfile = async (): Promise<MealShareUser> => {
    try {
      const response = await apiClient.get('/users/me');
      const profile: MealShareUser = response.data;

      if (profile.role !== 'ADMIN') {
        const errorMsg = 'Access denied. Only ADMIN users can access this portal.';
        setAuthError(errorMsg);
        setMealshareUser(null);
        await firebaseSignOut(auth);
        throw new Error(errorMsg);
      }

      if (profile.status === 'SUSPENDED') {
        const errorMsg = 'Access denied. Your administrator account has been suspended.';
        setAuthError(errorMsg);
        setMealshareUser(null);
        await firebaseSignOut(auth);
        throw new Error(errorMsg);
      }

      setMealshareUser(profile);
      setAuthError(null);
      return profile;
    } catch (err: any) {
      const message =
        err.response?.data?.message || err.message || 'Failed to verify admin access';
      setAuthError(message);
      setMealshareUser(null);
      throw new Error(message);
    }
  };

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setFirebaseUser(user);

      if (user) {
        setLoading(true);
        try {
          await fetchMealShareProfile();
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
    return await fetchMealShareProfile();
  };

  const signOut = async () => {
    setMealshareUser(null);
    setAuthError(null);
    await firebaseSignOut(auth);
  };

  const refreshProfile = async () => {
    if (auth.currentUser) {
      return await fetchMealShareProfile();
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
