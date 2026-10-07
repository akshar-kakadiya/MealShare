import { useState, useMemo, useEffect, useCallback } from 'react';
import { Truck, MapPin, Clock, Shield, Plus, X, AlertCircle, CheckCircle, RefreshCw } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import StatCard from '../components/StatCard';
import apiClient from '../api/client';

export interface Pickup {
  id: string;
  donationId: string;
  donationItem: string;
  donorName: string;
  receiverName: string;
  pickupAddress: string;
  deliveryAddress: string;
  scheduledTime: string;
  otpCode: string;
  status: string;
  createdAt: string;
}

const STATUS_TABS = ['All', 'ASSIGNED', 'PICKED_UP', 'COMPLETED', 'CANCELLED'];

export default function PickupsPage() {
  const [data, setData] = useState<Pickup[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [activeTab, setActiveTab] = useState('All');
  const [selectedPickup, setSelectedPickup] = useState<Pickup | null>(null);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // Schedule Pickup Modal State
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [assignForm, setAssignForm] = useState({
    donationRequestId: '',
    scheduledAt: new Date(Date.now() + 3600000 * 2).toISOString().slice(0, 16),
  });
  const [assignLoading, setAssignLoading] = useState(false);
  const [assignSuccess, setAssignSuccess] = useState<string | null>(null);
  const [assignError, setAssignError] = useState<string | null>(null);

  const fetchPickups = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (activeTab !== 'All') {
        params.status = activeTab;
      }
      const res = await apiClient.get('/admin/pickups', { params });
      if (res.data && res.data.content && Array.isArray(res.data.content)) {
        const mapped: Pickup[] = res.data.content.map((p: any) => ({
          id: p.pickupId || p.id,
          donationId: p.donationId || '',
          donationItem: p.donationTitle || 'Food Donation',
          donorName: p.donorName || 'Donor Org',
          receiverName: p.receiverOrganizationName || p.receiverName || 'Receiver Org',
          pickupAddress: p.pickupAddress || 'Address on file',
          deliveryAddress: 'Receiver Distribution Center',
          scheduledTime: p.scheduledAt || new Date().toISOString(),
          otpCode: '••••••', // Strictly masked for operational security
          status: p.status || 'ASSIGNED',
          createdAt: p.createdAt || p.assignedAt || new Date().toISOString(),
        }));
        setData(mapped);
        setTotalElements(res.data.totalElements || mapped.length);
        setTotalPages(res.data.totalPages || 1);
      } else {
        setData([]);
        setTotalElements(0);
        setTotalPages(1);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load pickups from backend';
      setApiError(msg);
      setData([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [activeTab, currentPage, pageSize]);

  useEffect(() => {
    fetchPickups();
  }, [fetchPickups]);

  const handleAssignSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAssignLoading(true);
    setAssignError(null);
    setAssignSuccess(null);

    try {
      const payload = {
        donationRequestId: assignForm.donationRequestId.trim(),
        scheduledAt: new Date(assignForm.scheduledAt).toISOString(),
      };

      const res = await apiClient.post('/admin/pickups', payload);
      setAssignSuccess(
        `Pickup scheduled successfully! Pickup ID: ${res.data?.pickupId}. Handover OTP securely routed to donor.`
      );
      fetchPickups();
    } catch (err: any) {
      setAssignError(err.response?.data?.message || err.message || 'Failed to schedule pickup');
    } finally {
      setAssignLoading(false);
    }
  };

  const stats = useMemo(
    () => ({
      total: totalElements || data.length,
      active: data.filter((p) => ['ASSIGNED', 'EN_ROUTE', 'OTP_VERIFIED', 'PICKED_UP'].includes(p.status)).length,
      delivered: data.filter((p) => ['COMPLETED', 'DELIVERED'].includes(p.status)).length,
      failed: data.filter((p) => ['CANCELLED', 'FAILED'].includes(p.status)).length,
    }),
    [data, totalElements]
  );

  const columns: Column<Pickup>[] = [
    {
      key: 'id',
      label: 'ID',
      render: (val: string) => <span className="font-mono text-xs text-gray-500">{val ? val.slice(0, 8) + '...' : '—'}</span>,
    },
    {
      key: 'donationItem',
      label: 'Donation',
      render: (val: string, row: Pickup) => (
        <div>
          <p className="font-medium text-gray-800">{val}</p>
          <p className="font-mono text-xs text-gray-400">{row.donationId ? row.donationId.slice(0, 8) + '...' : '—'}</p>
        </div>
      ),
    },
    { key: 'donorName', label: 'From (Donor)', render: (val: string) => <span className="text-sm">{val}</span> },
    { key: 'receiverName', label: 'To (Receiver)', render: (val: string) => <span className="text-sm">{val}</span> },
    {
      key: 'scheduledTime',
      label: 'Scheduled',
      render: (val: string) => (
        <span className="text-xs text-gray-600">
          {new Date(val).toLocaleString('en-IN', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>
      ),
    },
    {
      key: 'otpCode',
      label: 'Handover OTP',
      render: () => <span className="font-mono text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded tracking-widest">Confidential</span>,
    },
    { key: 'status', label: 'Status', render: (val: string) => <StatusBadge status={val} /> },
  ];

  const LIFECYCLE = ['ASSIGNED', 'PICKED_UP', 'COMPLETED'];

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Pickups</h1>
          <p className="text-sm text-gray-500">Track and manage food pickups via Spring Boot PostGIS backend</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => fetchPickups()}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => {
              setAssignError(null);
              setAssignSuccess(null);
              setShowAssignModal(true);
            }}
            className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 transition"
          >
            <Plus className="w-4 h-4" />
            Schedule Pickup
          </button>
        </div>
      </div>

      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend unavailable: {apiError}. Operational pickups data is not available.</span>
          </div>
          <button
            onClick={() => fetchPickups()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <StatCard title="Total Pickups" value={stats.total} icon={Truck} />
        <StatCard title="Active" value={stats.active} icon={Clock} iconBg="bg-amber-50" iconColor="text-amber-600" />
        <StatCard title="Completed" value={stats.delivered} icon={MapPin} iconBg="bg-emerald-50" iconColor="text-emerald-600" />
        <StatCard title="Cancelled" value={stats.failed} icon={Shield} iconBg="bg-red-50" iconColor="text-red-600" />
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
              activeTab === tab ? 'border-primary-500 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab === 'All' ? 'All Pickups' : tab.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        data={data}
        emptyMessage={loading ? 'Loading pickups from backend...' : 'No pickups found matching your filter'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => setSelectedPickup(row)}
      />

      {/* Schedule Food Pickup Modal */}
      {showAssignModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-semibold text-gray-900">Schedule Food Pickup</h3>
              <button onClick={() => setShowAssignModal(false)} className="text-gray-400 hover:text-gray-600 p-1">
                <X className="w-5 h-5" />
              </button>
            </div>

            {assignSuccess && (
              <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 p-3 rounded-lg text-sm flex items-start gap-2">
                <CheckCircle className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
                <span>{assignSuccess}</span>
              </div>
            )}

            {assignError && (
              <div className="bg-red-50 border border-red-200 text-red-800 p-3 rounded-lg text-sm flex items-start gap-2">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <span>{assignError}</span>
              </div>
            )}

            <form onSubmit={handleAssignSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Donation Request ID (Approved Request UUID)
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
                  value={assignForm.donationRequestId}
                  onChange={(e) => setAssignForm({ ...assignForm, donationRequestId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Scheduled Pickup Time</label>
                <input
                  type="datetime-local"
                  required
                  value={assignForm.scheduledAt}
                  onChange={(e) => setAssignForm({ ...assignForm, scheduledAt: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAssignModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  Close
                </button>
                <button
                  type="submit"
                  disabled={assignLoading}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-50 flex items-center gap-2"
                >
                  {assignLoading ? 'Scheduling...' : 'Confirm Schedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detail Drawer */}
      {selectedPickup && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelectedPickup(null)} />
          <div className="relative w-full max-w-md bg-white shadow-xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="text-xs font-mono text-gray-500">{selectedPickup.id}</p>
                <h2 className="text-lg font-semibold text-gray-900">Pickup Details</h2>
              </div>
              <button onClick={() => setSelectedPickup(null)} className="p-2 hover:bg-gray-100 rounded-lg">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <div className="p-6 space-y-5">
              <StatusBadge status={selectedPickup.status} size="md" />

              {/* Route */}
              <div className="bg-gray-50 rounded-xl p-4">
                <div className="flex items-start gap-3">
                  <div className="flex flex-col items-center">
                    <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center">
                      <MapPin className="w-4 h-4 text-emerald-600" />
                    </div>
                    <div className="w-0.5 h-8 bg-gray-200 my-1" />
                    <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                      <MapPin className="w-4 h-4 text-blue-600" />
                    </div>
                  </div>
                  <div className="flex-1 space-y-6">
                    <div>
                      <p className="text-xs text-gray-500">Pickup From</p>
                      <p className="text-sm font-medium text-gray-800">{selectedPickup.donorName}</p>
                      <p className="text-xs text-gray-500">{selectedPickup.pickupAddress}</p>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Deliver To</p>
                      <p className="text-sm font-medium text-gray-800">{selectedPickup.receiverName}</p>
                      <p className="text-xs text-gray-500">{selectedPickup.deliveryAddress}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Details */}
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-gray-50 rounded-xl p-4">
                  <p className="text-xs text-gray-500">Receiver</p>
                  <p className="text-sm font-medium">{selectedPickup.receiverName}</p>
                </div>
                <div className="bg-gray-50 rounded-xl p-4">
                  <p className="text-xs text-gray-500">Scheduled</p>
                  <p className="text-sm font-medium">
                    {new Date(selectedPickup.scheduledTime).toLocaleTimeString('en-IN', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
                <div className="bg-gray-50 rounded-xl p-4">
                  <p className="text-xs text-gray-500">Donation</p>
                  <p className="text-sm font-medium">{selectedPickup.donationItem}</p>
                </div>
                <div className="bg-gray-50 rounded-xl p-4">
                  <p className="text-xs text-gray-500">Handover OTP</p>
                  <p className="text-sm font-mono text-gray-500 italic">Held exclusively by Donor</p>
                </div>
              </div>

              {/* Lifecycle */}
              <div>
                <h4 className="text-sm font-semibold text-gray-700 mb-3">Pickup Lifecycle</h4>
                <div className="space-y-3">
                  {LIFECYCLE.map((step, i) => {
                    const idx = LIFECYCLE.indexOf(selectedPickup.status);
                    const isDone = i <= idx;
                    return (
                      <div key={step} className="flex items-center gap-3">
                        <div
                          className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                            isDone ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-400'
                          }`}
                        >
                          {i + 1}
                        </div>
                        <span className={`text-sm ${isDone ? 'text-gray-800 font-medium' : 'text-gray-400'}`}>
                          {step.replace(/_/g, ' ')}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
