import { useState, useEffect, useCallback } from 'react';
import { Search, CheckCircle, X, AlertCircle, RefreshCw, User, Shield, Phone, Mail, Building2, Calendar, Trash2, AlertTriangle } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';
import { useAuth } from '../contexts/AuthContext';

const ROLE_TABS = [
  { label: 'All Users', value: 'All' },
  { label: 'Donors', value: 'DONOR' },
  { label: 'Receivers', value: 'RECEIVER' },
  { label: 'Admins', value: 'ADMIN' },
];

interface AdminUser {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: string;
  status: string;
  verified: boolean;
  organization?: string;
  createdAt: string;
  updatedAt: string;
}

interface AdminUserDetail {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: string;
  status: string;
  verified: boolean;
  createdAt: string;
  updatedAt: string;
  profileDetails?: Record<string, any>;
}

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [search, setSearch] = useState('');
  const [activeRole, setActiveRole] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const { mealshareUser } = useAuth();

  // User detail drawer
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [detailData, setDetailData] = useState<AdminUserDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Remove user state
  const [userToRemove, setUserToRemove] = useState<AdminUser | null>(null);
  const [removeLoading, setRemoveLoading] = useState(false);
  const [removeError, setRemoveError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (activeRole !== 'All') params.role = activeRole;
      if (statusFilter !== 'All') params.status = statusFilter;
      if (search.trim()) params.search = search.trim();

      const res = await apiClient.get('/admin/users', { params });
      if (res.data && Array.isArray(res.data.content)) {
        setUsers(res.data.content);
        setTotalElements(res.data.totalElements || res.data.content.length);
        setTotalPages(res.data.totalPages || 1);
      } else {
        setUsers([]);
        setTotalElements(0);
        setTotalPages(1);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load users from backend';
      setApiError(msg);
      setUsers([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, activeRole, statusFilter, search]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Load detail when user is selected
  const handleSelectUser = async (user: AdminUser) => {
    setSelectedUser(user);
    setDetailData(null);
    setDetailLoading(true);
    setActionMessage(null);
    try {
      const res = await apiClient.get(`/admin/users/${user.id}`);
      if (res.data) {
        setDetailData(res.data);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load user profile details';
      setActionMessage({ type: 'error', text: msg });
    } finally {
      setDetailLoading(false);
    }
  };

  const handleUpdateStatus = async (newStatus: 'ACTIVE' | 'RESTRICTED' | 'SUSPENDED') => {
    if (!selectedUser) return;
    setStatusUpdating(true);
    setActionMessage(null);
    try {
      await apiClient.patch(`/admin/users/${selectedUser.id}/status`, {
        status: newStatus,
        reason: `Admin updated status to ${newStatus}`,
      });
      setActionMessage({ type: 'success', text: `User status changed to ${newStatus}` });
      setUsers((prev) => prev.map((u) => (u.id === selectedUser.id ? { ...u, status: newStatus } : u)));
      setSelectedUser((prev) => prev ? { ...prev, status: newStatus } : null);
      if (detailData) {
        setDetailData({ ...detailData, status: newStatus });
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to update user status';
      setActionMessage({ type: 'error', text: msg });
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleInitiateRemove = (user: AdminUser, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setUserToRemove(user);
    setRemoveError(null);
  };

  const handleConfirmRemove = async () => {
    if (!userToRemove) return;
    setRemoveLoading(true);
    setRemoveError(null);
    try {
      const targetEmail = userToRemove.email;
      await apiClient.delete(`/admin/users/${userToRemove.id}`);
      fetchUsers();
      if (selectedUser?.id === userToRemove.id) {
        setSelectedUser(null);
        setDetailData(null);
      }
      setUserToRemove(null);
      setActionMessage({
        type: 'success',
        text: `User ${targetEmail} has been permanently deleted from PostgreSQL and Firebase Auth.`,
      });
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to permanently delete user account';
      setRemoveError(msg);
    } finally {
      setRemoveLoading(false);
    }
  };

  const columns: Column<AdminUser>[] = [
    {
      key: 'name',
      label: 'Name',
      render: (val: string, row: AdminUser) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-bold flex-shrink-0">
            {val ? val[0].toUpperCase() : 'U'}
          </div>
          <div>
            <p className="font-medium text-gray-800">{val || 'Unnamed User'}</p>
            {row.organization && <p className="text-xs text-gray-500">{row.organization}</p>}
          </div>
        </div>
      ),
    },
    { key: 'email', label: 'Email', render: (val: string) => <span className="text-sm text-gray-600">{val}</span> },
    { key: 'phone', label: 'Phone', render: (val: string) => <span className="text-sm text-gray-600">{val || '—'}</span> },
    { key: 'role', label: 'Role', render: (val: string) => <StatusBadge status={val} /> },
    { key: 'status', label: 'Status', render: (val: string) => <StatusBadge status={val} /> },
    {
      key: 'verified',
      label: 'Active',
      render: (val: boolean) => (
        val ? <CheckCircle className="w-4 h-4 text-emerald-500" /> : <X className="w-4 h-4 text-gray-300" />
      ),
    },
    {
      key: 'createdAt',
      label: 'Joined',
      render: (val: string) => (
        <span className="text-xs text-gray-500">
          {val ? new Date(val).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      className: 'text-right',
      render: (_: any, row: AdminUser) => {
        const isSelf = row.id === mealshareUser?.id || row.email === mealshareUser?.email;

        return (
          <div className="flex justify-end items-center gap-2">
            <button
              onClick={(e) => handleInitiateRemove(row, e)}
              disabled={isSelf}
              title={
                isSelf
                  ? 'Cannot delete your own admin account'
                  : 'Permanently Delete User'
              }
              className={`p-1.5 rounded-lg text-xs font-medium inline-flex items-center gap-1 transition ${
                isSelf
                  ? 'text-gray-300 hover:text-gray-300 cursor-not-allowed'
                  : 'text-red-600 hover:bg-red-50 hover:text-red-700'
              }`}
            >
              <Trash2 className="w-4 h-4" />
              <span className="hidden sm:inline">Delete</span>
            </button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">User Directory</h1>
          <p className="text-sm text-gray-500">
            {totalElements} registered {activeRole === 'All' ? 'users' : activeRole.toLowerCase().replace(/_/g, ' ')}
          </p>
        </div>
        <button
          onClick={() => fetchUsers()}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Backend Error Banner */}
      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend error: {apiError}. User data could not be refreshed.</span>
          </div>
          <button
            onClick={() => fetchUsers()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Role Tabs */}
      <div className="flex gap-1 border-b border-gray-200 overflow-x-auto">
        {ROLE_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => {
              setActiveRole(tab.value);
              setCurrentPage(1);
            }}
            className={`px-3 py-2 text-sm font-medium border-b-2 whitespace-nowrap transition ${
              activeRole === tab.value
                ? 'border-primary-500 text-primary-700'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setCurrentPage(1);
            }}
            placeholder="Search by name, email, or org..."
            className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setCurrentPage(1);
          }}
          className="text-sm px-3 py-2 bg-white border border-gray-200 rounded-lg"
        >
          <option value="All">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="PENDING_VERIFICATION">Pending Verification</option>
          <option value="RESTRICTED">Restricted</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={users}
        emptyMessage={loading ? 'Loading users from backend...' : 'No users found matching your filters'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => handleSelectUser(row)}
      />

      {/* User Detail & Status Management Drawer */}
      {selectedUser && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelectedUser(null)} />
          <div className="relative w-full max-w-md bg-white shadow-xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="text-xs font-mono text-gray-500">{selectedUser.id}</p>
                <h2 className="text-lg font-semibold text-gray-900">{selectedUser.name || 'User Profile'}</h2>
              </div>
              <button onClick={() => setSelectedUser(null)} className="p-2 hover:bg-gray-100 rounded-lg">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {actionMessage && (
                <div
                  className={`p-3 rounded-lg text-sm flex items-center gap-2 ${
                    actionMessage.type === 'success'
                      ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                      : 'bg-red-50 text-red-800 border border-red-200'
                  }`}
                >
                  {actionMessage.type === 'success' ? (
                    <CheckCircle className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
                  )}
                  <span>{actionMessage.text}</span>
                </div>
              )}

              {/* Status and Role */}
              <div className="flex items-center gap-3">
                <StatusBadge status={selectedUser.role} size="md" />
                <StatusBadge status={selectedUser.status} size="md" />
              </div>

              {/* Core Details */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <User className="w-4 h-4 text-primary-500" /> Contact & Account
                </h4>
                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2 text-gray-700">
                    <Mail className="w-4 h-4 text-gray-400" />
                    <span>{selectedUser.email}</span>
                  </div>
                  <div className="flex items-center gap-2 text-gray-700">
                    <Phone className="w-4 h-4 text-gray-400" />
                    <span>{selectedUser.phone || 'No phone registered'}</span>
                  </div>
                  {selectedUser.organization && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <Building2 className="w-4 h-4 text-gray-400" />
                      <span>{selectedUser.organization}</span>
                    </div>
                  )}
                  <div className="flex items-center gap-2 text-gray-700">
                    <Calendar className="w-4 h-4 text-gray-400" />
                    <span>Joined {new Date(selectedUser.createdAt).toLocaleDateString('en-IN')}</span>
                  </div>
                </div>
              </div>

              {/* Profile details from database */}
              {detailLoading ? (
                <div className="p-4 text-center text-sm text-gray-400">Loading profile details...</div>
              ) : detailData?.profileDetails && Object.keys(detailData.profileDetails).length > 0 ? (
                <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                  <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                    <Shield className="w-4 h-4 text-primary-500" /> Role Profile Details
                  </h4>
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    {Object.entries(detailData.profileDetails).map(([k, v]) => (
                      <div key={k} className="p-2 bg-white rounded border border-gray-100">
                        <span className="text-gray-400 capitalize block">{k.replace(/([A-Z])/g, ' $1')}</span>
                        <span className="font-medium text-gray-800 break-words">{String(v ?? '—')}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {/* Status Management */}
              <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-800">Manage User Status</h4>
                <p className="text-xs text-gray-500">
                  Update active account standing. This changes the user's status in PostgreSQL.
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleUpdateStatus('ACTIVE')}
                    disabled={statusUpdating || selectedUser.status === 'ACTIVE'}
                    className={`flex-1 py-2 px-3 rounded-lg text-xs font-medium transition ${
                      selectedUser.status === 'ACTIVE'
                        ? 'bg-emerald-100 text-emerald-800 cursor-not-allowed'
                        : 'bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50'
                    }`}
                  >
                    Activate
                  </button>
                  <button
                    onClick={() => handleUpdateStatus('RESTRICTED')}
                    disabled={statusUpdating || selectedUser.status === 'RESTRICTED'}
                    className={`flex-1 py-2 px-3 rounded-lg text-xs font-medium transition ${
                      selectedUser.status === 'RESTRICTED'
                        ? 'bg-amber-100 text-amber-800 cursor-not-allowed'
                        : 'bg-amber-500 text-white hover:bg-amber-600 disabled:opacity-50'
                    }`}
                  >
                    Restrict
                  </button>
                  <button
                    onClick={() => handleUpdateStatus('SUSPENDED')}
                    disabled={statusUpdating || selectedUser.status === 'SUSPENDED'}
                    className={`flex-1 py-2 px-3 rounded-lg text-xs font-medium transition ${
                      selectedUser.status === 'SUSPENDED'
                        ? 'bg-red-100 text-red-800 cursor-not-allowed'
                        : 'bg-red-600 text-white hover:bg-red-700 disabled:opacity-50'
                    }`}
                  >
                    Suspend
                  </button>
                </div>
              </div>

              {/* Danger Zone: Permanent User Deletion */}
              <div className="bg-red-50/50 border border-red-200 rounded-xl p-4 space-y-3">
                <div className="flex items-center gap-2 text-red-800 font-semibold text-sm">
                  <AlertTriangle className="w-4 h-4 text-red-600" />
                  <h4>Danger Zone</h4>
                </div>
                <p className="text-xs text-red-700 leading-relaxed">
                  Permanently deleting this user will delete their record from PostgreSQL (cascading all dependent records) and remove their Firebase Authentication identity. This action is irreversible.
                </p>
                {selectedUser.id === mealshareUser?.id || selectedUser.email === mealshareUser?.email ? (
                  <div className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200">
                    You cannot delete your currently authenticated administrator account.
                  </div>
                ) : (
                  <button
                    onClick={() => handleInitiateRemove(selectedUser)}
                    className="w-full py-2 px-3 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-medium transition flex items-center justify-center gap-1.5 shadow-sm shadow-red-200"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    Permanently Delete User Account
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Remove User Confirmation Modal */}
      {userToRemove && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => !removeLoading && setUserToRemove(null)} />
          <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-gray-100 p-6 space-y-5 z-10 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-xl bg-red-100 flex items-center justify-center text-red-600 flex-shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-gray-900">Permanently Delete User Account</h3>
                <p className="text-xs text-gray-500 mt-1">
                  Are you sure you want to permanently delete this user? This action cannot be undone.
                </p>
              </div>
              <button
                onClick={() => setUserToRemove(null)}
                disabled={removeLoading}
                className="p-1 hover:bg-gray-100 rounded-lg text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {removeError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-800 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
                <span>{removeError}</span>
              </div>
            )}

            {/* Identifying Details Card */}
            <div className="bg-gray-50 rounded-xl p-4 space-y-2 border border-gray-100 text-xs">
              <div className="flex justify-between items-center py-1 border-b border-gray-200/60">
                <span className="text-gray-500 font-medium">Name:</span>
                <span className="text-gray-900 font-semibold">{userToRemove.name || 'Unnamed'}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-gray-200/60">
                <span className="text-gray-500 font-medium">Email:</span>
                <span className="text-gray-900 font-mono">{userToRemove.email}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-gray-200/60">
                <span className="text-gray-500 font-medium">Phone:</span>
                <span className="text-gray-900">{userToRemove.phone || '—'}</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-gray-200/60">
                <span className="text-gray-500 font-medium">Role:</span>
                <StatusBadge status={userToRemove.role} size="sm" />
              </div>
              <div className="flex justify-between items-center py-1 border-b border-gray-200/60">
                <span className="text-gray-500 font-medium">Current Status:</span>
                <StatusBadge status={userToRemove.status} size="sm" />
              </div>
              <div className="flex justify-between items-center py-1">
                <span className="text-gray-500 font-medium">User ID:</span>
                <span className="text-gray-600 font-mono text-[11px] truncate max-w-[200px]">{userToRemove.id}</span>
              </div>
            </div>

            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-800">
              <p className="font-semibold mb-1 text-red-900">Permanent Deletion Warning:</p>
              <ul className="list-disc list-inside space-y-0.5 text-red-700">
                <li>User record will be permanently deleted from PostgreSQL</li>
                <li>Dependent donor/receiver profiles and operational records will be removed</li>
                <li>Firebase Authentication account identity will be revoked and deleted</li>
                <li>This operation is irreversible; the user must re-register if they wish to rejoin</li>
              </ul>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setUserToRemove(null)}
                disabled={removeLoading}
                className="px-4 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-100 rounded-xl transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmRemove}
                disabled={removeLoading}
                className="px-4 py-2 text-xs font-semibold text-white bg-red-600 hover:bg-red-700 rounded-xl transition disabled:opacity-50 inline-flex items-center gap-1.5 shadow-sm shadow-red-200"
              >
                {removeLoading ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    Deleting Permanently...
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    Permanently Delete User
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
