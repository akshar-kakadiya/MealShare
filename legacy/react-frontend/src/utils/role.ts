import type { UserRole } from '../types/auth.types';

/** Map a UserRole to its dashboard path. */
export function getRoleDashboardPath(role: UserRole): string {
  switch (role) {
    case 'Donor':
      return '/donor';
    case 'Receiver_Org':
      return '/receiver';
    case 'Receiver_Vol':
      return '/volunteer';
    case 'Admin':
      return '/admin';
    default:
      return '/login';
  }
}

/** Human-readable label for a role. */
export function getRoleLabel(role: UserRole): string {
  switch (role) {
    case 'Donor':
      return 'Donor';
    case 'Receiver_Org':
      return 'Receiver Organization';
    case 'Receiver_Vol':
      return 'Volunteer';
    case 'Admin':
      return 'Administrator';
    default:
      return role;
  }
}
