import { useState, useEffect, useCallback } from 'react';
import { Star, AlertCircle, RefreshCw, X } from 'lucide-react';
import DataTable from '../components/DataTable';
import type { Column } from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';

interface AdminRatingEntry {
  id: string;
  pickupId: string;
  donationId?: string;
  donationTitle?: string;
  raterUserId: string;
  raterName: string;
  raterRole: string;
  ratedUserId: string;
  ratedName: string;
  ratedRole: string;
  rating: number;
  feedback?: string;
  createdAt: string;
}

export default function RatingsPage() {
  const [ratings, setRatings] = useState<AdminRatingEntry[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);

  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [selectedRating, setSelectedRating] = useState<AdminRatingEntry | null>(null);

  const fetchRatings = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const params = {
        page: currentPage - 1,
        size: pageSize,
      };

      const res = await apiClient.get('/admin/ratings', { params });
      if (res.data && Array.isArray(res.data.content)) {
        setRatings(res.data.content);
        setTotalPages(res.data.totalPages ?? 1);
      } else {
        setRatings([]);
        setTotalPages(1);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load ratings from backend';
      setApiError(msg);
      setRatings([]);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  useEffect(() => {
    fetchRatings();
  }, [fetchRatings]);

  const renderStars = (count: number) => {
    return (
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((s) => (
          <Star
            key={s}
            className={`w-4 h-4 ${s <= count ? 'text-amber-500 fill-amber-500' : 'text-gray-300'}`}
          />
        ))}
        <span className="text-xs font-semibold text-gray-700 ml-1">{count}/5</span>
      </div>
    );
  };

  const columns: Column<AdminRatingEntry>[] = [
    {
      key: 'rating',
      label: 'Score',
      render: (_, r) => renderStars(r.rating),
    },
    {
      key: 'feedback',
      label: 'Feedback',
      render: (_, r) => (
        <span className="text-sm text-gray-700 truncate max-w-xs block" title={r.feedback || ''}>
          {r.feedback ? `"${r.feedback}"` : <span className="text-gray-400 italic">No text</span>}
        </span>
      ),
    },
    {
      key: 'raterName',
      label: 'Rater',
      render: (_, r) => (
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-900">{r.raterName}</span>
          <StatusBadge status={r.raterRole} />
        </div>
      ),
    },
    {
      key: 'ratedName',
      label: 'Rated User',
      render: (_, r) => (
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-900">{r.ratedName}</span>
          <StatusBadge status={r.ratedRole} />
        </div>
      ),
    },
    {
      key: 'donationTitle',
      label: 'Donation',
      render: (_, r) => (
        <span className="text-sm text-gray-800 font-medium">
          {r.donationTitle || <span className="font-mono text-xs text-gray-500">{r.pickupId.substring(0, 8)}...</span>}
        </span>
      ),
    },
    {
      key: 'createdAt',
      label: 'Submitted At',
      render: (_, r) => (
        <span className="text-xs text-gray-500 whitespace-nowrap">
          {r.createdAt ? new Date(r.createdAt).toLocaleString('en-IN') : 'N/A'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: '',
      render: (_, r) => (
        <button
          onClick={() => setSelectedRating(r)}
          className="text-xs font-medium text-emerald-600 hover:text-emerald-800 hover:underline"
        >
          View Details
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Star className="w-6 h-6 text-amber-500 fill-amber-500" />
            Ratings & Feedback
          </h1>
          <p className="text-sm text-gray-500">
            Post-pickup mutual ratings submitted by Donors and Receivers upon completed redistribution.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchRatings}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors shadow-sm disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Error state */}
      {apiError && (
        <div className="flex items-center justify-between p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
            <p className="text-sm text-red-800">{apiError}</p>
          </div>
          <button
            onClick={fetchRatings}
            className="text-sm font-medium text-red-700 hover:text-red-900 underline ml-4"
          >
            Retry
          </button>
        </div>
      )}

      {/* Ratings Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {loading && ratings.length === 0 ? (
          <div className="p-12 text-center text-sm text-gray-500">Loading ratings...</div>
        ) : (
          <DataTable
            columns={columns}
            data={ratings}
            emptyMessage="No ratings recorded yet. Completed pickups with submitted feedback will appear here."
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        )}
      </div>

      {/* Detail Modal */}
      {selectedRating && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto border border-gray-100">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-amber-50 flex items-center justify-center text-amber-600">
                  <Star className="w-5 h-5 fill-amber-500 text-amber-500" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">Rating Details</h3>
                  <p className="text-xs text-gray-500">ID: {selectedRating.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedRating(null)}
                className="text-gray-400 hover:text-gray-500 p-1 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-5 text-sm">
              {/* Score Display */}
              <div className="p-4 bg-amber-50/50 rounded-xl border border-amber-100 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-amber-800 uppercase tracking-wider block mb-1">
                    Transaction Rating
                  </span>
                  {renderStars(selectedRating.rating)}
                </div>
                <span className="text-2xl font-bold text-amber-600">{selectedRating.rating} / 5</span>
              </div>

              {/* Feedback */}
              <div>
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider block mb-1">
                  Written Feedback
                </label>
                <div className="p-3 bg-gray-50 rounded-lg border border-gray-200 text-gray-800 italic">
                  {selectedRating.feedback ? `"${selectedRating.feedback}"` : 'No written feedback was provided.'}
                </div>
              </div>

              {/* Participants */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 rounded-lg border border-gray-200">
                  <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider block mb-1">
                    Submitted By
                  </span>
                  <p className="font-semibold text-gray-900">{selectedRating.raterName}</p>
                  <div className="mt-1 flex items-center gap-1.5">
                    <StatusBadge status={selectedRating.raterRole} />
                  </div>
                  <p className="text-xs text-gray-400 mt-1 truncate">ID: {selectedRating.raterUserId}</p>
                </div>

                <div className="p-3 rounded-lg border border-gray-200">
                  <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider block mb-1">
                    Rated Participant
                  </span>
                  <p className="font-semibold text-gray-900">{selectedRating.ratedName}</p>
                  <div className="mt-1 flex items-center gap-1.5">
                    <StatusBadge status={selectedRating.ratedRole} />
                  </div>
                  <p className="text-xs text-gray-400 mt-1 truncate">ID: {selectedRating.ratedUserId}</p>
                </div>
              </div>

              {/* Operation Context */}
              <div className="space-y-2 pt-2 border-t border-gray-100 text-xs text-gray-600">
                <div className="flex justify-between py-1 border-b border-gray-50">
                  <span className="font-medium text-gray-500">Donation Title</span>
                  <span className="font-semibold text-gray-900">{selectedRating.donationTitle || 'N/A'}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-50">
                  <span className="font-medium text-gray-500">Pickup ID</span>
                  <span className="font-mono text-gray-800">{selectedRating.pickupId}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-50">
                  <span className="font-medium text-gray-500">Submitted At</span>
                  <span>{new Date(selectedRating.createdAt).toLocaleString('en-IN')}</span>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
                onClick={() => setSelectedRating(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors shadow-xs"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
