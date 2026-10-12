
interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

const STATUS_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  // Donation statuses
  AVAILABLE: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Available' },
  REQUESTED: { bg: 'bg-amber-50', text: 'text-amber-700', label: 'Requested' },
  APPROVED: { bg: 'bg-blue-50', text: 'text-blue-700', label: 'Approved' },
  PICKUP_ASSIGNED: { bg: 'bg-violet-50', text: 'text-violet-700', label: 'Pickup Assigned' },
  PICKED_UP: { bg: 'bg-indigo-50', text: 'text-indigo-700', label: 'Picked Up' },
  COMPLETED: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Completed' },
  EXPIRED: { bg: 'bg-gray-100', text: 'text-gray-600', label: 'Expired' },
  CANCELLED: { bg: 'bg-gray-100', text: 'text-gray-600', label: 'Cancelled' },
  // Request statuses
  PENDING: { bg: 'bg-amber-50', text: 'text-amber-700', label: 'Pending' },
  REJECTED: { bg: 'bg-red-50', text: 'text-red-700', label: 'Rejected' },
  WITHDRAWN: { bg: 'bg-gray-100', text: 'text-gray-600', label: 'Withdrawn' },
  // Pickup statuses
  PICKUP_PENDING: { bg: 'bg-amber-50', text: 'text-amber-700', label: 'Pickup Pending' },
  ASSIGNED: { bg: 'bg-blue-50', text: 'text-blue-700', label: 'Assigned' },
  EN_ROUTE_TO_PICKUP: { bg: 'bg-blue-50', text: 'text-blue-700', label: 'En Route to Pickup' },
  EN_ROUTE: { bg: 'bg-violet-50', text: 'text-violet-700', label: 'En Route' },
  AT_PICKUP: { bg: 'bg-purple-50', text: 'text-purple-700', label: 'At Pickup' },
  OTP_VERIFIED: { bg: 'bg-indigo-50', text: 'text-indigo-700', label: 'OTP Verified' },
  IN_TRANSIT: { bg: 'bg-cyan-50', text: 'text-cyan-700', label: 'In Transit' },
  DELIVERED: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Delivered' },
  FAILED: { bg: 'bg-red-50', text: 'text-red-700', label: 'Failed' },
  // User statuses
  ACTIVE: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Active' },
  INACTIVE: { bg: 'bg-gray-100', text: 'text-gray-600', label: 'Inactive' },
  SUSPENDED: { bg: 'bg-red-50', text: 'text-red-700', label: 'Suspended' },
  // Verification statuses
  MORE_INFO_NEEDED: { bg: 'bg-amber-50', text: 'text-amber-700', label: 'More Info Needed' },
  ACTION_REQUIRED: { bg: 'bg-orange-50', text: 'text-orange-700', label: 'Action Required' },
  COMPLETE: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Complete' },
  INCOMPLETE: { bg: 'bg-amber-50', text: 'text-amber-700', label: 'Incomplete' },
  // Roles
  DONOR: { bg: 'bg-emerald-50', text: 'text-emerald-700', label: 'Donor' },
  RECEIVER: { bg: 'bg-blue-50', text: 'text-blue-700', label: 'Receiver' },
  ADMIN: { bg: 'bg-gray-100', text: 'text-gray-700', label: 'Admin' },
};

export default function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  const normalized = status?.toUpperCase().replace(/[\s-]+/g, '_') || '';
  const style = STATUS_STYLES[normalized] || { bg: 'bg-gray-100', text: 'text-gray-600', label: status };
  const sizeClass = size === 'md' ? 'px-3 py-1 text-sm' : 'px-2 py-0.5 text-xs';
  return (
    <span className={`inline-flex items-center font-medium rounded-full ${style.bg} ${style.text} ${sizeClass}`}>
      {style.label}
    </span>
  );
}
