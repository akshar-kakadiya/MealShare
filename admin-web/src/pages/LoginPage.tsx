import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Leaf, Lock, Mail, AlertCircle, Loader2, RefreshCw } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export default function LoginPage() {
  const navigate = useNavigate();
  const { signIn, authError, mealshareUser, firebaseUser, refreshProfile, connectionStatus } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // If already logged in as ADMIN, navigate to dashboard
  useEffect(() => {
    if (mealshareUser && mealshareUser.role === 'ADMIN') {
      navigate('/admin', { replace: true });
    }
  }, [mealshareUser, navigate]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSubmitting(true);

    try {
      await signIn(email.trim(), password);
      navigate('/admin', { replace: true });
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Authentication failed. Please check credentials.';
      setErrorMessage(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const isWakingUpError =
    (errorMessage && (errorMessage.includes('waking up') || errorMessage.includes('unavailable') || errorMessage.includes('timed out'))) ||
    (authError && (authError.includes('waking up') || authError.includes('unavailable') || authError.includes('timed out')));

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Left Panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-primary-600 flex-col justify-center items-center p-12">
        <div className="max-w-md text-center">
          <div className="w-16 h-16 bg-white/10 rounded-2xl flex items-center justify-center mb-6 mx-auto">
            <Leaf className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-3">MealShare</h1>
          <p className="text-primary-100 text-lg mb-8">Connecting surplus food with those who need it most</p>
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 text-left border border-white/10 space-y-3">
            <div className="flex items-center gap-2 text-white font-medium text-sm">
              <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
              Secure Production Administration
            </div>
            <p className="text-xs text-primary-100 leading-relaxed">
              Authorized platform administrator access only. All actions are cryptographically authenticated via Firebase ID tokens and recorded in the immutable PostgreSQL audit log.
            </p>
          </div>
        </div>
      </div>

      {/* Right Panel */}
      <div className="flex-1 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-12">
        <div className="sm:mx-auto sm:w-full sm:max-w-md">
          <div className="flex items-center justify-center gap-2 mb-8 lg:hidden">
            <div className="w-10 h-10 bg-primary-600 rounded-xl flex items-center justify-center">
              <Leaf className="w-5 h-5 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">MealShare</span>
          </div>

          <h2 className="text-2xl font-bold text-gray-900 text-center lg:text-left">Admin Portal</h2>
          <p className="text-sm text-gray-500 mt-1 text-center lg:text-left">
            Sign in with your verified administrator credentials
          </p>

          <div className="mt-8 bg-white border border-gray-100 rounded-xl p-6 sm:p-8 shadow-sm">
            {(errorMessage || authError) && (
              <div className="mb-5 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-start gap-2.5">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <span className="text-xs sm:text-sm">{errorMessage || authError}</span>
                  {(isWakingUpError || firebaseUser) && !submitting && (
                    <button
                      type="button"
                      onClick={async () => {
                        setErrorMessage(null);
                        setSubmitting(true);
                        try {
                          const user = await refreshProfile();
                          if (user && user.role === 'ADMIN') {
                            navigate('/admin', { replace: true });
                          }
                        } catch (err: any) {
                          setErrorMessage(err.message || 'Verification failed. Please retry.');
                        } finally {
                          setSubmitting(false);
                        }
                      }}
                      className="block mt-1.5 text-xs font-semibold text-primary-700 underline hover:text-primary-800 cursor-pointer"
                    >
                      Retry Verification
                    </button>
                  )}
                </div>
              </div>
            )}

            {submitting && connectionStatus && connectionStatus.attempt > 1 && (
              <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800 flex items-center gap-2">
                <RefreshCw className="w-4 h-4 text-amber-600 animate-spin flex-shrink-0" />
                <span>{connectionStatus.message}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Administrator Email
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="admin@mealshare.org"
                    className="w-full pl-9 pr-3 py-2.5 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className="w-full pl-9 pr-3 py-2.5 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="w-full py-2.5 bg-primary-600 text-white text-sm font-semibold rounded-lg hover:bg-primary-700 transition flex items-center justify-center gap-2 disabled:opacity-60 cursor-pointer"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>
                      {connectionStatus?.attempt && connectionStatus.attempt > 1
                        ? `Connecting (attempt ${connectionStatus.attempt})...`
                        : 'Verifying credentials...'}
                    </span>
                  </>
                ) : (
                  <span>Sign in to Dashboard</span>
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
