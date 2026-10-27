import { useState, useMemo, useEffect, useCallback } from 'react';
import {
  ShieldCheck, CheckCircle, XCircle, X, AlertCircle, RefreshCw,
  Building, Mail, Phone, MapPin, Users, Search, HelpCircle,
  Image as ImageIcon, Calendar, FileText
} from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import StatCard from '../components/StatCard';
import apiClient, { getBackendAssetUrl } from '../api/client';

const STATUS_TABS = ['All', 'PENDING', 'ACTION_REQUIRED', 'APPROVED', 'REJECTED'];

const ROLE_OPTIONS = [
  { value: 'All', label: 'All Roles' },
  { value: 'DONOR', label: 'Donors' },
  { value: 'RECEIVER', label: 'Receivers' },
];

const CATEGORY_MAP: Record<string, string> = {
  HOTEL_RESTAURANT: 'Hotel / Restaurant',
  HOSTEL_MESS: 'Hostel / Mess',
  EVENT_ORGANIZER: 'Event Organizer',
  HOUSEHOLD: 'Household',
  OTHER_BUSINESS: 'Other Business',
  NGO: 'NGO',
  COMMUNITY_SHELTER: 'Community Shelter',
  COMMUNITY_GROUP: 'Community Group',
  INDIVIDUAL: 'Individual',
};

const DONOR_CATEGORIES = [
  'HOTEL_RESTAURANT',
  'HOSTEL_MESS',
  'EVENT_ORGANIZER',
  'HOUSEHOLD',
  'OTHER_BUSINESS',
];

const RECEIVER_CATEGORIES = [
  'NGO',
  'COMMUNITY_SHELTER',
  'COMMUNITY_GROUP',
  'INDIVIDUAL',
];

interface AdminVerification {
  id: string;
  userId?: string;
  role?: string;
  category?: string;
  organizationName: string;
  organizationType: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  latitude?: number;
  longitude?: number;
  registrationNumber?: string;
  beneficiaryCapacity?: number;
  description?: string;
  businessType?: string;
  eventInfo?: string;
  photos?: string;
  profileStatus?: string;
  status: string;
  rejectionReason?: string;
  actionRequiredNotes?: string;
  submittedAt: string;
  verifiedAt?: string;
  verifiedBy?: string;
  notes?: string;
}

export default function VerificationPage() {
  const [data, setData] = useState<AdminVerification[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [activeTab, setActiveTab] = useState('All');
  const [selectedRole, setSelectedRole] = useState('All');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<AdminVerification | null>(null);

  // Adjudication prompt dialog state
  const [dialogAction, setDialogAction] = useState<'ACTION_REQUIRED' | 'REJECTED' | null>(null);
  const [dialogNotes, setDialogNotes] = useState('');
  const [dialogError, setDialogError] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const availableCategories = useMemo(() => {
    if (selectedRole === 'DONOR') return DONOR_CATEGORIES;
    if (selectedRole === 'RECEIVER') return RECEIVER_CATEGORIES;
    return [...DONOR_CATEGORIES, ...RECEIVER_CATEGORIES];
  }, [selectedRole]);

  const fetchVerifications = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params: Record<string, any> = {
        page: currentPage - 1,
        size: pageSize,
      };
      if (activeTab !== 'All') params.status = activeTab;
      if (selectedRole !== 'All') params.role = selectedRole;
      if (selectedCategory !== 'All') params.category = selectedCategory;
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
  }, [activeTab, selectedRole, selectedCategory, search, currentPage, pageSize]);

  useEffect(() => {
    fetchVerifications();
  }, [fetchVerifications]);

  const stats = useMemo(
    () => ({
      pending: data.filter((v) => v.status === 'PENDING').length,
      actionRequired: data.filter((v) => v.status === 'ACTION_REQUIRED').length,
      approved: data.filter((v) => v.status === 'APPROVED').length,
      rejected: data.filter((v) => v.status === 'REJECTED').length,
      total: totalElements || data.length,
    }),
    [data, totalElements]
  );

  const handleUpdateStatus = async (
    id: string,
    newStatus: 'APPROVED' | 'REJECTED' | 'ACTION_REQUIRED',
    notesText?: string
  ) => {
    setActionLoading(true);
    setActionMessage(null);
    try {
      const payload: Record<string, any> = {
        status: newStatus,
        notes: notesText || `Admin adjudicated verification status to ${newStatus}`,
      };
      if (newStatus === 'REJECTED') {
        payload.rejectionReason = notesText || 'Application rejected by administrator';
      }
      if (newStatus === 'ACTION_REQUIRED') {
        payload.actionRequiredNotes = notesText || 'Additional information required';
      }

      const res = await apiClient.patch(`/admin/verification/${id}/status`, payload);
      const updatedItem = res.data;

      setActionMessage({ type: 'success', text: `Profile verification updated to ${newStatus}` });
      setData((prev) => prev.map((v) => (v.id === id ? { ...v, ...updatedItem, status: newStatus } : v)));
      if (selected && selected.id === id) {
        setSelected({ ...selected, ...updatedItem, status: newStatus });
      }
      setDialogAction(null);
      setDialogNotes('');
      setDialogError(null);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to update verification status';
      setActionMessage({ type: 'error', text: msg });
      setDialogError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const openActionDialog = (action: 'ACTION_REQUIRED' | 'REJECTED') => {
    setDialogAction(action);
    setDialogNotes('');
    setDialogError(null);
  };

  const columns: Column<AdminVerification>[] = [
    {
      key: 'organizationName',
      label: 'Organization / Name',
      render: (val: string, row: AdminVerification) => {
        const catKey = row.category || row.organizationType;
        const catDisplay = CATEGORY_MAP[catKey] || catKey || 'Profile';
        const role = row.role || (row.beneficiaryCapacity !== undefined ? 'RECEIVER' : 'DONOR');
        return (
          <div>
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-800">{val || 'Unnamed Entity'}</span>
              <span
                className={`text-[10px] uppercase font-bold px-1.5 py-0.5 rounded ${
                  role === 'DONOR' ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'
                }`}
              >
                {role}
              </span>
            </div>
            <p className="text-xs text-gray-500">{catDisplay}</p>
          </div>
        );
      },
    },
    {
      key: 'contactPerson',
      label: 'Contact',
      render: (val: string, row: AdminVerification) => (
        <div>
          <p className="text-sm font-medium text-gray-700">{val || '—'}</p>
          <p className="text-xs text-gray-500">{row.email}</p>
        </div>
      ),
    },
    {
      key: 'profileStatus',
      label: 'Profile',
      render: (val: string) => <StatusBadge status={val || 'INCOMPLETE'} />,
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
    { key: 'status', label: 'Verification', render: (val: string) => <StatusBadge status={val} /> },
    {
      key: 'actions',
      label: 'Actions',
      render: (_: any, row: AdminVerification) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {row.status !== 'APPROVED' && (
            <button
              onClick={() => handleUpdateStatus(row.id, 'APPROVED')}
              disabled={actionLoading || row.profileStatus !== 'COMPLETE'}
              className={`p-1.5 rounded-lg transition ${
                row.profileStatus !== 'COMPLETE'
                  ? 'text-gray-300 cursor-not-allowed'
                  : 'hover:bg-emerald-50 text-emerald-600'
              }`}
              title={
                row.profileStatus !== 'COMPLETE'
                  ? 'Profile must be completed before approval'
                  : 'Approve'
              }
            >
              <CheckCircle className="w-4 h-4" />
            </button>
          )}
          {row.status !== 'ACTION_REQUIRED' && (
            <button
              onClick={() => {
                setSelected(row);
                openActionDialog('ACTION_REQUIRED');
              }}
              disabled={actionLoading}
              className="p-1.5 hover:bg-amber-50 rounded-lg text-amber-600 transition"
              title="Request Additional Information"
            >
              <HelpCircle className="w-4 h-4" />
            </button>
          )}
          {row.status !== 'REJECTED' && (
            <button
              onClick={() => {
                setSelected(row);
                openActionDialog('REJECTED');
              }}
              disabled={actionLoading}
              className="p-1.5 hover:bg-red-50 rounded-lg text-red-500 transition"
              title="Reject Request"
            >
              <XCircle className="w-4 h-4" />
            </button>
          )}
        </div>
      ),
    },
  ];

  const parsePhotos = (photoStr?: string): string[] => {
    if (!photoStr || !photoStr.trim()) return [];
    return photoStr
      .split(',')
      .map((p) => p.trim())
      .filter((p) => p.length > 0)
      .map((p) => getBackendAssetUrl(p));
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">User Profile & Verification Review</h1>
          <p className="text-sm text-gray-500">
            Adjudicate role-based registration, category attributes, and verification requests
          </p>
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
            <span>Backend notice: {apiError}.</span>
          </div>
          <button
            onClick={() => fetchVerifications()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <StatCard title="Pending Review" value={stats.pending} icon={ShieldCheck} iconBg="bg-amber-50" iconColor="text-amber-600" />
        <StatCard title="Action Required" value={stats.actionRequired} icon={HelpCircle} iconBg="bg-orange-50" iconColor="text-orange-600" />
        <StatCard title="Approved" value={stats.approved} icon={CheckCircle} iconBg="bg-emerald-50" iconColor="text-emerald-600" />
        <StatCard title="Rejected" value={stats.rejected} icon={XCircle} iconBg="bg-red-50" iconColor="text-red-600" />
        <StatCard title="Total Profiles" value={stats.total} icon={Building} iconBg="bg-blue-50" iconColor="text-blue-600" />
      </div>

      {/* Status Tabs */}
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
            {tab === 'All' ? 'All Statuses' : CATEGORY_MAP[tab] || tab.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Filters & Search Row */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
        <div className="flex flex-wrap items-center gap-2">
          {/* Role Filter */}
          <select
            value={selectedRole}
            onChange={(e) => {
              setSelectedRole(e.target.value);
              setSelectedCategory('All');
              setCurrentPage(1);
            }}
            className="px-3 py-2 text-sm bg-white border border-gray-200 rounded-lg text-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          >
            {ROLE_OPTIONS.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>

          {/* Category Filter */}
          <select
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value);
              setCurrentPage(1);
            }}
            className="px-3 py-2 text-sm bg-white border border-gray-200 rounded-lg text-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          >
            <option value="All">All Categories</option>
            {availableCategories.map((c) => (
              <option key={c} value={c}>
                {CATEGORY_MAP[c] || c}
              </option>
            ))}
          </select>
        </div>

        {/* Search */}
        <div className="relative w-full sm:max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setCurrentPage(1);
            }}
            placeholder="Search name, email, contact..."
            className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          />
        </div>
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={data}
        emptyMessage={loading ? 'Loading verification requests from backend...' : 'No verification requests matching criteria.'}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
        onRowClick={(row) => {
          setSelected(row);
          setActionMessage(null);
          setDialogAction(null);
        }}
      />

      {/* Review Drawer / Modal */}
      {selected && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/25 backdrop-blur-sm" onClick={() => setSelected(null)} />
          <div className="relative w-full max-w-lg bg-white shadow-2xl overflow-y-auto">
            {/* Drawer Header */}
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between z-10">
              <div>
                <div className="flex items-center gap-2">
                  <span
                    className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
                      selected.role === 'DONOR' ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'
                    }`}
                  >
                    {selected.role || 'PROFILE'}
                  </span>
                  <span className="text-xs font-mono text-gray-400">{selected.id.slice(0, 8)}...</span>
                </div>
                <h2 className="text-lg font-semibold text-gray-900 mt-0.5">{selected.organizationName || 'Entity Review'}</h2>
              </div>
              <button onClick={() => setSelected(null)} className="p-2 hover:bg-gray-100 rounded-lg transition">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            {/* Drawer Content */}
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

              {/* Status and Profile Completion Row */}
              <div className="flex items-center justify-between bg-gray-50 p-3 rounded-xl border border-gray-100">
                <div className="space-y-0.5">
                  <span className="text-[11px] text-gray-500 block uppercase font-medium">Verification Status</span>
                  <StatusBadge status={selected.status} size="md" />
                </div>
                <div className="space-y-0.5 text-right">
                  <span className="text-[11px] text-gray-500 block uppercase font-medium">Profile Completion</span>
                  <StatusBadge status={selected.profileStatus || 'INCOMPLETE'} size="md" />
                </div>
              </div>

              {/* Category Profile Details */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-3 border border-gray-100">
                <h4 className="text-sm font-semibold text-gray-800 flex items-center gap-2">
                  <Building className="w-4 h-4 text-primary-600" />
                  Category Specific Profile
                </h4>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-gray-500 text-xs block">Category</span>
                    <p className="font-medium text-gray-800">
                      {CATEGORY_MAP[selected.category || selected.organizationType] || selected.organizationType || '—'}
                    </p>
                  </div>
                  {selected.businessType && (
                    <div>
                      <span className="text-gray-500 text-xs block">Business Type</span>
                      <p className="font-medium text-gray-800">{selected.businessType}</p>
                    </div>
                  )}
                  {selected.registrationNumber && (
                    <div>
                      <span className="text-gray-500 text-xs block">Reg / Trust #</span>
                      <p className="font-mono text-xs font-semibold text-gray-800">{selected.registrationNumber}</p>
                    </div>
                  )}
                  {selected.beneficiaryCapacity && (
                    <div>
                      <span className="text-gray-500 text-xs block">Serving Capacity</span>
                      <p className="font-medium text-gray-800">{selected.beneficiaryCapacity} persons</p>
                    </div>
                  )}
                </div>

                {selected.description && (
                  <div className="pt-2 border-t border-gray-200/60">
                    <span className="text-gray-500 text-xs block flex items-center gap-1">
                      <FileText className="w-3 h-3" /> Mission / Description
                    </span>
                    <p className="text-xs text-gray-700 mt-1 leading-relaxed">{selected.description}</p>
                  </div>
                )}

                {selected.eventInfo && (
                  <div className="pt-2 border-t border-gray-200/60">
                    <span className="text-gray-500 text-xs block flex items-center gap-1">
                      <Calendar className="w-3 h-3" /> Event Details
                    </span>
                    <p className="text-xs text-gray-700 mt-1 leading-relaxed">{selected.eventInfo}</p>
                  </div>
                )}
              </div>

              {/* Contact & Location Details */}
              <div className="bg-gray-50 rounded-xl p-4 space-y-3 border border-gray-100">
                <h4 className="text-sm font-semibold text-gray-800 flex items-center gap-2">
                  <Users className="w-4 h-4 text-primary-600" />
                  Representative & Operating Location
                </h4>
                <div className="space-y-2 text-sm text-gray-700">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-500 text-xs w-20">Contact:</span>
                    <span className="font-medium">{selected.contactPerson || '—'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Mail className="w-3.5 h-3.5 text-gray-400" />
                    <span>{selected.email || '—'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Phone className="w-3.5 h-3.5 text-gray-400" />
                    <span>{selected.phone || '—'}</span>
                  </div>
                  <div className="flex items-start gap-2 pt-2 border-t border-gray-200/60">
                    <MapPin className="w-3.5 h-3.5 text-gray-400 mt-0.5 flex-shrink-0" />
                    <div className="text-xs text-gray-600">
                      <p>{selected.address || 'Address not specified'}</p>
                      {selected.latitude !== undefined && selected.longitude !== undefined && (
                        <p className="text-[11px] text-gray-400 mt-0.5">
                          GPS: {selected.latitude.toFixed(4)}, {selected.longitude.toFixed(4)}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Photos Review */}
              {parsePhotos(selected.photos).length > 0 && (
                <div className="bg-gray-50 rounded-xl p-4 space-y-3 border border-gray-100">
                  <h4 className="text-sm font-semibold text-gray-800 flex items-center gap-2">
                    <ImageIcon className="w-4 h-4 text-primary-600" />
                    Submitted Photos ({parsePhotos(selected.photos).length})
                  </h4>
                  <div className="grid grid-cols-2 gap-2">
                    {parsePhotos(selected.photos).map((photoUrl, idx) => (
                      <a
                        key={idx}
                        href={photoUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="group relative block aspect-video rounded-lg overflow-hidden border border-gray-200 bg-gray-100 hover:opacity-90 transition"
                      >
                        <img
                          src={photoUrl}
                          alt={`Submission ${idx + 1}`}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            (e.target as HTMLElement).style.display = 'none';
                          }}
                        />
                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center text-white text-xs font-medium transition">
                          View Full Image
                        </div>
                      </a>
                    ))}
                  </div>
                </div>
              )}

              {/* History / Reasons Banner */}
              {(selected.rejectionReason || selected.actionRequiredNotes || selected.notes) && (
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-1">
                  <p className="text-xs font-semibold text-amber-900 uppercase">Verification History / Notes</p>
                  {selected.rejectionReason && (
                    <p className="text-xs text-red-800 font-medium">Rejection Reason: {selected.rejectionReason}</p>
                  )}
                  {selected.actionRequiredNotes && (
                    <p className="text-xs text-amber-800 font-medium">
                      Action Required Note: {selected.actionRequiredNotes}
                    </p>
                  )}
                  {selected.notes && selected.notes !== selected.actionRequiredNotes && selected.notes !== selected.rejectionReason && (
                    <p className="text-xs text-amber-700">{selected.notes}</p>
                  )}
                  {selected.verifiedAt && (
                    <p className="text-[10px] text-amber-600 pt-1">
                      Adjudicated: {new Date(selected.verifiedAt).toLocaleString('en-IN')}
                    </p>
                  )}
                </div>
              )}

              {/* Action Prompt Dialog when Request Info or Reject clicked */}
              {dialogAction && (
                <div className="p-4 bg-gray-50 border-2 border-primary-500/30 rounded-xl space-y-3">
                  <div className="flex items-center justify-between">
                    <h5 className="text-sm font-semibold text-gray-900">
                      {dialogAction === 'ACTION_REQUIRED' ? 'Request Additional Information' : 'Reject Application'}
                    </h5>
                    <button onClick={() => setDialogAction(null)} className="text-gray-400 hover:text-gray-600">
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  <p className="text-xs text-gray-500">
                    {dialogAction === 'ACTION_REQUIRED'
                      ? 'Specify clearly what information, documentation, or profile updates the user must provide.'
                      : 'Provide a reason for rejection. The user will be notified and can view this reason.'}
                  </p>
                  {dialogError && <p className="text-xs text-red-600">{dialogError}</p>}
                  <textarea
                    rows={3}
                    value={dialogNotes}
                    onChange={(e) => setDialogNotes(e.target.value)}
                    placeholder={
                      dialogAction === 'ACTION_REQUIRED'
                        ? 'e.g., Please provide a clear photo of the kitchen premises or update your operating address.'
                        : 'e.g., The registration credentials provided could not be verified with local authorities.'
                    }
                    className="w-full p-2.5 text-xs bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20"
                  />
                  <div className="flex gap-2 justify-end">
                    <button
                      onClick={() => setDialogAction(null)}
                      disabled={actionLoading}
                      className="px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded-lg transition"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(selected.id, dialogAction, dialogNotes.trim())}
                      disabled={actionLoading || !dialogNotes.trim()}
                      className={`px-4 py-1.5 text-xs font-semibold text-white rounded-lg transition disabled:opacity-50 ${
                        dialogAction === 'ACTION_REQUIRED'
                          ? 'bg-amber-600 hover:bg-amber-700'
                          : 'bg-red-600 hover:bg-red-700'
                      }`}
                    >
                      {actionLoading ? 'Saving...' : 'Submit Decision'}
                    </button>
                  </div>
                </div>
              )}

              {/* Main Adjudication Buttons */}
              {!dialogAction && (
                <div className="pt-2 space-y-2">
                  {selected.profileStatus !== 'COMPLETE' && (
                    <div className="p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-amber-800 text-xs flex items-center gap-2">
                      <HelpCircle className="w-4 h-4 flex-shrink-0 text-amber-600" />
                      <span>
                        Profile is incomplete. Mandatory organization and operating details must be provided before approval.
                      </span>
                    </div>
                  )}
                  <div className="flex flex-col sm:flex-row gap-2">
                    <button
                      onClick={() => handleUpdateStatus(selected.id, 'APPROVED', 'Verified by administrator')}
                      disabled={actionLoading || selected.status === 'APPROVED' || selected.profileStatus !== 'COMPLETE'}
                      className="flex-1 py-2.5 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition"
                      title={
                        selected.profileStatus !== 'COMPLETE'
                          ? 'Profile must be completed before approval'
                          : 'Approve Profile'
                      }
                    >
                      <CheckCircle className="w-4 h-4" /> Approve Profile
                    </button>

                    <button
                      onClick={() => openActionDialog('ACTION_REQUIRED')}
                      disabled={actionLoading || selected.status === 'ACTION_REQUIRED'}
                      className="flex-1 py-2.5 bg-white border border-amber-300 text-amber-700 rounded-lg text-sm font-medium hover:bg-amber-50 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                    >
                      <HelpCircle className="w-4 h-4" /> Request Info
                    </button>

                    <button
                      onClick={() => openActionDialog('REJECTED')}
                      disabled={actionLoading || selected.status === 'REJECTED'}
                      className="flex-1 py-2.5 bg-white border border-red-200 text-red-600 rounded-lg text-sm font-medium hover:bg-red-50 disabled:opacity-50 flex items-center justify-center gap-2 transition"
                    >
                      <XCircle className="w-4 h-4" /> Reject
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
