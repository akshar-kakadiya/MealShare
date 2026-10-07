import { Package } from 'lucide-react';
import { DashboardLayout } from '../../layouts/DashboardLayout';
import { useAuth } from '../../contexts/AuthContext';

export default function DonorDashboard() {
  const { user } = useAuth();

  return (
    <DashboardLayout>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Donor Dashboard</h1>
        <p className="text-gray-500 mt-1">Manage your food donations</p>
      </div>

      {/* User info card */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 max-w-lg">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
            <Package size={20} className="text-emerald-600" />
          </div>
          <div>
            <h2 className="font-semibold text-gray-900">Account Information</h2>
            <p className="text-xs text-gray-500">Your profile details</p>
          </div>
        </div>

        <dl className="space-y-3 text-sm">
          <div className="flex justify-between">
            <dt className="text-gray-500">Email</dt>
            <dd className="text-gray-900 font-medium">{user?.email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-gray-500">Phone</dt>
            <dd className="text-gray-900 font-medium">{user?.phone ?? '—'}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-gray-500">Role</dt>
            <dd className="text-gray-900 font-medium">Donor</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-gray-500">Status</dt>
            <dd>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">
                {user?.status}
              </span>
            </dd>
          </div>
        </dl>
      </div>

      <div className="mt-8 bg-amber-50 border border-amber-200 rounded-xl p-6 max-w-lg">
        <p className="text-sm text-amber-800">
          🚧 Donation creation, listing management, and analytics features are coming in the next phase.
        </p>
      </div>
    </DashboardLayout>
  );
}
