import { Navigate } from 'react-router-dom';
import { ShieldAlert, Loader2 } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { firebaseUser, mealshareUser, loading, authError, signOut } = useAuth();

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
        <Loader2 className="w-8 h-8 text-primary-600 animate-spin mb-3" />
        <p className="text-sm font-medium text-gray-600">Verifying administrator credentials...</p>
      </div>
    );
  }

  // Not authenticated with Firebase
  if (!firebaseUser) {
    return <Navigate to="/login" replace />;
  }

  // Firebase auth succeeded but backend returned an error or non-ADMIN role
  if (authError || !mealshareUser || mealshareUser.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
          <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6 text-red-600" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Access Denied</h2>
          <p className="text-sm text-gray-600 mb-4">
            {authError || 'You do not have administrator permissions on this platform.'}
          </p>
          <p className="text-xs text-gray-500 mb-6">
            Only users with the authoritative ROLE_ADMIN role in PostgreSQL can access this management portal.
          </p>
          <button
            onClick={() => signOut()}
            className="w-full bg-primary-600 text-white py-2.5 px-4 rounded-lg hover:bg-primary-700 font-medium text-sm transition"
          >
            Sign Out & Return to Login
          </button>
        </div>
      </div>
    );
  }

  // Authoritative ADMIN verified
  return <>{children}</>;
}
