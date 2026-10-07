import { useState, useMemo, useEffect, useCallback } from 'react';
import {
  ShieldCheck, CheckCircle, XCircle, X, AlertCircle, RefreshCw,
  Building, Mail, Phone, MapPin, Users, Search
} from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import StatCard from '../components/StatCard';
import apiClient from '../api/client';

const STATUS_TABS = ['All', 'PENDING', 'APPROVED', 'REJECTED'];

interface AdminVerification {
  id: string;
  userId?: string;
  organizationName: string;
  organizationType: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  registrationNumber?: string;
  beneficiaryCapacity?: number;
  status: string;
  submittedAt: string;
  notes?: string;
}

export default function VerificationPage() {
  const [data, setData] = useState<AdminVerification[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [activeTab, setActiveTab] = useState('All');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<AdminVerification | null>(null);

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchVerifications = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (activeTab !== 'All') params.status = activeTab;
      if (search.trim()) params.search = search.trim();

      const res = await apiClient.get('/admin/verification', { params });
      if (res.data && Array.isArray(res.data.content)) {
        setData(res.data.content);
        setTotalElements(res.data.totalElements || res.data.content.length);
        setTotalPages(res.data.totalPages || 1);
      } else {
        setData([]);
        setTotalElements(0);
        setTotalPages(1);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load verification records from backend';
      setApiError(msg);
      setData([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [activeTab, search, currentPage, pageSize]);

  useEffect(() => {
    fetchVerifications();
  }, [fetchVerifications]);

  const stats = useMemo(
    () => ({
      pending: data.filter((v) => v.status === 'PENDING').length,
      approved: data.filter((v) => v.status === 'APPROVED').length,
      rejected: data.filter((v) => v.status === 'REJECTED').length,
      total: totalElements || data.length,
    }),
    [data, totalElements]
  );

  const handleUpdateStatus = async (id: string, newStatus: 'APPROVED' | 'REJECTED') => {
    setActionLoading(true);
    setActionMessage(null);
    try {
      await apiClient.patch(`/admin/verification/${id}/status`, {
        status: newStatus,
        notes: `Admin adjudicated verification status to ${newStatus}`,
      });
      setActionMessage({ type: 'success', text: `Organization status set to ${newStatus}` });
      setData((prev) => prev.map((v) => (v.id === id ? { ...v, status: newStatus } : v)));
      if (selected && selected.id === id) {
        setSelected({ ...selected, status: newStatus });
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to update verification status';
      setActionMessage({ type: 'error', text: msg });
    } finally {
      setActionLoading(false);
    }
  };

  const columns: Column<AdminVerification>[] = [
    {
      key: 'organizationName',
      label: 'Organization',
      render: (val: string, row: AdminVerification) => (
        <div>
          <p className="font-medium text-gray-800">{val}</p>
          <p className="text-xs text-gray-500">{row.organizationType}</p>
        </div>
      ),
    },
    { key: 'contactPerson', label: 'Contact' },
    { key: 'email', label: 'Email', render: (val: string) => <span className="text-sm text-gray-600">{val}</span> },
    {
      key: 'registrationNumber',
      label: 'Reg #',
      render: (val: string) => (
        <span className="font-mono text-xs text-gray-600 bg-gray-50 px-2 py-0.5 rounded border border-gray-100">
          {val || '—'}
        </span>
      ),
    },
    {
      key: 'submittedAt',
      label: 'Submitted',
      render: (val: string) => (
        <span className="text-xs text-gray-500">
          {val ? new Date(val).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
        </span>
      ),
    },
    { key: 'status', label: 'Status', render: (val: string) => <StatusBadge status={val} /> },
    {
      key: 'actions',
      label: '',
      render: (_: any, row: AdminVerification) =>
        row.status === 'PENDING' ? (
          <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => handleUpdateStatus(row.id, 'APPROVED')}
              disabled={actionLoading}
              className="p-1.5 hover:bg-emerald-50 rounded-lg text-emerald-600"
              title="Approve"
            >
              <CheckCircle className="w-4 h-4" />
            </button>
            <button
              onClick={() => handleUpdateStatus(row.id, 'REJECTED')}
              disabled={actionLoading}
              className="p-1.5 hover:bg-red-50 rounded-lg text-red-500"
              title="Reject"
            >
              <XCircle className="w-4 h-4" />
            </button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Receiver Verification</h1>
          <p className="text-sm text-gray-500">Review and verify receiver organizations for food allocation</p>
        </div>
        <button
          onClick={() => fetchVerifications()}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend unavailable: {apiError}. Verification data could not be refreshed.</span>
          </div>
          <button
            onClick={() => fetchVerifications()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <StatCard title="Pending Review" value={stats.pending} icon={ShieldCheck} iconBg="bg-amber-50" iconColor="text-amber-600" />
        <StatCard title="Approved Organizations" value={stats.approved} icon={CheckCircle} iconBg="bg-emerald-50" iconColor="text-emerald-600" />
        <StatCard title="Rejected Requests" value={stats.rejected} icon={XCircle} iconBg="bg-red-50" iconColor="text-red-600" />
        <StatCard title="Total Receiver Orgs" value={stats.total} icon={Building} iconBg="bg-blue-50" iconColor="text-blue-600" />
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-gray-200 overflow-x-auto">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => {
              setActiveTab(tab);
              setCurrentPage(1);
            }}
            className={`px-3 py-2 text-sm font-medium border-b-2 whitespace-nowrap transition ${
              activeTab === tab
                ? 'border-primary-500 text-primary-700'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab === 'All' ? 'All' : tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setCurrentPage(1);
          }}
          placeholder="Search organizations or contact person..."
          className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
        />
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={data}
        emptyMessage={loading ? 'Loading verifications from backend...' : 'No verification requests found'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => {
          setSelected(row);
          setActionMessage(null);
        }}
      />

      {/* Detail Drawer */}
      {selected && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelected(null)} />
          <div className="relative w-full max-w-md bg-white shadow-xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="text-xs font-mono text-gray-500">{selected.id}</p>
                <h2 className="text-lg font-semibold text-gray-900">Verification Review</h2>
              </div>
              <button onClick={() => setSelected(null)} className="p-2 hover:bg-gray-100 rounded-lg">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <div className="p-6 space-y-5">
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

              <div className="flex items-center justify-between">
                <StatusBadge status={selected.status} size="md" />
                <span className="text-xs text-gray-500">
                  Submitted {new Date(selected.submittedAt).toLocaleDateString('en-IN')}
                </span>
              </div>

              <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <Building className="w-4 h-4 text-primary-500" /> Organization Profile
                </h4>
                <div className="space-y-2 text-sm">
                  <div>
                    <span className="text-gray-500 text-xs">Official Name</span>
                    <p className="font-medium text-gray-800">{selected.organizationName}</p>
                  </div>
                  <div>
                    <span className="text-gray-500 text-xs">Type</span>
                    <p className="font-medium text-gray-800">{selected.organizationType}</p>
                  </div>
                  {selected.registrationNumber && (
                    <div>
                      <span className="text-gray-500 text-xs">Govt Registration Number</span>
                      <p className="font-mono text-sm font-medium text-gray-800">{selected.registrationNumber}</p>
                    </div>
                  )}
                  {selected.beneficiaryCapacity && (
                    <div>
                      <span className="text-gray-500 text-xs">Beneficiary Serving Capacity</span>
                      <p className="font-medium text-gray-800">{selected.beneficiaryCapacity} people</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <Users className="w-4 h-4 text-primary-500" /> Contact Representative
                </h4>
                <div className="space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-500 text-xs w-16">Contact:</span>
                    <span className="font-medium">{selected.contactPerson}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Mail className="w-3.5 h-3.5 text-gray-400" />
                    <span>{selected.email}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Phone className="w-3.5 h-3.5 text-gray-400" />
                    <span>{selected.phone || '—'}</span>
                  </div>
                  <div className="flex items-start gap-2 pt-1 border-t border-gray-200/60">
                    <MapPin className="w-3.5 h-3.5 text-gray-400 mt-0.5 flex-shrink-0" />
                    <span className="text-xs text-gray-600">{selected.address}</span>
                  </div>
                </div>
              </div>

              {selected.notes && (
                <div className="bg-amber-50 border border-amber-100 rounded-xl p-4">
                  <p className="text-sm font-medium text-amber-800">Verification History / Notes</p>
                  <p className="text-xs text-amber-700 mt-1">{selected.notes}</p>
                </div>
              )}

              {selected.status === 'PENDING' && (
                <div className="flex gap-2 pt-2">
                  <button
                    onClick={() => handleUpdateStatus(selected.id, 'APPROVED')}
                    disabled={actionLoading}
                    className="flex-1 py-2.5 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <CheckCircle className="w-4 h-4" /> Approve Organization
                  </button>
                  <button
                    onClick={() => handleUpdateStatus(selected.id, 'REJECTED')}
                    disabled={actionLoading}
                    className="flex-1 py-2.5 bg-white border border-red-200 text-red-600 rounded-lg text-sm font-medium hover:bg-red-50 disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <XCircle className="w-4 h-4" /> Reject
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
