import { AlertCircle, CheckCircle, Info, XCircle } from 'lucide-react';
import type { ReactNode } from 'react';

interface AlertProps {
  type: 'success' | 'error' | 'warning' | 'info';
  children: ReactNode;
  className?: string;
}

const styles = {
  success: { bg: 'bg-emerald-50 border-emerald-200 text-emerald-800', Icon: CheckCircle },
  error:   { bg: 'bg-red-50 border-red-200 text-red-800',           Icon: XCircle },
  warning: { bg: 'bg-amber-50 border-amber-200 text-amber-800',     Icon: AlertCircle },
  info:    { bg: 'bg-blue-50 border-blue-200 text-blue-800',         Icon: Info },
} as const;

export function Alert({ type, children, className = '' }: AlertProps) {
  const { bg, Icon } = styles[type];

  return (
    <div className={`flex items-start gap-3 px-4 py-3 rounded-lg border ${bg} ${className}`}>
      <Icon size={18} className="mt-0.5 shrink-0" />
      <div className="text-sm">{children}</div>
    </div>
  );
}
