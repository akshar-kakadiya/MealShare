import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AuthLayout } from '../../layouts/AuthLayout';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Alert } from '../../components/ui/Alert';
import { useAuth } from '../../contexts/AuthContext';
import { extractErrorMessage } from '../../utils/error';
import { getRoleDashboardPath } from '../../utils/role';
import type { RegisterableRole } from '../../types/auth.types';

interface FieldErrors {
  name?: string;
  email?: string;
  phone?: string;
  password?: string;
  role?: string;
}

const ROLE_OPTIONS: { value: RegisterableRole; label: string }[] = [
  { value: 'Donor', label: 'Donor — Share surplus food' },
  { value: 'Receiver_Org', label: 'Receiver Organization — Collect food for beneficiaries' },
  { value: 'Receiver_Vol', label: 'Volunteer — Help deliver food' },
];

export default function Register() {
  const { register, isAuthenticated, user } = useAuth();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<RegisterableRole | ''>('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [pendingVerification, setPendingVerification] = useState(false);

  // If already authenticated, redirect to dashboard
  if (isAuthenticated && user) {
    return <Navigate to={getRoleDashboardPath(user.role)} replace />;
  }

  function validate(): boolean {
    const errs: FieldErrors = {};

    if (!name.trim()) errs.name = 'Name is required';
    if (!email.trim()) {
      errs.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errs.email = 'Please enter a valid email address';
    }
    if (!phone.trim()) {
      errs.phone = 'Phone number is required';
    } else if (!/^\+[1-9]\d{1,14}$/.test(phone.trim())) {
      errs.phone = 'Phone must be in E.164 format (e.g. +919876543210)';
    }
    if (!password) {
      errs.password = 'Password is required';
    } else if (password.length < 8) {
      errs.password = 'Password must be at least 8 characters';
    } else if (!/(?=.*[a-z])/.test(password)) {
      errs.password = 'Password must contain at least one lowercase letter';
    } else if (!/(?=.*[A-Z])/.test(password)) {
      errs.password = 'Password must contain at least one uppercase letter';
    } else if (!/(?=.*[\d\W])/.test(password)) {
      errs.password = 'Password must contain at least one number or special character';
    }
    if (!role) errs.role = 'Please select a role';

    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(evt: FormEvent) {
    evt.preventDefault();
    setApiError('');
    if (!validate()) return;

    setIsLoading(true);
    try {
      const result = await register({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        password,
        role: role as RegisterableRole,
      });

      setSuccess(true);
      if (result.status === 'Pending_Verification') {
        setPendingVerification(true);
      }
    } catch (err) {
      setApiError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  // Success state
  if (success) {
    return (
      <AuthLayout title="Registration successful!" subtitle="Your account has been created">
        <div className="space-y-4">
          {pendingVerification ? (
            <Alert type="info">
              Your organization account is pending verification. You will be able to access full
              features once an administrator reviews your account.
            </Alert>
          ) : (
            <Alert type="success">
              Your account is active and ready to use.
            </Alert>
          )}
          <Link
            to="/login"
            className="block w-full text-center py-2.5 bg-emerald-600 text-white font-medium rounded-lg hover:bg-emerald-700 transition-colors"
          >
            Continue to Login
          </Link>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Create account" subtitle="Join MealShare and help reduce food waste">
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {apiError && <Alert type="error">{apiError}</Alert>}

        <Input
          label="Full Name"
          placeholder="Your name or organization name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={fieldErrors.name}
          disabled={isLoading}
        />

        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
          autoComplete="email"
          disabled={isLoading}
        />

        <Input
          label="Phone Number"
          type="tel"
          placeholder="+919876543210"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          error={fieldErrors.phone}
          autoComplete="tel"
          disabled={isLoading}
        />

        <Input
          label="Password"
          isPassword
          placeholder="Min 8 characters, upper+lower+number/special"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          autoComplete="new-password"
          disabled={isLoading}
        />

        {/* Role select */}
        <div className="w-full">
          <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as RegisterableRole | '')}
            className={`
              w-full px-3 py-2.5 rounded-lg border bg-white text-gray-900
              transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500
              disabled:bg-gray-50 disabled:text-gray-500
              ${fieldErrors.role ? 'border-red-400' : 'border-gray-300'}
            `}
            disabled={isLoading}
          >
            <option value="">Select your role…</option>
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
          {fieldErrors.role && <p className="mt-1 text-sm text-red-600">{fieldErrors.role}</p>}
        </div>

        <Button type="submit" fullWidth isLoading={isLoading}>
          Create Account
        </Button>
      </form>

      <p className="text-center text-sm text-gray-500 mt-6">
        Already have an account?{' '}
        <Link to="/login" className="text-emerald-600 font-medium hover:text-emerald-700">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  );
}
