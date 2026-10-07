import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getRoleDashboardPath } from '../utils/role';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';

// Pages
import Login from '../pages/auth/Login';
import Register from '../pages/auth/Register';
import DonorDashboard from '../pages/donor/DonorDashboard';
import ReceiverDashboard from '../pages/receiver/ReceiverDashboard';
import VolunteerDashboard from '../pages/volunteer/VolunteerDashboard';
import AdminDashboard from '../pages/admin/AdminDashboard';

/** Redirect root to dashboard (if authenticated) or login */
function RootRedirect() {
  const { user, isAuthenticated } = useAuth();

  if (isAuthenticated && user) {
    return <Navigate to={getRoleDashboardPath(user.role)} replace />;
  }

  return <Navigate to="/login" replace />;
}

export function AppRouter() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Protected routes — require authentication */}
      <Route element={<ProtectedRoute />}>
        {/* Donor */}
        <Route element={<RoleRoute allowedRoles={['Donor']} />}>
          <Route path="/donor" element={<DonorDashboard />} />
        </Route>

        {/* Receiver Organization */}
        <Route element={<RoleRoute allowedRoles={['Receiver_Org']} />}>
          <Route path="/receiver" element={<ReceiverDashboard />} />
        </Route>

        {/* Volunteer */}
        <Route element={<RoleRoute allowedRoles={['Receiver_Vol']} />}>
          <Route path="/volunteer" element={<VolunteerDashboard />} />
        </Route>

        {/* Admin */}
        <Route element={<RoleRoute allowedRoles={['Admin']} />}>
          <Route path="/admin" element={<AdminDashboard />} />
        </Route>
      </Route>

      {/* Root redirect */}
      <Route path="/" element={<RootRedirect />} />

      {/* Catch-all: redirect to root */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
