import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Package, FileText, Truck,
  Users, ShieldCheck, BarChart3, Bell, Settings,
  Leaf, ScrollText, Star,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface MenuItem {
  name: string;
  path: string;
  icon: React.ElementType;
}

interface MenuSection {
  label: string;
  items: MenuItem[];
}

const MENU_SECTIONS: MenuSection[] = [
  {
    label: 'Overview',
    items: [
      { name: 'Dashboard', path: '/admin', icon: LayoutDashboard },
    ],
  },
  {
    label: 'Operations',
    items: [
      { name: 'Donations', path: '/admin/donations', icon: Package },
      { name: 'Requests', path: '/admin/requests', icon: FileText },
      { name: 'Pickups', path: '/admin/pickups', icon: Truck },
    ],
  },
  {
    label: 'People',
    items: [
      { name: 'Users', path: '/admin/users', icon: Users },
    ],
  },
  {
    label: 'Trust & Safety',
    items: [
      { name: 'Verification', path: '/admin/verification', icon: ShieldCheck },
      { name: 'Audit Logs', path: '/admin/audit-logs', icon: ScrollText },
      { name: 'Ratings & Feedback', path: '/admin/ratings', icon: Star },
    ],
  },
  {
    label: 'Insights',
    items: [
      { name: 'Reports', path: '/admin/reports', icon: BarChart3 },
    ],
  },
  {
    label: 'System',
    items: [
      { name: 'Notifications', path: '/admin/notifications', icon: Bell },
      { name: 'Settings', path: '/admin/settings', icon: Settings },
    ],
  },
];

export default function Sidebar() {
  const location = useLocation();
  const { mealshareUser } = useAuth();

  const isActive = (path: string) => {
    if (path === '/admin') return location.pathname === '/admin';
    return location.pathname.startsWith(path);
  };

  const displayName = mealshareUser?.name || 'Platform Administrator';
  const displayEmail = mealshareUser?.email || 'admin@mealshare.org';
  const initial = displayName[0]?.toUpperCase() || 'A';

  return (
    <aside className="w-60 bg-white border-r border-gray-100 h-full flex flex-col flex-shrink-0">
      {/* Brand */}
      <div className="px-5 py-4 border-b border-gray-100">
        <Link to="/admin" className="flex items-center gap-2.5">
          <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
            <Leaf className="w-4.5 h-4.5 text-white" />
          </div>
          <div>
            <span className="text-base font-bold text-gray-900">MealShare</span>
            <span className="text-[10px] font-medium text-gray-400 ml-1.5 uppercase">Admin</span>
          </div>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-3 px-3">
        {MENU_SECTIONS.map((section) => (
          <div key={section.label} className="mb-1">
            <p className="px-3 pt-3 pb-1.5 text-[10px] font-semibold text-gray-400 uppercase tracking-widest">
              {section.label}
            </p>
            {section.items.map((item) => {
              const active = isActive(item.path);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors mb-0.5 ${
                    active
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`}
                >
                  <item.icon className={`w-[18px] h-[18px] flex-shrink-0 ${active ? 'text-primary-600' : 'text-gray-400'}`} />
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      {/* Footer with Authenticated User Profile */}
      <div className="px-4 py-3 border-t border-gray-100">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-bold flex-shrink-0">
            {initial}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-800 truncate">{displayName}</p>
            <p className="text-xs text-gray-400 truncate">{displayEmail}</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
