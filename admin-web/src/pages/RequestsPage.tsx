import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, X, FileText, MessageSquare, AlertCircle, RefreshCw, Building, User, Info, Truck } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';

const STATUS_TABS = ['All', 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'COMPLETED'];

interface AdminRequest {
  id: string;
  donationId: string;
  donationTitle: string;
  donorId?: string;
  donorName?: string;
  receiverId?: string;
  receiverName?: string;
  receiverOrganization?: string;
  receiverOrgId?: string;
  receiverOrgName?: string;
  quantity?: number;
  requestedQuantity?: number;
  allocatedQuantity?: number;
  quantityUnit?: string;
  unit?: string;
  purpose?: string;
  requestReason?: string;
  status: string;
  requestedAt: string;
  updatedAt?: string;
  reviewedAt?: string;
  reviewerNotes?: string;
}

interface AdminRequestDetail {
  request: AdminRequest;
  donationSummary?: {
    id: string;
    title: string;
    donorName: string;
    foodCategory: string;
    status: string;
  };
}

export default function RequestsPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<AdminRequest[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState('All');

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const [selectedRequest, setSelectedRequest] = useState<AdminRequest | null>(null);
  const [detailData, setDetailData] = useState<AdminRequestDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (activeTab !== 'All') params.status = activeTab;
      if (search.trim()) params.search = search.trim();

      const res = await apiClient.get('/admin/requests', { params });
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
      const msg = err.response?.data?.message || err.message || 'Could not load requests from backend';
      setApiError(msg);
      setData([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, activeTab, search]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleSelectRequest = async (req: AdminRequest) => {
    setSelectedRequest(req);
    setDetailData(null);
    setDetailLoading(true);
    try {
      const res = await apiClient.get(`/admin/requests/${req.id}`);
      if (res.data) {
        setDetailData(res.data);
      }
    } catch (err: any) {
      setDetailData({
        request: req,
      });
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: Column<AdminRequest>[] = [
    {
      key: 'id',
      label: 'ID',
      render: (val: string) => <span className="font-mono text-xs text-gray-500">{val ? val.slice(0, 8) + '...' : '—'}</span>,
    },
    {
      key: 'donationTitle',
      label: 'Donation',
      render: (val: string, row: AdminRequest) => (
        <div>
          <p className="font-medium text-gray-800">{val || 'Donation Item'}</p>
          <p className="font-mono text-xs text-gray-400">{row.donationId ? row.donationId.slice(0, 8) + '...' : '—'}</p>
        </div>
      ),
    },
    {
      key: 'receiverName',
      label: 'Receiver',
      render: (_val: string, row: AdminRequest) => {
        const orgName = row.receiverOrgName || row.receiverOrganization || row.receiverName || 'Receiver Org';
        return (
          <div>
            <p className="font-medium text-gray-800">{orgName}</p>
            {row.receiverName && row.receiverName !== orgName && (
              <p className="text-xs text-gray-500">Rep: {row.receiverName}</p>
            )}
          </div>
        );
      },
    },
    {
      key: 'quantity',
      label: 'Quantity',
      render: (_val: any, row: AdminRequest) => {
        const reqQty = row.requestedQuantity ?? row.quantity ?? 0;
        const allocQty = row.allocatedQuantity;
        const unit = row.unit || row.quantityUnit || '';
        return (
          <div>
            <span className="text-sm font-medium">{reqQty} {unit}</span>
            {allocQty != null && (row.status === 'APPROVED' || row.status === 'COMPLETED') && (
              <span className="block text-xs text-emerald-600 font-medium">
                Allocated: {allocQty} {unit}
              </span>
            )}
          </div>
        );
      },
    },
    {
      key: 'purpose',
      label: 'Purpose',
      render: (_val: string, row: AdminRequest) => {
        const reason = row.requestReason || row.purpose || '—';
        return (
          <span className="text-xs text-gray-600 max-w-[200px] truncate block" title={reason}>
            {reason}
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
      key: 'requestedAt',
      label: 'Requested',
      render: (val: string) => (
        <span className="text-xs text-gray-500">
          {val
            ? new Date(val).toLocaleDateString('en-IN', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })
            : '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (_val: any, row: AdminRequest) => (
        <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
          {row.status === 'APPROVED' ? (
            <button
              onClick={() => navigate(`/admin/pickups?requestId=${row.id}`)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary-600 text-white rounded-lg text-xs font-semibold hover:bg-primary-700 transition shadow-sm"
              title="Schedule Pickup for this Approved Claim"
            >
              <Truck className="w-3.5 h-3.5" />
              Schedule Pickup
            </button>
          ) : row.status === 'PENDING' ? (
            <span className="text-xs text-amber-600 font-medium">Awaiting Donor Review</span>
          ) : row.status === 'COMPLETED' ? (
            <span className="text-xs text-emerald-600 font-medium">Completed</span>
          ) : (
            <span className="text-xs text-gray-400">—</span>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Donation Requests</h1>
          <p className="text-sm text-gray-500">
            {totalElements} receiver requests across platform (Operational Visibility)
          </p>
        </div>
        <button
          onClick={() => fetchRequests()}
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
            <span>Backend unavailable: {apiError}. Requests data could not be refreshed.</span>
          </div>
          <button
            onClick={() => fetchRequests()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

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
            {tab === 'All' ? 'All Requests' : tab.charAt(0) + tab.slice(1).toLowerCase().replace(/_/g, ' ')}
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
          placeholder="Search requests by receiver or donation..."
          className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
        />
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={data}
        emptyMessage={loading ? 'Loading requests from backend...' : 'No requests found matching your filters'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => handleSelectRequest(row)}
      />

      {/* Detail Drawer */}
      {selectedRequest && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelectedRequest(null)} />
          <div className="relative w-full max-w-md bg-white shadow-xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <p className="text-xs font-mono text-gray-500">{selectedRequest.id}</p>
                <h2 className="text-lg font-semibold text-gray-900">Request Details</h2>
              </div>
              <button onClick={() => setSelectedRequest(null)} className="p-2 hover:bg-gray-100 rounded-lg">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="p-6 space-y-5">
              <div className="flex items-center justify-between">
                <StatusBadge status={selectedRequest.status} size="md" />
                <span className="text-xs text-gray-500">
                  Requested {new Date(selectedRequest.requestedAt).toLocaleString('en-IN')}
                </span>
              </div>

              {/* Notice that request approvals are managed by donor */}
              <div className="bg-blue-50 border border-blue-200 text-blue-800 p-3 rounded-lg text-xs flex items-start gap-2">
                <Info className="w-4 h-4 text-blue-600 flex-shrink-0 mt-0.5" />
                <span>
                  Donation requests are evaluated and approved directly by donors under FCFS allocation rules. Admin access provides operational oversight.
                </span>
              </div>

              {/* Donation Info */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-primary-500" /> Target Donation
                </h4>
                <p className="text-sm font-medium text-gray-800">
                  {detailData?.donationSummary?.title || selectedRequest.donationTitle}
                </p>
                <p className="font-mono text-xs text-gray-400">{selectedRequest.donationId}</p>
                {detailLoading && <p className="text-xs text-gray-400">Loading donation details...</p>}
                {detailData?.donationSummary && (
                  <div className="flex items-center gap-2 mt-2 pt-2 border-t border-gray-200/60 text-xs text-gray-600">
                    <span>Donor: {detailData.donationSummary.donorName}</span>
                    <span>•</span>
                    <StatusBadge status={detailData.donationSummary.status} />
                  </div>
                )}
              </div>

              {/* Receiver Info */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <Building className="w-4 h-4 text-primary-500" /> Receiver Organization
                </h4>
                <p className="text-sm font-medium text-gray-800">{selectedRequest.receiverOrganization || selectedRequest.receiverName}</p>
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <User className="w-3.5 h-3.5 text-gray-400" />
                  <span>Representative: {selectedRequest.receiverName}</span>
                </div>
              </div>

              {/* Request Metrics */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <h4 className="text-sm font-semibold text-gray-700">Allocation Details</h4>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-gray-500">Requested Quantity</span>
                    <p className="font-medium text-gray-900">
                      {selectedRequest.quantity} {selectedRequest.quantityUnit}
                    </p>
                  </div>
                  <div>
                    <span className="text-gray-500">Status</span>
                    <p className="font-medium text-gray-900">{selectedRequest.status}</p>
                  </div>
                </div>
              </div>

              {/* Purpose */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-primary-500" /> Purpose / Community Need
                </h4>
                <p className="text-sm text-gray-800">{selectedRequest.purpose || 'No purpose notes provided'}</p>
              </div>

              {/* Review details if reviewed */}
              {(selectedRequest.reviewedAt || selectedRequest.reviewerNotes) && (
                <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                  <h4 className="text-sm font-semibold text-gray-700">Donor Review Outcome</h4>
                  {selectedRequest.reviewedAt && (
                    <p className="text-xs text-gray-500">
                      Reviewed on {new Date(selectedRequest.reviewedAt).toLocaleString('en-IN')}
                    </p>
                  )}
                  {selectedRequest.reviewerNotes && (
                    <p className="text-sm text-gray-800 italic">"{selectedRequest.reviewerNotes}"</p>
                  )}
                </div>
              )}

              {/* Action to schedule pickup if approved */}
              {selectedRequest.status === 'APPROVED' && (
                <div className="pt-2">
                  <button
                    onClick={() => navigate(`/admin/pickups?requestId=${selectedRequest.id}`)}
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 transition shadow-sm"
                  >
                    <Truck className="w-4 h-4" />
                    Schedule Pickup for this Request
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
