import type { LucideIcon } from 'lucide-react';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: string;
  trendDirection?: 'up' | 'down' | 'neutral';
  iconBg?: string;
  iconColor?: string;
}

export default function StatCard({
  title,
  value,
  icon: Icon,
  trend,
  trendDirection = 'up',
  iconBg = 'bg-primary-50',
  iconColor = 'text-primary-600',
}: StatCardProps) {
  return (
    <div className="bg-white rounded-xl border border-gray-100 p-5 hover:shadow-sm transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          {trend && (
            <div className="flex items-center mt-2 gap-1">
              {trendDirection === 'up' && <TrendingUp className="w-3.5 h-3.5 text-emerald-500" />}
              {trendDirection === 'down' && <TrendingDown className="w-3.5 h-3.5 text-red-500" />}
              <span className={`text-xs font-medium ${
                trendDirection === 'up' ? 'text-emerald-600' :
                trendDirection === 'down' ? 'text-red-600' : 'text-gray-500'
              }`}>{trend}</span>
            </div>
          )}
        </div>
        <div className={`w-11 h-11 rounded-lg ${iconBg} flex items-center justify-center flex-shrink-0`}>
          <Icon className={`w-5 h-5 ${iconColor}`} />
        </div>
      </div>
    </div>
  );
}
