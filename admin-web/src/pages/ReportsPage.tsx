import { useState, useEffect, useCallback } from 'react';
import {
  BarChart3, TrendingUp, Users, Package, Truck, Utensils, Download, AlertCircle, RefreshCw
} from 'lucide-react';
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';
import StatCard from '../components/StatCard';
import apiClient from '../api/client';

interface ReportSummary {
  totalDonations: number;
  completedDonations: number;
  activeDonations: number;
  totalQuantityDonated: number;
  totalAllocatedQuantity: number;
  totalCompletedQuantity: number;
  totalRequests: number;
  pendingRequests: number;
  completedPickups: number;
  activeDonorsCount: number;
  activeReceiversCount: number;
  statusDistribution: Array<{ status: string; count: number; color: string }>;
  topDonors: Array<{ donorName: string; donationsCount: number; totalQuantity: number }>;
  monthlyData: Array<{ month: string; donations: number; pickups: number; meals: number }>;
}

export default function ReportsPage() {
  const [data, setData] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const fetchReport = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const res = await apiClient.get('/admin/reports/summary');
      if (res.data) {
        setData(res.data);
      } else {
        setData(null);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load reports summary from backend';
      setApiError(msg);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const handleExport = () => {
    if (!data) return;
    const reportJson = JSON.stringify(data, null, 2);
    const blob = new Blob([reportJson], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mealshare-operational-report-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const effectiveData: ReportSummary = data || {
    totalDonations: 0,
    completedDonations: 0,
    activeDonations: 0,
    totalQuantityDonated: 0,
    totalAllocatedQuantity: 0,
    totalCompletedQuantity: 0,
    totalRequests: 0,
    pendingRequests: 0,
    completedPickups: 0,
    activeDonorsCount: 0,
    activeReceiversCount: 0,
    statusDistribution: [],
    topDonors: [],
    monthlyData: [],
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Reports & Analytics</h1>
          <p className="text-sm text-gray-500">
            Real operational metrics and platform analytics from PostgreSQL database
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => fetchReport()}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-gray-700 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={handleExport}
            disabled={!data}
            className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition disabled:opacity-50"
          >
            <Download className="w-4 h-4" /> Export Report (JSON)
          </button>
        </div>
      </div>

      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend unavailable: {apiError}. Analytics metrics could not be loaded.</span>
          </div>
          <button
            onClick={() => fetchReport()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 text-red-800 font-medium rounded text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Real KPIs supported by database queries */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          title="Total Donations"
          value={effectiveData.totalDonations}
          icon={Package}
          trend={`${effectiveData.activeDonations} active currently`}
          trendDirection="neutral"
        />
        <StatCard
          title="Completed Pickups"
          value={effectiveData.completedPickups}
          icon={Truck}
          trend="Delivered to receivers"
          trendDirection="up"
          iconBg="bg-violet-50"
          iconColor="text-violet-600"
        />
        <StatCard
          title="Food Completed"
          value={`${effectiveData.totalCompletedQuantity.toLocaleString()} units`}
          icon={Utensils}
          trend={`${effectiveData.totalAllocatedQuantity.toLocaleString()} allocated`}
          trendDirection="up"
          iconBg="bg-orange-50"
          iconColor="text-orange-600"
        />
        <StatCard
          title="Active Receivers"
          value={effectiveData.activeReceiversCount}
          icon={Users}
          trend="Registered receivers"
          trendDirection="neutral"
          iconBg="bg-rose-50"
          iconColor="text-rose-600"
        />
        <StatCard
          title="Active Donors"
          value={effectiveData.activeDonorsCount}
          icon={TrendingUp}
          trend="Contributing partners"
          trendDirection="neutral"
          iconBg="bg-emerald-50"
          iconColor="text-emerald-600"
        />
        <StatCard
          title="Receiver Orgs"
          value={effectiveData.activeReceiversCount}
          icon={BarChart3}
          trend="Community organizations"
          trendDirection="neutral"
          iconBg="bg-blue-50"
          iconColor="text-blue-600"
        />
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Donations Over Time */}
        <div className="bg-white rounded-xl border border-gray-100 p-5">
          <h3 className="text-sm font-semibold text-gray-900 mb-1">Donations & Pickups Over Time</h3>
          <p className="text-xs text-gray-500 mb-4">Monthly completed activity</p>
          {effectiveData.monthlyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={effectiveData.monthlyData}>
                <defs>
                  <linearGradient id="donGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#10b981" stopOpacity={0.2} />
                    <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" vertical={false} />
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }} />
                <Area type="monotone" dataKey="donations" stroke="#10b981" fill="url(#donGrad)" strokeWidth={2} name="Donations" />
                <Area type="monotone" dataKey="pickups" stroke="#8b5cf6" fill="transparent" strokeWidth={2} strokeDasharray="5 5" name="Pickups" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[260px] flex items-center justify-center text-sm text-gray-400">
              No historical trend data available in current window
            </div>
          )}
        </div>

        {/* Food Quantity Completed */}
        <div className="bg-white rounded-xl border border-gray-100 p-5">
          <h3 className="text-sm font-semibold text-gray-900 mb-1">Food Volume Completed</h3>
          <p className="text-xs text-gray-500 mb-4">Quantity of food successfully delivered</p>
          {effectiveData.monthlyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={effectiveData.monthlyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" vertical={false} />
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }} />
                <Bar dataKey="meals" fill="#f97316" radius={[4, 4, 0, 0]} name="Completed Qty (Units)" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[260px] flex items-center justify-center text-sm text-gray-400">
              No food volume distribution available
            </div>
          )}
        </div>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Donation Status Distribution */}
        <div className="bg-white rounded-xl border border-gray-100 p-5">
          <h3 className="text-sm font-semibold text-gray-900 mb-1">Donation Status Distribution</h3>
          <p className="text-xs text-gray-500 mb-4">All-time database breakdown</p>
          {effectiveData.statusDistribution.length > 0 ? (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={effectiveData.statusDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={3}
                  dataKey="count"
                  nameKey="status"
                >
                  {effectiveData.statusDistribution.map((entry, i) => (
                    <Cell key={i} fill={entry.color || '#10b981'} />
                  ))}
                </Pie>
                <Tooltip formatter={(value: any, name: any) => [`${value} donations`, name]} contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }} />
                <Legend iconType="circle" iconSize={8} formatter={(value) => <span className="text-xs text-gray-600">{value}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[260px] flex items-center justify-center text-sm text-gray-400">
              No donation status data available
            </div>
          )}
        </div>

        {/* Top Donor Contributions */}
        <div className="bg-white rounded-xl border border-gray-100 p-5">
          <h3 className="text-sm font-semibold text-gray-900 mb-1">Top Donor Contributions</h3>
          <p className="text-xs text-gray-500 mb-4">Ranked by total donations created</p>
          {effectiveData.topDonors.length > 0 ? (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={effectiveData.topDonors} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" horizontal={false} />
                <XAxis type="number" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <YAxis
                  type="category"
                  dataKey="donorName"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 11, fill: '#6b7280' }}
                  width={130}
                />
                <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }} />
                <Bar dataKey="donationsCount" fill="#10b981" radius={[0, 4, 4, 0]} name="Donations Count" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[260px] flex items-center justify-center text-sm text-gray-400">
              No donor contributions registered yet
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
