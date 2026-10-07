import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AdminLayout from './layouts/AdminLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/UsersPage';
import DonationsPage from './pages/DonationsPage';
import RequestsPage from './pages/RequestsPage';
import PickupsPage from './pages/PickupsPage';
import VerificationPage from './pages/VerificationPage';
import AuditLogsPage from './pages/AuditLogsPage';
import RatingsPage from './pages/RatingsPage';
import ReportsPage from './pages/ReportsPage';
import NotificationsPage from './pages/NotificationsPage';
import SettingsPage from './pages/SettingsPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/admin"
            element={
              <ProtectedRoute>
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="donations" element={<DonationsPage />} />
            <Route path="requests" element={<RequestsPage />} />
            <Route path="pickups" element={<PickupsPage />} />
            <Route path="verification" element={<VerificationPage />} />
            <Route path="audit-logs" element={<AuditLogsPage />} />
            <Route path="ratings" element={<RatingsPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
