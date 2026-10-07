import { useState, useEffect, useCallback, Fragment } from 'react';
import { Search, X, Package, MapPin, ShieldCheck, ChevronRight, AlertCircle, RefreshCw } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';

const STATUS_FILTERS = [
  'All',
  'AVAILABLE',
  'REQUESTED',
  'APPROVED',
  'PICKUP_ASSIGNED',
  'PICKED_UP',
  'COMPLETED',
  'EXPIRED',
  'CANCELLED',
];

const CATEGORY_FILTERS = [
  { label: 'All Categories', value: 'All' },
  { label: 'Prepared Meals', value: 'PREPARED_MEALS' },
  { label: 'Packaged Food', value: 'PACKAGED_FOOD' },
  { label: 'Bakery', value: 'BAKERY' },
  { label: 'Fruits', value: 'FRUITS' },
  { label: 'Vegetables', value: 'VEGETABLES' },
  { label: 'Grains', value: 'GRAINS' },
  { label: 'Dairy', value: 'DAIRY' },
  { label: 'Other', value: 'OTHER' },
];

interface AdminDonation {
  id: string;
  donorId?: string;
  donorName: string;
  title: string;
  description?: string;
  foodCategory: string;
  quantity: number;
  allocatedQuantity: number;
  remainingQuantity: number;
  quantityUnit: string;
  preparationDateTime?: string;
  consumeBefore: string;
  pickupAvailableFrom?: string;
  pickupAvailableUntil?: string;
  pickupAddress: string;
  latitude?: number;
  longitude?: number;
  foodSafetyDeclared: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface AdminDonationRequest {
  id: string;
  donationId: string;
  donationTitle: string;
  receiverId: string;
  receiverName: string;
  receiverOrganization: string;
  quantity: number;
  quantityUnit: string;
  purpose: string;
  status: string;
  requestedAt: string;
}

interface AdminDonationDetail {
  donation: AdminDonation;
  requests: AdminDonationRequest[];
}

export default function DonationsPage() {
  const [data, setData] = useState<AdminDonation[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [categoryFilter, setCategoryFilter] = useState('All');

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [selectedDonation, setSelectedDonation] = useState<AdminDonation | null>(null);
  const [detailData, setDetailData] = useState<AdminDonationDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const fetchDonations = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (statusFilter !== 'All') params.status = statusFilter;
      if (categoryFilter !== 'All') params.foodCategory = categoryFilter;
      if (search.trim()) params.search = search.trim();

      const res = await apiClient.get('/admin/donations', { params });
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
      const msg = err.response?.data?.message || err.message || 'Could not load donations from backend';
      setApiError(msg);
      setData([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, statusFilter, categoryFilter, search]);

  useEffect(() => {
    fetchDonations();
  }, [fetchDonations]);

  const handleSelectDonation = async (donation: AdminDonation) => {
    setSelectedDonation(donation);
    setDetailData(null);
    setDetailLoading(true);
    try {
      const res = await apiClient.get(`/admin/donations/${donation.id}`);
      if (res.data) {
        setDetailData(res.data);
      }
    } catch (err: any) {
      // In case detail endpoint fails, use selected row data
      setDetailData({
        donation,
        requests: [],
      });
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: Column<AdminDonation>[] = [
    {
      key: 'id',
      label: 'ID',
      render: (val: string) => <span className="font-mono text-xs text-gray-500">{val ? val.slice(0, 8) + '...' : '—'}</span>,
    },
    {
      key: 'title',
      label: 'Food Item',
      render: (val: string, row: AdminDonation) => (
        <div>
          <p className="font-medium text-gray-800">{val}</p>
          <p className="text-xs text-gray-500">{row.foodCategory?.replace(/_/g, ' ')}</p>
        </div>
      ),
    },
    {
      key: 'donorName',
      label: 'Donor',
      render: (val: string) => <span className="text-sm text-gray-700">{val}</span>,
    },
    {
      key: 'quantity',
      label: 'Qty (Remaining/Total)',
      render: (val: number, row: AdminDonation) => (
        <span className="text-sm font-medium">
          {row.remainingQuantity ?? val} / {val} {row.quantityUnit}
        </span>
      ),
    },
    {
      key: 'pickupAddress',
      label: 'Location',
      render: (val: string) => (
        <span className="text-xs text-gray-600 max-w-[200px] truncate block" title={val}>
          {val || '—'}
        </span>
      ),
    },
    {
      key: 'consumeBefore',
      label: 'Consume Before',
      render: (val: string) => {
        if (!val) return <span className="text-xs text-gray-400">—</span>;
        const date = new Date(val);
        const isUrgent = date.getTime() - Date.now() < 4 * 60 * 60 * 1000 && date.getTime() > Date.now();
        const isPast = date.getTime() <= Date.now();
        return (
          <span className={`text-xs ${isPast ? 'text-gray-400' : isUrgent ? 'text-red-600 font-medium' : 'text-gray-600'}`}>
            {date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })}{' '}
            {date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
          </span>
        );
      },
    },
    {
      key: 'status',
      label: 'Status',
      render: (val: string) => <StatusBadge status={val} />,
    },
    {
      key: 'createdAt',
      label: 'Created',
      render: (val: string) => (
        <span className="text-xs text-gray-500">
          {val ? new Date(val).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }) : '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: '',
      render: (_: any, row: AdminDonation) => (
        <button
          onClick={(e) => {
            e.stopPropagation();
            handleSelectDonation(row);
          }}
          className="p-1 hover:bg-gray-100 rounded"
        >
          <ChevronRight className="w-4 h-4 text-gray-400" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Donations</h1>
          <p className="text-sm text-gray-500">
            {totalElements} donations across platform
          </p>
        </div>
        <button
          onClick={() => fetchDonations()}
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
            <span>Backend unavailable: {apiError}. Donation data could not be refreshed.</span>
          </div>
          <button
            onClick={() => fetchDonations()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setCurrentPage(1);
            }}
            placeholder="Search by ID, food title, or donor..."
            className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400"
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setCurrentPage(1);
          }}
          className="text-sm px-3 py-2 bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
        >
          {STATUS_FILTERS.map((s) => (
            <option key={s} value={s}>
              {s === 'All' ? 'All Statuses' : s.replace(/_/g, ' ')}
            </option>
          ))}
        </select>

        <select
          value={categoryFilter}
          onChange={(e) => {
            setCategoryFilter(e.target.value);
            setCurrentPage(1);
          }}
          className="text-sm px-3 py-2 bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
        >
          {CATEGORY_FILTERS.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>

        {(statusFilter !== 'All' || categoryFilter !== 'All' || search) && (
          <button
            onClick={() => {
              setSearch('');
              setStatusFilter('All');
              setCategoryFilter('All');
              setCurrentPage(1);
            }}
            className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1"
          >
            <X className="w-3 h-3" /> Clear filters
          </button>
        )}
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={data}
        emptyMessage={loading ? 'Loading donations from backend...' : 'No donations match your filters'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => handleSelectDonation(row)}
      />

      {/* Detail Drawer */}
      {selectedDonation && (
        <DonationDetailDrawer
          donation={detailData?.donation || selectedDonation}
          requests={detailData?.requests || []}
          loadingRequests={detailLoading}
          onClose={() => setSelectedDonation(null)}
        />
      )}
    </div>
  );
}

function DonationDetailDrawer({
  donation,
  requests,
  loadingRequests,
  onClose,
}: {
  donation: AdminDonation;
  requests: AdminDonationRequest[];
  loadingRequests: boolean;
  onClose: () => void;
}) {
  const LIFECYCLE = ['AVAILABLE', 'REQUESTED', 'APPROVED', 'PICKUP_ASSIGNED', 'PICKED_UP', 'COMPLETED'];
  const currentStep = LIFECYCLE.indexOf(donation.status);

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-black/20" onClick={onClose} />
      <div className="relative w-full max-w-lg bg-white shadow-xl overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
          <div>
            <p className="text-xs font-mono text-gray-500">{donation.id}</p>
            <h2 className="text-lg font-semibold text-gray-900">{donation.title}</h2>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg">
            <X className="w-5 h-5 text-gray-500" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Status */}
          <div className="flex items-center gap-3">
            <StatusBadge status={donation.status} size="md" />
          </div>

          {/* Lifecycle */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-3">Donation Lifecycle</h4>
            <div className="flex items-center gap-1">
              {LIFECYCLE.map((step, i) => {
                const isDone = i <= currentStep && currentStep >= 0;
                const isCurrent = i === currentStep;
                return (
                  <Fragment key={step}>
                    <div className="flex flex-col items-center flex-1 min-w-0">
                      <div
                        className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                          isDone ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-400'
                        } ${isCurrent ? 'ring-2 ring-primary-200' : ''}`}
                      >
                        {i + 1}
                      </div>
                      <span
                        className={`text-[10px] mt-1 text-center leading-tight ${
                          isDone ? 'text-primary-700 font-medium' : 'text-gray-400'
                        }`}
                      >
                        {step.replace(/_/g, ' ')}
                      </span>
                    </div>
                    {i < LIFECYCLE.length - 1 && (
                      <div className={`h-px flex-1 ${i < currentStep ? 'bg-primary-400' : 'bg-gray-200'}`} />
                    )}
                  </Fragment>
                );
              })}
            </div>
          </div>

          {/* Food Details */}
          <div className="bg-gray-50 rounded-xl p-4 space-y-3">
            <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
              <Package className="w-4 h-4 text-primary-500" /> Food Details
            </h4>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-500">Category</span>
                <p className="font-medium text-gray-800">{donation.foodCategory?.replace(/_/g, ' ')}</p>
              </div>
              <div>
                <span className="text-gray-500">Total Quantity</span>
                <p className="font-medium text-gray-800">
                  {donation.quantity} {donation.quantityUnit}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Allocated Quantity</span>
                <p className="font-medium text-gray-800">
                  {donation.allocatedQuantity ?? 0} {donation.quantityUnit}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Remaining Quantity</span>
                <p className="font-medium text-gray-800">
                  {donation.remainingQuantity ?? donation.quantity} {donation.quantityUnit}
                </p>
              </div>
              <div className="col-span-2">
                <span className="text-gray-500">Description</span>
                <p className="font-medium text-gray-800">{donation.description || 'No description provided'}</p>
              </div>
            </div>
          </div>

          {/* Food Safety */}
          <div className="bg-gray-50 rounded-xl p-4 space-y-3">
            <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-primary-500" /> Food Safety Information
            </h4>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-500">Consume Before</span>
                <p className="font-medium text-gray-800">
                  {donation.consumeBefore
                    ? new Date(donation.consumeBefore).toLocaleString('en-IN', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })
                    : '—'}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Safety Declared</span>
                <p className="font-medium text-gray-800">{donation.foodSafetyDeclared ? 'Yes' : 'No'}</p>
              </div>
              {donation.pickupAvailableFrom && (
                <div className="col-span-2">
                  <span className="text-gray-500">Available Window</span>
                  <p className="font-medium text-gray-800">
                    {new Date(donation.pickupAvailableFrom).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}{' '}
                    - {donation.pickupAvailableUntil ? new Date(donation.pickupAvailableUntil).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : 'Open'}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Location */}
          <div className="bg-gray-50 rounded-xl p-4 space-y-2">
            <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
              <MapPin className="w-4 h-4 text-primary-500" /> Pickup Location
            </h4>
            <p className="text-sm text-gray-800">{donation.pickupAddress || 'Address on file'}</p>
            {donation.latitude && donation.longitude && (
              <p className="text-xs font-mono text-gray-500">
                Coordinates: {donation.latitude.toFixed(6)}, {donation.longitude.toFixed(6)}
              </p>
            )}
          </div>

          {/* Related Requests */}
          <div>
            <h4 className="text-sm font-semibold text-gray-700 mb-2">
              Requests ({requests.length})
            </h4>
            {loadingRequests ? (
              <div className="p-4 text-center text-sm text-gray-400">Loading requests...</div>
            ) : requests.length > 0 ? (
              <div className="space-y-2">
                {requests.map((r) => (
                  <div key={r.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div>
                      <p className="text-sm font-medium text-gray-800">{r.receiverName}</p>
                      <p className="text-xs text-gray-500">
                        {r.quantity} {r.quantityUnit} • {r.purpose || r.receiverOrganization}
                      </p>
                    </div>
                    <StatusBadge status={r.status} />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-400 italic">No receiver requests for this donation yet.</p>
            )}
          </div>

          {/* Donor Info */}
          <div className="bg-gray-50 rounded-xl p-4">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">Donor</h4>
            <p className="text-sm font-medium text-gray-800">{donation.donorName}</p>
            <p className="text-xs text-gray-500 mt-0.5">
              Donation created: {donation.createdAt ? new Date(donation.createdAt).toLocaleString('en-IN') : '—'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
