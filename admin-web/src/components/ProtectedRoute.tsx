import { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { ShieldAlert, AlertCircle, Loader2 } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { firebaseUser, mealshareUser, loading, authError, signOut, refreshProfile } = useAuth();
  const [takingLonger, setTakingLonger] = useState(false);

  useEffect(() => {
    if (!loading) {
      setTakingLonger(false);
      return;
    }
    const timer = setTimeout(() => {
      setTakingLonger(true);
    }, 7000);
    return () => clearTimeout(timer);
  }, [loading]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-4">
        <Loader2 className="w-8 h-8 text-primary-600 animate-spin mb-3" />
        <p className="text-sm font-medium text-gray-700">Verifying administrator credentials...</p>
        {takingLonger && (
          <div className="mt-4 max-w-sm text-center">
            <p className="text-xs text-gray-500 mb-4">
              Verification is taking longer than expected. The server may be waking up from sleep.
            </p>
            <div className="flex gap-2 justify-center">
              <button
                type="button"
                onClick={() => refreshProfile()}
                className="px-3 py-1.5 bg-primary-600 text-white rounded text-xs font-medium hover:bg-primary-700 transition"
              >
                Retry
              </button>
              <button
                type="button"
                onClick={() => signOut()}
                className="px-3 py-1.5 border border-gray-300 text-gray-700 rounded text-xs font-medium hover:bg-gray-100 transition"
              >
                Return to Login
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // Not authenticated with Firebase
  if (!firebaseUser) {
    return <Navigate to="/" replace />;
  }

  // Firebase auth succeeded but backend returned an error or non-ADMIN role
  if (authError || !mealshareUser || mealshareUser.role !== 'ADMIN') {
    const isAccessDenied = mealshareUser && mealshareUser.role !== 'ADMIN';

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
          <div
            className={`w-12 h-12 rounded-full ${
              isAccessDenied ? 'bg-red-100' : 'bg-amber-100'
            } flex items-center justify-center mx-auto mb-4`}
          >
            {isAccessDenied ? (
              <ShieldAlert className="w-6 h-6 text-red-600" />
            ) : (
              <AlertCircle className="w-6 h-6 text-amber-600" />
            )}
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            {isAccessDenied ? 'Access Denied' : 'Verification Issue'}
          </h2>
          <p className="text-sm text-gray-600 mb-4">
            {authError ||
              (isAccessDenied
                ? 'You do not have administrator permissions on this platform.'
                : 'Unable to verify administrator status with backend.')}
          </p>
          <p className="text-xs text-gray-500 mb-6">
            {isAccessDenied
              ? 'Only users with the authoritative ROLE_ADMIN role in PostgreSQL can access this management portal.'
              : 'Please check your connection or retry verification.'}
          </p>
          <div className="space-y-2">
            {!isAccessDenied && (
              <button
                type="button"
                onClick={() => refreshProfile()}
                className="w-full bg-primary-600 text-white py-2.5 px-4 rounded-lg hover:bg-primary-700 font-medium text-sm transition"
              >
                Retry Verification
              </button>
            )}
            <button
              type="button"
              onClick={() => signOut()}
              className={`w-full ${
                isAccessDenied
                  ? 'bg-primary-600 text-white hover:bg-primary-700'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              } py-2.5 px-4 rounded-lg font-medium text-sm transition`}
            >
              Sign Out & Return to Login
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Authoritative ADMIN verified
  return <>{children}</>;
}
