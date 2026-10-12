import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Search, Bell, LogOut } from 'lucide-react';
import apiClient from '../api/client';
import { useAuth } from '../contexts/AuthContext';

const PAGE_TITLES: Record<string, string> = {
  '/admin': 'Dashboard',
  '/admin/donations': 'Donations',
  '/admin/requests': 'Requests',
  '/admin/pickups': 'Pickups',
  '/admin/users': 'Users',
  '/admin/verification': 'Verification',
  '/admin/audit-logs': 'System Audit Logs',
  '/admin/reports': 'Reports & Analytics',
  '/admin/notifications': 'Notifications',
  '/admin/settings': 'Settings',
};

export default function TopBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mealshareUser, signOut } = useAuth();
  const title = PAGE_TITLES[location.pathname] || 'MealShare Admin';
  const [unreadCount, setUnreadCount] = useState<number>(0);

  useEffect(() => {
    let isMounted = true;
    apiClient.get('/notifications/unread-count')
      .then((res) => {
        if (isMounted && res.data && typeof res.data.unreadCount === 'number') {
          setUnreadCount(res.data.unreadCount);
        }
      })
      .catch(() => {
        // Leave unread count as is on network blip
      });
    return () => {
      isMounted = false;
    };
  }, [location.pathname]);

  const handleSignOut = async () => {
    await signOut();
    navigate('/', { replace: true });
  };

  const displayName = mealshareUser?.name || 'Platform Administrator';
  const displayRole = mealshareUser?.role ? `ROLE_${mealshareUser.role}` : 'ROLE_ADMIN';
  const initial = displayName[0]?.toUpperCase() || 'A';

  return (
    <header className="bg-white border-b border-gray-100 h-14 flex items-center justify-between px-6 flex-shrink-0">
      <h1 className="text-lg font-semibold text-gray-900">{title}</h1>

      <div className="flex items-center gap-3">
        {/* Search */}
        <div className="relative hidden md:block">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search..."
            className="w-56 pl-9 pr-3 py-1.5 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 transition"
          />
        </div>

        {/* Notifications */}
        <button
          onClick={() => navigate('/admin/notifications')}
          aria-label="View notifications"
          className="relative p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-lg transition"
        >
          <Bell className="w-5 h-5" />
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 min-w-[16px] h-4 px-1 bg-red-500 rounded-full text-white text-[10px] font-bold flex items-center justify-center">
              {unreadCount}
            </span>
          )}
        </button>

        {/* Divider */}
        <div className="h-6 w-px bg-gray-200" />

        {/* Profile */}
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-bold">
            {initial}
          </div>
          <div className="hidden sm:block">
            <p className="text-sm font-medium text-gray-700 leading-tight">{displayName}</p>
            <p className="text-[11px] text-gray-400 leading-tight">{displayRole}</p>
          </div>
        </div>

        {/* Sign Out Button */}
        <button
          onClick={handleSignOut}
          title="Sign out of Admin Portal"
          aria-label="Sign out"
          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition ml-1"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
}
