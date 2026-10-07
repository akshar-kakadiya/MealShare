import { useState, useEffect, useCallback } from 'react';
import {
  Package, FileText, Truck, Users, TrendingUp, Utensils,
  CheckCircle2, AlertTriangle, Activity, AlertCircle, RefreshCw
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import apiClient from '../api/client';

interface DashboardData {
  users: { total: number; donors: number; receivers: number; admins: number };
  donations: { total: number; available: number; requested: number; approved: number; completed: number; cancelled: number; expired: number; totalQuantityDonated: number; totalAllocatedQuantity: number; totalCompletedQuantity: number };
  requests: { total: number; pending: number; approved: number; rejected: number; withdrawn: number; completed: number };
  pickups: { total: number; assigned: number; pickedUp: number; completed: number; cancelled: number };
  donationStatusDistribution: Array<{ status: string; count: number; color: string }>;
  recentDonations: Array<{ id: string; title: string; donorName: string; quantity: number; unit: string; status: string; createdAt: string }>;
  pendingVerifications: Array<{ id: string; organizationName: string; organizationType: string; status: string; submittedAt: string }>;
  recentActivity: Array<{ id: number; action: string; description: string; user: string; timestamp: string }>;
  dailyActivity: Array<{ day: string; created: number; completed: number }>;
}

const EMPTY_DASHBOARD: DashboardData = {
  users: { total: 0, donors: 0, receivers: 0, admins: 0 },
  donations: { total: 0, available: 0, requested: 0, approved: 0, completed: 0, cancelled: 0, expired: 0, totalQuantityDonated: 0, totalAllocatedQuantity: 0, totalCompletedQuantity: 0 },
  requests: { total: 0, pending: 0, approved: 0, rejected: 0, withdrawn: 0, completed: 0 },
  pickups: { total: 0, assigned: 0, pickedUp: 0, completed: 0, cancelled: 0 },
  donationStatusDistribution: [],
  recentDonations: [],
  pendingVerifications: [],
  recentActivity: [],
  dailyActivity: [],
};

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const res = await apiClient.get('/admin/dashboard');
      if (res.data) {
        setData(res.data);
      } else {
        setData(EMPTY_DASHBOARD);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load dashboard data from backend';
      setApiError(msg);
      setData(EMPTY_DASHBOARD);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();
  }, [fetchDashboard]);

  const dashboard = data || EMPTY_DASHBOARD;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Admin Dashboard</h1>
          <p className="text-sm text-gray-500">Live operational metrics from PostgreSQL and Spring Boot backend</p>
        </div>
        <button
          onClick={fetchDashboard}
          disabled={loading}
          className="flex items-center gap-1.5 text-sm text-gray-600 hover:text-gray-900 px-3 py-1.5 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Refresh
        </button>
      </div>

      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend error: {apiError}</span>
          </div>
          <button
            onClick={fetchDashboard}
            className="px-2.5 py-1 bg-red-100 hover:bg-red-200 text-red-900 rounded font-medium text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {loading && !data ? (
        <div className="bg-white rounded-xl border border-gray-100 p-16 text-center">
          <RefreshCw className="w-8 h-8 text-primary-500 animate-spin mx-auto mb-3" />
          <p className="text-sm text-gray-500">Loading live operational metrics...</p>
        </div>
      ) : (
        <>
          {/* KPI Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
            <StatCard
              title="Total Donations"
              value={dashboard.donations.total}
              icon={Package}
              trend={`${dashboard.donations.available} active`}
              trendDirection="up"
            />
            <StatCard
              title="Active Donations"
              value={dashboard.donations.available}
              icon={TrendingUp}
              trend="Available"
              trendDirection="neutral"
              iconBg="bg-amber-50"
              iconColor="text-amber-600"
            />
            <StatCard
              title="Pending Requests"
              value={dashboard.requests.pending}
              icon={FileText}
              trend={`${dashboard.requests.total} total`}
              trendDirection="neutral"
              iconBg="bg-blue-50"
              iconColor="text-blue-600"
            />
            <StatCard
              title="Completed Pickups"
              value={dashboard.pickups.completed}
              icon={Truck}
              trend={`${dashboard.pickups.assigned} in transit`}
              trendDirection="up"
              iconBg="bg-violet-50"
              iconColor="text-violet-600"
            />
            <StatCard
              title="Food Donated"
              value={`${dashboard.donations.totalQuantityDonated}`}
              icon={Utensils}
              trend="Units registered"
              trendDirection="up"
              iconBg="bg-orange-50"
              iconColor="text-orange-600"
            />
            <StatCard
              title="Platform Users"
              value={dashboard.users.total}
              icon={Users}
              trend={`${dashboard.users.donors} donors, ${dashboard.users.receivers} receivers`}
              trendDirection="neutral"
              iconBg="bg-rose-50"
              iconColor="text-rose-600"
            />
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Donation Activity Chart */}
            <div className="lg:col-span-2 bg-white rounded-xl border border-gray-100 p-5">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-semibold text-gray-900">Donation Activity</h3>
                  <p className="text-xs text-gray-500 mt-0.5">7-day operational trajectory</p>
                </div>
                <div className="flex items-center gap-4 text-xs">
                  <div className="flex items-center gap-1.5">
                    <div className="w-2.5 h-2.5 rounded-sm bg-primary-500" />
                    <span className="text-gray-500">Created</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <div className="w-2.5 h-2.5 rounded-sm bg-primary-200" />
                    <span className="text-gray-500">Completed</span>
                  </div>
                </div>
              </div>
              {dashboard.dailyActivity.length === 0 ? (
                <div className="h-60 flex items-center justify-center text-sm text-gray-400">
                  No activity data recorded yet
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={dashboard.dailyActivity} barGap={4}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" vertical={false} />
                    <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                    <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                    <Tooltip
                      contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.05)' }}
                    />
                    <Bar dataKey="created" fill="#10b981" radius={[4, 4, 0, 0]} name="Created" />
                    <Bar dataKey="completed" fill="#a7f3d0" radius={[4, 4, 0, 0]} name="Completed" />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>

            {/* Status Distribution */}
            <div className="bg-white rounded-xl border border-gray-100 p-5">
              <h3 className="text-sm font-semibold text-gray-900 mb-1">Donation Status</h3>
              <p className="text-xs text-gray-500 mb-4">Current distribution</p>
              {dashboard.donationStatusDistribution.length === 0 || dashboard.donations.total === 0 ? (
                <div className="h-60 flex items-center justify-center text-sm text-gray-400">
                  No donations registered
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie
                      data={dashboard.donationStatusDistribution}
                      cx="50%"
                      cy="50%"
                      innerRadius={55}
                      outerRadius={80}
                      paddingAngle={3}
                      dataKey="count"
                      nameKey="status"
                    >
                      {dashboard.donationStatusDistribution.map((entry, index) => (
                        <Cell key={index} fill={entry.color || '#10b981'} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: any, name: any) => [`${value} donations`, name]}
                      contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }}
                    />
                    <Legend
                      iconType="circle"
                      iconSize={8}
                      formatter={(value) => <span className="text-xs text-gray-600">{value}</span>}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Bottom Row */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Recent Donations */}
            <div className="lg:col-span-2 bg-white rounded-xl border border-gray-100">
              <div className="px-5 py-4 border-b border-gray-50 flex items-center justify-between">
                <h3 className="text-sm font-semibold text-gray-900">Recent Donations</h3>
                <a href="/admin/donations" className="text-xs font-medium text-primary-600 hover:text-primary-700">
                  View all
                </a>
              </div>
              {dashboard.recentDonations.length === 0 ? (
                <div className="p-8 text-center text-sm text-gray-400">No donations registered yet</div>
              ) : (
                <div className="divide-y divide-gray-50">
                  {dashboard.recentDonations.map((d) => (
                    <div key={d.id} className="px-5 py-3 flex items-center gap-4 hover:bg-gray-50/50">
                      <div className="w-9 h-9 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                        <Package className="w-4 h-4 text-primary-600" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-800 truncate">{d.title}</p>
                        <p className="text-xs text-gray-500">{d.donorName} • {d.quantity} {d.unit}</p>
                      </div>
                      <StatusBadge status={d.status} />
                      <span className="text-xs font-mono text-gray-400 hidden sm:block">{d.id.slice(0, 8)}...</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Right Column: Pending Verification + Activity */}
            <div className="space-y-6">
              {/* Pending Verifications */}
              <div className="bg-white rounded-xl border border-gray-100">
                <div className="px-5 py-4 border-b border-gray-50 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-semibold text-gray-900">Pending Verification</h3>
                    {dashboard.pendingVerifications.length > 0 && (
                      <span className="px-1.5 py-0.5 text-[10px] font-bold bg-amber-100 text-amber-700 rounded-full">
                        {dashboard.pendingVerifications.length}
                      </span>
                    )}
                  </div>
                  <a href="/admin/verification" className="text-xs font-medium text-primary-600 hover:text-primary-700">
                    View all
                  </a>
                </div>
                {dashboard.pendingVerifications.length === 0 ? (
                  <div className="p-5 text-center text-sm text-gray-500">
                    <CheckCircle2 className="w-6 h-6 text-gray-300 mx-auto mb-1" />
                    No pending verifications
                  </div>
                ) : (
                  <div className="divide-y divide-gray-50">
                    {dashboard.pendingVerifications.map((v) => (
                      <div key={v.id} className="px-5 py-3 flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-amber-50 flex items-center justify-center flex-shrink-0">
                          <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-800 truncate">{v.organizationName}</p>
                          <p className="text-xs text-gray-500">{v.organizationType}</p>
                        </div>
                        <StatusBadge status={v.status} />
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Recent Activity */}
              <div className="bg-white rounded-xl border border-gray-100">
                <div className="px-5 py-4 border-b border-gray-50">
                  <h3 className="text-sm font-semibold text-gray-900">Recent Activity</h3>
                </div>
                {dashboard.recentActivity.length === 0 ? (
                  <div className="p-5 text-center text-sm text-gray-400">No recent activity logs</div>
                ) : (
                  <div className="p-4">
                    <div className="relative">
                      <div className="absolute left-3.5 top-0 bottom-0 w-px bg-gray-100" />
                      {dashboard.recentActivity.map((a, idx) => (
                        <div key={idx} className="relative flex gap-3 pb-4 last:pb-0">
                          <div className="w-7 h-7 rounded-full bg-gray-50 border-2 border-white flex items-center justify-center flex-shrink-0 z-10">
                            <Activity className="w-3 h-3 text-gray-400" />
                          </div>
                          <div className="flex-1 min-w-0 pt-0.5">
                            <p className="text-xs font-medium text-gray-700">{a.action}</p>
                            <p className="text-xs text-gray-500 truncate">{a.description}</p>
                            <p className="text-[10px] text-gray-400 mt-0.5">{formatTimeAgo(a.timestamp)}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function formatTimeAgo(timestamp: string): string {
  if (!timestamp) return '';
  const diff = Date.now() - new Date(timestamp).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}
