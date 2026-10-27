import { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { ShieldAlert, AlertCircle, Loader2, RefreshCw } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { firebaseUser, mealshareUser, loading, authError, connectionStatus, signOut, refreshProfile } = useAuth();
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
    const isWakingUp = connectionStatus && connectionStatus.attempt > 1;

    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-4">
        <Loader2 className="w-8 h-8 text-primary-600 animate-spin mb-3" />
        <p className="text-sm font-medium text-gray-800">
          {connectionStatus?.message || 'Verifying administrator credentials...'}
        </p>
        {isWakingUp ? (
          <div className="mt-3 max-w-sm text-center">
            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-800 mb-2">
              Server Waking Up
            </span>
            <p className="text-xs text-gray-500">
              The MealShare backend is spinning up from idle state. The portal will continue automatically as soon as it responds.
            </p>
          </div>
        ) : takingLonger ? (
          <div className="mt-4 max-w-sm text-center">
            <p className="text-xs text-gray-500 mb-4">
              Connecting to backend is taking longer than expected.
            </p>
            <div className="flex gap-2 justify-center">
              <button
                type="button"
                onClick={() => refreshProfile()}
                className="px-3 py-1.5 bg-primary-600 text-white rounded text-xs font-medium hover:bg-primary-700 transition inline-flex items-center gap-1.5"
              >
                <RefreshCw className="w-3.5 h-3.5" />
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
        ) : null}
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
    const isBackendUnavailable = authError && (authError.includes('waking up') || authError.includes('unavailable') || authError.includes('unreachable'));
    const isNotFound = authError && authError.includes('No administrator profile');

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
          <div
            className={`w-12 h-12 rounded-full ${
              isAccessDenied ? 'bg-red-100' : isBackendUnavailable ? 'bg-amber-100' : 'bg-orange-100'
            } flex items-center justify-center mx-auto mb-4`}
          >
            {isAccessDenied ? (
              <ShieldAlert className="w-6 h-6 text-red-600" />
            ) : (
              <AlertCircle className={`w-6 h-6 ${isBackendUnavailable ? 'text-amber-600' : 'text-orange-600'}`} />
            )}
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            {isAccessDenied
              ? 'Access Denied'
              : isBackendUnavailable
              ? 'Backend Unreachable'
              : isNotFound
              ? 'Profile Not Found'
              : 'Verification Issue'}
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
              : isBackendUnavailable
              ? 'The server may still be cold-starting or experiencing network delays. You can retry the connection below.'
              : 'Please check your connection or retry verification.'}
          </p>
          <div className="space-y-2">
            {!isAccessDenied && (
              <button
                type="button"
                onClick={() => refreshProfile()}
                className="w-full bg-primary-600 text-white py-2.5 px-4 rounded-lg hover:bg-primary-700 font-medium text-sm transition inline-flex items-center justify-center gap-1.5"
              >
                <RefreshCw className="w-4 h-4" />
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
