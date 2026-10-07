// ═══════════════════════════════════════════════════════
// TYPES
// ═══════════════════════════════════════════════════════

export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: 'DONOR' | 'RECEIVER' | 'ADMIN';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  organization?: string;
  organizationType?: string;
  verified: boolean;
  joinedAt: string;
  lastActive: string;
  totalDonations?: number;
  totalRequests?: number;
  totalPickups?: number;
}

export interface Donation {
  id: string;
  donorId: string;
  donorName: string;
  foodItem: string;
  description: string;
  category: string;
  quantity: number;
  unit: string;
  location: string;
  consumeBefore: string;
  storageCondition: string;
  hasAllergens: boolean;
  allergenInfo: string;
  status: 'AVAILABLE' | 'REQUESTED' | 'APPROVED' | 'PICKUP_ASSIGNED' | 'PICKED_UP' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export interface FoodRequest {
  id: string;
  donationId: string;
  donationItem: string;
  receiverId: string;
  receiverName: string;
  receiverOrg: string;
  quantity: number;
  unit: string;
  purpose: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'COMPLETED';
  requestedAt: string;
  updatedAt: string;
}

export interface Pickup {
  id: string;
  donationId: string;
  donationItem: string;
  donorName: string;
  receiverName: string;
  pickupAddress: string;
  deliveryAddress: string;
  scheduledTime: string;
  otpCode: string;
  status: 'ASSIGNED' | 'EN_ROUTE' | 'OTP_VERIFIED' | 'PICKED_UP' | 'DELIVERED' | 'FAILED';
  createdAt: string;
}

export interface Verification {
  id: string;
  organizationName: string;
  organizationType: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  documents: string[];
  submittedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_NEEDED';
  reviewNotes?: string;
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'donation' | 'request' | 'pickup' | 'verification' | 'system';
  entityId?: string;
  isRead: boolean;
  createdAt: string;
}

export interface ActivityItem {
  id: string;
  action: string;
  description: string;
  user: string;
  entityType: string;
  entityId: string;
  timestamp: string;
}

// ═══════════════════════════════════════════════════════
// MOCK DATA
// ═══════════════════════════════════════════════════════

export const mockUsers: User[] = [
  { id: 'u1', name: 'FreshBite Restaurant', email: 'freshbite@example.com', phone: '+91 98765 43210', role: 'DONOR', status: 'ACTIVE', organization: 'FreshBite Restaurant', organizationType: 'Restaurant', verified: true, joinedAt: '2026-03-15', lastActive: '2026-09-18', totalDonations: 48 },
  { id: 'u2', name: 'Hope Foundation', email: 'hope@example.com', phone: '+91 91234 56789', role: 'RECEIVER', status: 'ACTIVE', organization: 'Hope Foundation', organizationType: 'NGO', verified: true, joinedAt: '2026-01-10', lastActive: '2026-09-18', totalRequests: 32 },
  { id: 'u3', name: 'Rahul Sharma', email: 'rahul.s@example.com', phone: '+91 87654 32109', role: 'RECEIVER', status: 'ACTIVE', organization: 'Community Seva', organizationType: 'Community', verified: true, joinedAt: '2026-04-01', lastActive: '2026-09-17', totalRequests: 18 },
  { id: 'u4', name: 'Green Garden Café', email: 'greengarden@example.com', phone: '+91 98123 45678', role: 'DONOR', status: 'ACTIVE', organization: 'Green Garden Café', organizationType: 'Café', verified: true, joinedAt: '2026-02-20', lastActive: '2026-09-18', totalDonations: 35 },
  { id: 'u5', name: 'Shelter Home Trust', email: 'shelter@example.com', phone: '+91 99887 76655', role: 'RECEIVER', status: 'ACTIVE', organization: 'Shelter Home Trust', organizationType: 'Shelter', verified: true, joinedAt: '2026-05-12', lastActive: '2026-09-17', totalRequests: 21 },
  { id: 'u6', name: 'Priya Patel', email: 'priya.p@example.com', phone: '+91 77665 54433', role: 'RECEIVER', status: 'ACTIVE', organization: 'Youth Shelter Network', organizationType: 'Shelter', verified: true, joinedAt: '2026-06-05', lastActive: '2026-09-16', totalRequests: 14 },
  { id: 'u7', name: 'Royal Catering Services', email: 'royal@example.com', phone: '+91 88776 65544', role: 'DONOR', status: 'ACTIVE', organization: 'Royal Catering Services', organizationType: 'Catering', verified: true, joinedAt: '2026-03-25', lastActive: '2026-09-18', totalDonations: 62 },
  { id: 'u8', name: 'Community Kitchen Bengaluru', email: 'ckb@example.com', phone: '+91 99001 12233', role: 'RECEIVER', status: 'ACTIVE', organization: 'Community Kitchen Bengaluru', organizationType: 'Community Kitchen', verified: true, joinedAt: '2026-04-15', lastActive: '2026-09-18', totalRequests: 45 },
  { id: 'u9', name: 'Arjun Kumar', email: 'arjun.k@example.com', phone: '+91 66554 43322', role: 'RECEIVER', status: 'INACTIVE', organization: 'City Relief', organizationType: 'Community', verified: true, joinedAt: '2026-07-01', lastActive: '2026-08-20', totalRequests: 5 },
  { id: 'u10', name: 'Taj Kitchen Express', email: 'tajkitchen@example.com', phone: '+91 77889 90011', role: 'DONOR', status: 'ACTIVE', organization: 'Taj Kitchen Express', organizationType: 'Restaurant', verified: true, joinedAt: '2026-01-05', lastActive: '2026-09-18', totalDonations: 91 },
  { id: 'u11', name: 'Annapurna Seva Trust', email: 'annapurna@example.com', phone: '+91 88990 01122', role: 'RECEIVER', status: 'ACTIVE', organization: 'Annapurna Seva Trust', organizationType: 'NGO', verified: false, joinedAt: '2026-08-01', lastActive: '2026-09-15', totalRequests: 8 },
  { id: 'u12', name: 'Admin User', email: 'admin@mealshare.in', phone: '+91 90000 00001', role: 'ADMIN', status: 'ACTIVE', verified: true, joinedAt: '2025-12-01', lastActive: '2026-09-18' },
  { id: 'u13', name: 'Sanjay Reddy', email: 'sanjay.r@example.com', phone: '+91 55443 32211', role: 'RECEIVER', status: 'ACTIVE', organization: 'Hope Volunteers Center', organizationType: 'NGO', verified: true, joinedAt: '2026-05-20', lastActive: '2026-09-18', totalRequests: 9 },
  { id: 'u14', name: 'Nandi Hills Resort', email: 'nandihills@example.com', phone: '+91 44332 21100', role: 'DONOR', status: 'ACTIVE', organization: 'Nandi Hills Resort', organizationType: 'Hotel', verified: true, joinedAt: '2026-06-15', lastActive: '2026-09-17', totalDonations: 23 },
  { id: 'u15', name: 'Meals on Wheels Bengaluru', email: 'mow@example.com', phone: '+91 33221 10099', role: 'RECEIVER', status: 'ACTIVE', organization: 'Meals on Wheels Bengaluru', organizationType: 'NGO', verified: true, joinedAt: '2026-02-28', lastActive: '2026-09-18', totalRequests: 56 },
];

export const mockDonations: Donation[] = [
  { id: 'DON-1042', donorId: 'u1', donorName: 'FreshBite Restaurant', foodItem: 'Prepared Meals (Veg Biryani)', description: 'Fresh vegetable biryani from lunch catering', category: 'Meals', quantity: 120, unit: 'meals', location: '42 MG Road, Bengaluru', consumeBefore: '2026-09-18T22:30:00', storageCondition: 'Room Temperature', hasAllergens: true, allergenInfo: 'Contains nuts and dairy', status: 'AVAILABLE', createdAt: '2026-09-18T10:00:00', updatedAt: '2026-09-18T10:00:00' },
  { id: 'DON-1041', donorId: 'u4', donorName: 'Green Garden Café', foodItem: 'Dal Tadka & Roti', description: 'Home-style dal tadka with fresh rotis', category: 'Meals', quantity: 80, unit: 'meals', location: '15 Brigade Road, Bengaluru', consumeBefore: '2026-09-18T23:00:00', storageCondition: 'Room Temperature', hasAllergens: false, allergenInfo: '', status: 'AVAILABLE', createdAt: '2026-09-18T09:30:00', updatedAt: '2026-09-18T09:30:00' },
  { id: 'DON-1040', donorId: 'u7', donorName: 'Royal Catering Services', foodItem: 'Assorted Sandwiches', description: 'Leftover sandwiches from corporate meeting', category: 'Bakery', quantity: 50, unit: 'portions', location: '88 Lavelle Road, Bengaluru', consumeBefore: '2026-09-18T21:00:00', storageCondition: 'Refrigerated', hasAllergens: true, allergenInfo: 'Contains gluten and eggs', status: 'REQUESTED', createdAt: '2026-09-18T08:15:00', updatedAt: '2026-09-18T11:00:00' },
  { id: 'DON-1039', donorId: 'u10', donorName: 'Taj Kitchen Express', foodItem: 'Paneer Butter Masala & Rice', description: 'Rich paneer butter masala with steamed rice', category: 'Meals', quantity: 200, unit: 'meals', location: '5 Residency Road, Bengaluru', consumeBefore: '2026-09-18T20:00:00', storageCondition: 'Refrigerated', hasAllergens: true, allergenInfo: 'Contains dairy', status: 'APPROVED', createdAt: '2026-09-17T16:00:00', updatedAt: '2026-09-18T08:00:00' },
  { id: 'DON-1038', donorId: 'u1', donorName: 'FreshBite Restaurant', foodItem: 'Fresh Fruit Platter', description: 'Seasonal fruit platter with mangoes, grapes', category: 'Fruits', quantity: 30, unit: 'portions', location: '42 MG Road, Bengaluru', consumeBefore: '2026-09-19T12:00:00', storageCondition: 'Refrigerated', hasAllergens: false, allergenInfo: '', status: 'PICKUP_ASSIGNED', createdAt: '2026-09-17T14:00:00', updatedAt: '2026-09-18T07:00:00' },
  { id: 'DON-1037', donorId: 'u14', donorName: 'Nandi Hills Resort', foodItem: 'Vegetable Soup', description: 'Hearty vegetable soup with seasonal greens', category: 'Meals', quantity: 60, unit: 'portions', location: '22 Nandi Hills Road, Bengaluru', consumeBefore: '2026-09-17T20:00:00', storageCondition: 'Room Temperature', hasAllergens: false, allergenInfo: '', status: 'PICKED_UP', createdAt: '2026-09-17T10:00:00', updatedAt: '2026-09-17T16:00:00' },
  { id: 'DON-1036', donorId: 'u7', donorName: 'Royal Catering Services', foodItem: 'Chole Bhature', description: 'Spicy chickpea curry with fried bread', category: 'Meals', quantity: 150, unit: 'meals', location: '88 Lavelle Road, Bengaluru', consumeBefore: '2026-09-17T22:00:00', storageCondition: 'Room Temperature', hasAllergens: true, allergenInfo: 'Contains gluten', status: 'COMPLETED', createdAt: '2026-09-16T11:00:00', updatedAt: '2026-09-17T14:00:00' },
  { id: 'DON-1035', donorId: 'u4', donorName: 'Green Garden Café', foodItem: 'Mixed Salad Bowl', description: 'Fresh garden salad with dressing', category: 'Vegetables', quantity: 40, unit: 'portions', location: '15 Brigade Road, Bengaluru', consumeBefore: '2026-09-16T18:00:00', storageCondition: 'Refrigerated', hasAllergens: false, allergenInfo: '', status: 'EXPIRED', createdAt: '2026-09-15T09:00:00', updatedAt: '2026-09-16T18:00:00' },
  { id: 'DON-1034', donorId: 'u10', donorName: 'Taj Kitchen Express', foodItem: 'Idli Sambar', description: 'South Indian breakfast - steamed idlis with sambar', category: 'Meals', quantity: 100, unit: 'meals', location: '5 Residency Road, Bengaluru', consumeBefore: '2026-09-17T14:00:00', storageCondition: 'Room Temperature', hasAllergens: false, allergenInfo: '', status: 'COMPLETED', createdAt: '2026-09-16T07:00:00', updatedAt: '2026-09-16T13:00:00' },
  { id: 'DON-1033', donorId: 'u1', donorName: 'FreshBite Restaurant', foodItem: 'Pasta Salad', description: 'Italian pasta salad with fresh vegetables', category: 'Bakery', quantity: 45, unit: 'portions', location: '42 MG Road, Bengaluru', consumeBefore: '2026-09-16T20:00:00', storageCondition: 'Refrigerated', hasAllergens: true, allergenInfo: 'Contains gluten and dairy', status: 'CANCELLED', createdAt: '2026-09-15T14:00:00', updatedAt: '2026-09-15T16:00:00' },
  { id: 'DON-1032', donorId: 'u14', donorName: 'Nandi Hills Resort', foodItem: 'Chapati & Mixed Curry', description: 'Fresh chapatis with mixed vegetable curry', category: 'Meals', quantity: 90, unit: 'meals', location: '22 Nandi Hills Road, Bengaluru', consumeBefore: '2026-09-16T21:00:00', storageCondition: 'Room Temperature', hasAllergens: false, allergenInfo: '', status: 'COMPLETED', createdAt: '2026-09-15T10:00:00', updatedAt: '2026-09-15T18:00:00' },
  { id: 'DON-1031', donorId: 'u7', donorName: 'Royal Catering Services', foodItem: 'Pulao & Raita', description: 'Vegetable pulao with mint raita', category: 'Meals', quantity: 180, unit: 'meals', location: '88 Lavelle Road, Bengaluru', consumeBefore: '2026-09-15T22:00:00', storageCondition: 'Room Temperature', hasAllergens: true, allergenInfo: 'Contains dairy', status: 'COMPLETED', createdAt: '2026-09-14T12:00:00', updatedAt: '2026-09-15T15:00:00' },
];

export const mockRequests: FoodRequest[] = [
  { id: 'REQ-2015', donationId: 'DON-1040', donationItem: 'Assorted Sandwiches', receiverId: 'u2', receiverName: 'Hope Foundation', receiverOrg: 'Hope Foundation', quantity: 50, unit: 'portions', purpose: 'Evening shelter feeding program', status: 'PENDING', requestedAt: '2026-09-18T11:00:00', updatedAt: '2026-09-18T11:00:00' },
  { id: 'REQ-2014', donationId: 'DON-1042', donationItem: 'Prepared Meals (Veg Biryani)', receiverId: 'u8', receiverName: 'Community Kitchen Bengaluru', receiverOrg: 'Community Kitchen Bengaluru', quantity: 100, unit: 'meals', purpose: 'Community lunch distribution', status: 'PENDING', requestedAt: '2026-09-18T10:30:00', updatedAt: '2026-09-18T10:30:00' },
  { id: 'REQ-2013', donationId: 'DON-1039', donationItem: 'Paneer Butter Masala & Rice', receiverId: 'u15', receiverName: 'Meals on Wheels Bengaluru', receiverOrg: 'Meals on Wheels Bengaluru', quantity: 200, unit: 'meals', purpose: 'Elderly meal delivery program', status: 'APPROVED', requestedAt: '2026-09-17T17:00:00', updatedAt: '2026-09-18T08:00:00' },
  { id: 'REQ-2012', donationId: 'DON-1038', donationItem: 'Fresh Fruit Platter', receiverId: 'u2', receiverName: 'Hope Foundation', receiverOrg: 'Hope Foundation', quantity: 30, unit: 'portions', purpose: 'Children nutrition program', status: 'APPROVED', requestedAt: '2026-09-17T15:00:00', updatedAt: '2026-09-17T16:00:00' },
  { id: 'REQ-2011', donationId: 'DON-1037', donationItem: 'Vegetable Soup', receiverId: 'u5', receiverName: 'Shelter Home Trust', receiverOrg: 'Shelter Home Trust', quantity: 60, unit: 'portions', purpose: 'Shelter residents dinner', status: 'APPROVED', requestedAt: '2026-09-17T11:00:00', updatedAt: '2026-09-17T12:00:00' },
  { id: 'REQ-2010', donationId: 'DON-1036', donationItem: 'Chole Bhature', receiverId: 'u8', receiverName: 'Community Kitchen Bengaluru', receiverOrg: 'Community Kitchen Bengaluru', quantity: 150, unit: 'meals', purpose: 'Daily community meal service', status: 'COMPLETED', requestedAt: '2026-09-16T12:00:00', updatedAt: '2026-09-17T14:00:00' },
  { id: 'REQ-2009', donationId: 'DON-1034', donationItem: 'Idli Sambar', receiverId: 'u15', receiverName: 'Meals on Wheels Bengaluru', receiverOrg: 'Meals on Wheels Bengaluru', quantity: 100, unit: 'meals', purpose: 'Breakfast distribution', status: 'COMPLETED', requestedAt: '2026-09-16T07:30:00', updatedAt: '2026-09-16T13:00:00' },
  { id: 'REQ-2008', donationId: 'DON-1033', donationItem: 'Pasta Salad', receiverId: 'u5', receiverName: 'Shelter Home Trust', receiverOrg: 'Shelter Home Trust', quantity: 45, unit: 'portions', purpose: 'Shelter meal program', status: 'WITHDRAWN', requestedAt: '2026-09-15T15:00:00', updatedAt: '2026-09-15T16:00:00' },
  { id: 'REQ-2007', donationId: 'DON-1032', donationItem: 'Chapati & Mixed Curry', receiverId: 'u2', receiverName: 'Hope Foundation', receiverOrg: 'Hope Foundation', quantity: 90, unit: 'meals', purpose: 'Evening meal distribution', status: 'COMPLETED', requestedAt: '2026-09-15T11:00:00', updatedAt: '2026-09-15T18:00:00' },
  { id: 'REQ-2006', donationId: 'DON-1031', donationItem: 'Pulao & Raita', receiverId: 'u8', receiverName: 'Community Kitchen Bengaluru', receiverOrg: 'Community Kitchen Bengaluru', quantity: 180, unit: 'meals', purpose: 'Community lunch', status: 'COMPLETED', requestedAt: '2026-09-14T13:00:00', updatedAt: '2026-09-15T15:00:00' },
  { id: 'REQ-2005', donationId: 'DON-1041', donationItem: 'Dal Tadka & Roti', receiverId: 'u11', receiverName: 'Annapurna Seva Trust', receiverOrg: 'Annapurna Seva Trust', quantity: 40, unit: 'meals', purpose: 'Orphanage meal program', status: 'PENDING', requestedAt: '2026-09-18T10:45:00', updatedAt: '2026-09-18T10:45:00' },
];

export const mockPickups: Pickup[] = [
  { id: 'PCK-3010', donationId: 'DON-1038', donationItem: 'Fresh Fruit Platter', donorName: 'FreshBite Restaurant', receiverName: 'Hope Foundation', pickupAddress: '42 MG Road, Bengaluru', deliveryAddress: '18 Residency Road, Bengaluru', scheduledTime: '2026-09-18T18:00:00', otpCode: '****', status: 'ASSIGNED', createdAt: '2026-09-18T07:00:00' },
  { id: 'PCK-3009', donationId: 'DON-1039', donationItem: 'Paneer Butter Masala & Rice', donorName: 'Taj Kitchen Express', receiverName: 'Meals on Wheels Bengaluru', pickupAddress: '5 Residency Road, Bengaluru', deliveryAddress: '32 Koramangala, Bengaluru', scheduledTime: '2026-09-18T16:00:00', otpCode: '****', status: 'EN_ROUTE', createdAt: '2026-09-18T08:00:00' },
  { id: 'PCK-3008', donationId: 'DON-1037', donationItem: 'Vegetable Soup', donorName: 'Nandi Hills Resort', receiverName: 'Shelter Home Trust', pickupAddress: '22 Nandi Hills Road, Bengaluru', deliveryAddress: '10 Richmond Road, Bengaluru', scheduledTime: '2026-09-17T17:00:00', otpCode: '****', status: 'DELIVERED', createdAt: '2026-09-17T12:00:00' },
  { id: 'PCK-3007', donationId: 'DON-1036', donationItem: 'Chole Bhature', donorName: 'Royal Catering Services', receiverName: 'Community Kitchen Bengaluru', pickupAddress: '88 Lavelle Road, Bengaluru', deliveryAddress: '45 Jayanagar, Bengaluru', scheduledTime: '2026-09-17T13:00:00', otpCode: '****', status: 'DELIVERED', createdAt: '2026-09-16T14:00:00' },
  { id: 'PCK-3006', donationId: 'DON-1034', donationItem: 'Idli Sambar', donorName: 'Taj Kitchen Express', receiverName: 'Meals on Wheels Bengaluru', pickupAddress: '5 Residency Road, Bengaluru', deliveryAddress: '32 Koramangala, Bengaluru', scheduledTime: '2026-09-16T09:00:00', otpCode: '****', status: 'DELIVERED', createdAt: '2026-09-16T07:30:00' },
  { id: 'PCK-3005', donationId: 'DON-1032', donationItem: 'Chapati & Mixed Curry', donorName: 'Nandi Hills Resort', receiverName: 'Hope Foundation', pickupAddress: '22 Nandi Hills Road, Bengaluru', deliveryAddress: '18 Residency Road, Bengaluru', scheduledTime: '2026-09-15T16:00:00', otpCode: '****', status: 'DELIVERED', createdAt: '2026-09-15T11:00:00' },
  { id: 'PCK-3004', donationId: 'DON-1031', donationItem: 'Pulao & Raita', donorName: 'Royal Catering Services', receiverName: 'Community Kitchen Bengaluru', pickupAddress: '88 Lavelle Road, Bengaluru', deliveryAddress: '45 Jayanagar, Bengaluru', scheduledTime: '2026-09-15T14:00:00', otpCode: '****', status: 'DELIVERED', createdAt: '2026-09-14T13:00:00' },
];

export const mockVerifications: Verification[] = [
  { id: 'VER-001', organizationName: 'Annapurna Seva Trust', organizationType: 'NGO', contactPerson: 'Lakshmi Devi', email: 'annapurna@example.com', phone: '+91 88990 01122', address: '55 JP Nagar, Bengaluru 560078', documents: ['Registration Certificate', 'PAN Card', 'Address Proof'], submittedAt: '2026-09-16T10:00:00', status: 'PENDING' },
  { id: 'VER-002', organizationName: 'Nourish India Foundation', organizationType: 'NGO', contactPerson: 'Ravi Menon', email: 'nourish@example.com', phone: '+91 77665 54411', address: '12 Whitefield, Bengaluru 560066', documents: ['Registration Certificate', '80G Certificate', 'PAN Card'], submittedAt: '2026-09-15T14:00:00', status: 'PENDING' },
  { id: 'VER-003', organizationName: 'Old Age Care Home', organizationType: 'Shelter', contactPerson: 'Meena Kumari', email: 'oldagecare@example.com', phone: '+91 66554 43300', address: '28 Basavanagudi, Bengaluru 560004', documents: ['Registration Certificate', 'Trust Deed'], submittedAt: '2026-09-14T09:00:00', status: 'MORE_INFO_NEEDED', reviewNotes: 'PAN card copy is not legible. Please re-upload.' },
  { id: 'VER-004', organizationName: 'Hope Foundation', organizationType: 'NGO', contactPerson: 'Aisha Khan', email: 'hope@example.com', phone: '+91 91234 56789', address: '18 Residency Road, Bengaluru 560025', documents: ['Registration Certificate', 'PAN Card', '80G Certificate', 'FCRA Certificate'], submittedAt: '2026-09-01T10:00:00', status: 'APPROVED' },
  { id: 'VER-005', organizationName: 'Shelter Home Trust', organizationType: 'Shelter', contactPerson: 'Venkatesh R', email: 'shelter@example.com', phone: '+91 99887 76655', address: '10 Richmond Road, Bengaluru 560025', documents: ['Registration Certificate', 'PAN Card', 'Address Proof'], submittedAt: '2026-08-20T11:00:00', status: 'APPROVED' },
];

export const mockNotifications: Notification[] = [
  { id: 'n1', title: 'New donation created', message: 'FreshBite Restaurant added 120 meals of Veg Biryani (DON-1042)', type: 'donation', entityId: 'DON-1042', isRead: false, createdAt: '2026-09-18T10:00:00' },
  { id: 'n2', title: 'New request received', message: 'Community Kitchen Bengaluru requested 100 meals from DON-1042', type: 'request', entityId: 'REQ-2014', isRead: false, createdAt: '2026-09-18T10:30:00' },
  { id: 'n3', title: 'New request received', message: 'Hope Foundation requested 50 portions from DON-1040', type: 'request', entityId: 'REQ-2015', isRead: false, createdAt: '2026-09-18T11:00:00' },
  { id: 'n4', title: 'Request approved', message: 'Request REQ-2013 approved for Meals on Wheels Bengaluru', type: 'request', entityId: 'REQ-2013', isRead: false, createdAt: '2026-09-18T08:00:00' },
  { id: 'n5', title: 'Pickup scheduled', message: 'Pickup scheduled for Hope Foundation (PCK-3010)', type: 'pickup', entityId: 'PCK-3010', isRead: true, createdAt: '2026-09-18T07:00:00' },
  { id: 'n6', title: 'Pickup in transit', message: 'Recipient is en route for PCK-3009 (Paneer Butter Masala)', type: 'pickup', entityId: 'PCK-3009', isRead: true, createdAt: '2026-09-18T08:30:00' },
  { id: 'n7', title: 'Verification submitted', message: 'Annapurna Seva Trust submitted verification documents', type: 'verification', entityId: 'VER-001', isRead: true, createdAt: '2026-09-16T10:00:00' },
  { id: 'n8', title: 'Pickup completed', message: 'Vegetable Soup delivered to Shelter Home Trust (PCK-3008)', type: 'pickup', entityId: 'PCK-3008', isRead: true, createdAt: '2026-09-17T18:00:00' },
  { id: 'n9', title: 'Donation expired', message: 'Mixed Salad Bowl (DON-1035) has expired and been removed', type: 'donation', entityId: 'DON-1035', isRead: true, createdAt: '2026-09-16T18:00:00' },
  { id: 'n10', title: 'System update', message: 'MealShare v2.1 deployed with improved pickup tracking', type: 'system', isRead: true, createdAt: '2026-09-15T06:00:00' },
  { id: 'n11', title: 'Verification approved', message: 'Hope Foundation verification approved', type: 'verification', entityId: 'VER-004', isRead: true, createdAt: '2026-09-02T09:00:00' },
  { id: 'n12', title: 'New donation created', message: 'Green Garden Café added 80 meals of Dal Tadka & Roti', type: 'donation', entityId: 'DON-1041', isRead: false, createdAt: '2026-09-18T09:30:00' },
];

export const mockActivity: ActivityItem[] = [
  { id: 'act1', action: 'Donation Created', description: 'FreshBite Restaurant added 120 meals of Veg Biryani', user: 'FreshBite Restaurant', entityType: 'donation', entityId: 'DON-1042', timestamp: '2026-09-18T10:00:00' },
  { id: 'act2', action: 'Request Submitted', description: 'Community Kitchen Bengaluru requested 100 meals', user: 'Community Kitchen Bengaluru', entityType: 'request', entityId: 'REQ-2014', timestamp: '2026-09-18T10:30:00' },
  { id: 'act3', action: 'Request Submitted', description: 'Hope Foundation requested 50 portions of sandwiches', user: 'Hope Foundation', entityType: 'request', entityId: 'REQ-2015', timestamp: '2026-09-18T11:00:00' },
  { id: 'act4', action: 'Request Approved', description: 'Meals on Wheels request for Paneer Butter Masala approved', user: 'Admin', entityType: 'request', entityId: 'REQ-2013', timestamp: '2026-09-18T08:00:00' },
  { id: 'act5', action: 'Pickup Scheduled', description: 'Pickup scheduled for Fresh Fruit Platter', user: 'System', entityType: 'pickup', entityId: 'PCK-3010', timestamp: '2026-09-18T07:00:00' },
  { id: 'act6', action: 'Pickup In Transit', description: 'Recipient picked up Paneer Butter Masala from Taj Kitchen', user: 'Priya Patel', entityType: 'pickup', entityId: 'PCK-3009', timestamp: '2026-09-18T08:30:00' },
  { id: 'act7', action: 'Pickup Delivered', description: 'Vegetable Soup delivered to Shelter Home Trust', user: 'Sanjay Reddy', entityType: 'pickup', entityId: 'PCK-3008', timestamp: '2026-09-17T18:00:00' },
  { id: 'act8', action: 'Donation Completed', description: 'Chole Bhature successfully redistributed', user: 'System', entityType: 'donation', entityId: 'DON-1036', timestamp: '2026-09-17T14:00:00' },
  { id: 'act9', action: 'Verification Submitted', description: 'Annapurna Seva Trust submitted verification documents', user: 'Annapurna Seva Trust', entityType: 'verification', entityId: 'VER-001', timestamp: '2026-09-16T10:00:00' },
  { id: 'act10', action: 'Donation Expired', description: 'Mixed Salad Bowl from Green Garden Café expired', user: 'System', entityType: 'donation', entityId: 'DON-1035', timestamp: '2026-09-16T18:00:00' },
];

// ═══════════════════════════════════════════════════════
// DASHBOARD STATS
// ═══════════════════════════════════════════════════════

export const dashboardStats = {
  totalDonations: 236,
  activeDonations: 4,
  pendingRequests: 3,
  completedPickups: 142,
  foodRedistributed: '4,820 kg',
  peopleServed: 12450,
  donationsThisWeek: 18,
  donationsLastWeek: 14,
  requestsThisWeek: 11,
  pickupsThisWeek: 9,
};

// Chart data for last 7 days
export const donationChartData = [
  { day: 'Mon', created: 4, completed: 2 },
  { day: 'Tue', created: 6, completed: 5 },
  { day: 'Wed', created: 3, completed: 4 },
  { day: 'Thu', created: 7, completed: 3 },
  { day: 'Fri', created: 5, completed: 6 },
  { day: 'Sat', created: 8, completed: 5 },
  { day: 'Sun', created: 3, completed: 2 },
];

export const donationStatusDistribution = [
  { name: 'Available', value: 24, color: '#22c55e' },
  { name: 'Requested', value: 8, color: '#f59e0b' },
  { name: 'Approved', value: 12, color: '#3b82f6' },
  { name: 'In Pickup', value: 5, color: '#8b5cf6' },
  { name: 'Completed', value: 142, color: '#10b981' },
  { name: 'Expired', value: 18, color: '#6b7280' },
];

// Monthly data for reports
export const monthlyData = [
  { month: 'Apr', donations: 22, pickups: 18, meals: 1200 },
  { month: 'May', donations: 28, pickups: 24, meals: 1800 },
  { month: 'Jun', donations: 35, pickups: 30, meals: 2400 },
  { month: 'Jul', donations: 42, pickups: 38, meals: 3100 },
  { month: 'Aug', donations: 38, pickups: 34, meals: 2800 },
  { month: 'Sep', donations: 48, pickups: 42, meals: 3600 },
];

export const donorContributions = [
  { name: 'Taj Kitchen Express', donations: 91 },
  { name: 'Royal Catering', donations: 62 },
  { name: 'FreshBite Restaurant', donations: 48 },
  { name: 'Green Garden Café', donations: 35 },
  { name: 'Nandi Hills Resort', donations: 23 },
];
