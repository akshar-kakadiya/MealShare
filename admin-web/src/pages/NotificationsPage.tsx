import { useState, useMemo, useEffect, useCallback } from 'react';
import { Package, FileText, Truck, ShieldCheck, Zap, Bell, CheckCheck, Mail, MailOpen, AlertCircle, RefreshCw } from 'lucide-react';
import apiClient from '../api/client';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'donation' | 'request' | 'pickup' | 'verification' | 'system';
  entityId?: string;
  isRead: boolean;
  createdAt: string;
}

const TYPE_ICONS: Record<string, { icon: React.ElementType; bg: string; color: string }> = {
  donation: { icon: Package, bg: 'bg-emerald-50', color: 'text-emerald-600' },
  request: { icon: FileText, bg: 'bg-blue-50', color: 'text-blue-600' },
  pickup: { icon: Truck, bg: 'bg-violet-50', color: 'text-violet-600' },
  verification: { icon: ShieldCheck, bg: 'bg-amber-50', color: 'text-amber-600' },
  system: { icon: Zap, bg: 'bg-gray-100', color: 'text-gray-600' },
};

const formatTime = (timestamp: string): string => {
  const diff = Date.now() - new Date(timestamp).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
};

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    setApiError(null);
    try {
      const res = await apiClient.get('/notifications');
      if (res.data && res.data.content && Array.isArray(res.data.content)) {
        const mapped: NotificationItem[] = res.data.content.map((n: any) => {
          let mappedType: NotificationItem['type'] = 'system';
          const rawType = (n.type || '').toUpperCase();
          if (rawType.includes('REQUEST')) mappedType = 'request';
          else if (rawType.includes('PICKUP') || rawType.includes('OTP')) mappedType = 'pickup';
          else if (rawType.includes('DONATION')) mappedType = 'donation';
          else if (rawType.includes('VERIF')) mappedType = 'verification';

          return {
            id: n.id,
            title: n.title,
            message: n.message,
            type: mappedType,
            entityId: n.entityId,
            isRead: Boolean(n.read),
            createdAt: n.createdAt || new Date().toISOString(),
          };
        });
        setNotifications(mapped);
      } else {
        setNotifications([]);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Could not load notifications from backend';
      setApiError(msg);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const filtered = useMemo(() => {
    if (filter === 'unread') return notifications.filter((n) => !n.isRead);
    return notifications;
  }, [notifications, filter]);

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const markAsRead = async (id: string) => {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
    try {
      await apiClient.patch(`/notifications/${id}/read`);
    } catch (err) {
      console.error('Failed to mark notification as read on backend', err);
    }
  };

  const markAllAsRead = async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    try {
      await apiClient.post('/notifications/read-all');
    } catch (err) {
      console.error('Failed to mark all as read on backend', err);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">Notifications</h1>
          <p className="text-sm text-gray-500">{unreadCount} unread notification{unreadCount !== 1 ? 's' : ''}</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchNotifications}
            disabled={loading}
            className="flex items-center gap-1.5 text-sm text-gray-600 hover:text-gray-900 px-3 py-1.5 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Refresh
          </button>
          {unreadCount > 0 && (
            <button onClick={markAllAsRead} className="flex items-center gap-1.5 text-sm text-primary-600 hover:text-primary-700 font-medium">
              <CheckCheck className="w-4 h-4" /> Mark all as read
            </button>
          )}
        </div>
      </div>

      {apiError && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg text-sm flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
            <span>Backend error: {apiError}</span>
          </div>
          <button
            onClick={fetchNotifications}
            className="px-2.5 py-1 bg-red-100 hover:bg-red-200 text-red-900 rounded font-medium text-xs transition"
          >
            Retry
          </button>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setFilter('all')}
          className={`px-3 py-1.5 text-xs font-medium rounded-lg transition ${
            filter === 'all' ? 'bg-primary-50 text-primary-700 font-semibold' : 'text-gray-500 hover:bg-gray-100'
          }`}
        >
          All ({notifications.length})
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={`px-3 py-1.5 text-xs font-medium rounded-lg transition ${
            filter === 'unread' ? 'bg-primary-50 text-primary-700 font-semibold' : 'text-gray-500 hover:bg-gray-100'
          }`}
        >
          Unread ({unreadCount})
        </button>
      </div>

      {/* Notifications List */}
      <div className="bg-white rounded-xl border border-gray-100 divide-y divide-gray-50">
        {filtered.length === 0 ? (
          <div className="p-12 text-center">
            <Bell className="w-8 h-8 text-gray-300 mx-auto mb-2" />
            <p className="text-sm font-medium text-gray-500">No notifications</p>
            <p className="text-xs text-gray-400 mt-0.5">
              {filter === 'unread' ? 'You are all caught up!' : 'No notifications found'}
            </p>
          </div>
        ) : (
          filtered.map((n) => {
            const typeConfig = TYPE_ICONS[n.type] || TYPE_ICONS.system;
            const IconComponent = typeConfig.icon;
            return (
              <div
                key={n.id}
                className={`p-4 flex items-start gap-4 transition hover:bg-gray-50/50 ${
                  !n.isRead ? 'bg-primary-50/30' : ''
                }`}
              >
                <div className={`w-9 h-9 rounded-lg ${typeConfig.bg} flex items-center justify-center flex-shrink-0 mt-0.5`}>
                  <IconComponent className={`w-4 h-4 ${typeConfig.color}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className={`text-sm ${!n.isRead ? 'font-semibold text-gray-900' : 'font-medium text-gray-700'}`}>
                      {n.title}
                    </p>
                    {!n.isRead && <span className="w-2 h-2 rounded-full bg-primary-500 flex-shrink-0" />}
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5">{n.message}</p>
                  <p className="text-[10px] text-gray-400 mt-1">{formatTime(n.createdAt)}</p>
                </div>
                <div className="flex items-center gap-1">
                  {!n.isRead ? (
                    <button
                      onClick={() => markAsRead(n.id)}
                      title="Mark as read"
                      className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                    >
                      <MailOpen className="w-4 h-4" />
                    </button>
                  ) : (
                    <span title="Read" className="p-1.5 text-gray-300">
                      <Mail className="w-4 h-4" />
                    </span>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
