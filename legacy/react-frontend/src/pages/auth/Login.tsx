import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../../layouts/AuthLayout';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { useAuth } from '../../contexts/AuthContext';
import { extractErrorMessage } from '../../utils/error';
import { getRoleDashboardPath } from '../../utils/role';

export default function Login() {
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ identifier?: string; password?: string }>({});
  const [apiError, setApiError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // If already authenticated, redirect to dashboard
  if (isAuthenticated && user) {
    return <Navigate to={getRoleDashboardPath(user.role)} replace />;
  }

  function validate(): boolean {
    const errs: typeof fieldErrors = {};
    if (!identifier.trim()) errs.identifier = 'Email or phone is required';
    if (!password) errs.password = 'Password is required';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(evt: FormEvent) {
    evt.preventDefault();
    setApiError('');
    if (!validate()) return;

    setIsLoading(true);
    try {
      const loggedInUser = await login({ identifier: identifier.trim(), password });
      navigate(getRoleDashboardPath(loggedInUser.role), { replace: true });
    } catch (err) {
      setApiError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your MealShare account">
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {apiError && <Alert type="error">{apiError}</Alert>}

        <Input
          label="Email or Phone"
          placeholder="you@example.com or +919876543210"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          error={fieldErrors.identifier}
          autoComplete="username"
          disabled={isLoading}
        />

        <Input
          label="Password"
          isPassword
          placeholder="Enter your password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          autoComplete="current-password"
          disabled={isLoading}
        />

        <Button type="submit" fullWidth isLoading={isLoading}>
          Sign In
        </Button>
      </form>

      <p className="text-center text-sm text-gray-500 mt-6">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="text-emerald-600 font-medium hover:text-emerald-700">
          Create one
        </Link>
      </p>
    </AuthLayout>
  );
}
