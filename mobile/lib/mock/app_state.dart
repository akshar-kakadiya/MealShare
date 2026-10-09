import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'models.dart';
import '../shared/api_client.dart';
import '../config/api_config.dart';
import '../auth/auth_service.dart';

class AppState extends ChangeNotifier {
  final ApiClient? apiClient;
  final AuthService? authService;

  String? donorDonationsError;
  String? nearbyDonationsError;
  String? receiverRequestsError;
  String? notificationsError;
  bool isNotificationsLoading = false;
  int unreadNotificationsCount = 0;
  bool isLoading = false;

  AppState({this.apiClient, this.authService}) {
    if (ApiConfig.isRealMode) {
      donations = [];
      requests = [];
      donorNotifications = [];
      receiverNotifications = [];
    }
    if (authService != null) {
      authService!.addListener(_onAuthChanged);
    }
  }

  void _onAuthChanged() {
    if (authService != null && authService!.isAuthenticated) {
      final role = authService!.role;
      if (role == UserRole.donor) {
        currentUser = UserProfile(
          id: authService!.uid ?? 'u1',
          name: authService!.displayName ?? 'FreshBite Restaurant',
          role: 'DONOR',
          email: authService!.email ?? 'freshbite@example.com',
          phone: '+91 98765 43210',
          address: '42 MG Road, Bengaluru 560001',
          organizationType: 'Restaurant',
          isVerified: true,
        );
      } else if (role == UserRole.receiver) {
        currentUser = UserProfile(
          id: authService!.uid ?? 'u2',
          name: authService!.displayName ?? 'Hope Foundation',
          role: 'RECEIVER',
          email: authService!.email ?? 'hope@example.com',
          phone: '+91 91234 56789',
          address: '18 Residency Road, Bengaluru 560025',
          organizationType: 'NGO',
          isVerified: true,
        );
      }
      notifyListeners();
    }
  }

  // ─── Users ─────────────────────────────────
  UserProfile currentUser = UserProfile(
    id: 'u1',
    name: 'FreshBite Restaurant',
    role: 'DONOR',
    email: 'freshbite@example.com',
    phone: '+91 98765 43210',
    address: '42 MG Road, Bengaluru 560001',
    organizationType: 'Restaurant',
    isVerified: true,
    joinedAt: DateTime(2026, 3, 15),
  );

  static final _donorProfile = UserProfile(
    id: 'u1',
    name: 'FreshBite Restaurant',
    role: 'DONOR',
    email: 'freshbite@example.com',
    phone: '+91 98765 43210',
    address: '42 MG Road, Bengaluru 560001',
    organizationType: 'Restaurant',
    isVerified: true,
    joinedAt: DateTime(2026, 3, 15),
  );

  static final _receiverProfile = UserProfile(
    id: 'u2',
    name: 'Hope Foundation',
    role: 'RECEIVER',
    email: 'hope@example.com',
    phone: '+91 91234 56789',
    address: '18 Residency Road, Bengaluru 560025',
    organizationType: 'NGO',
    isVerified: true,
    joinedAt: DateTime(2026, 1, 10),
  );

  void switchRole() {
    if (currentUser.role == 'DONOR') {
      currentUser = _receiverProfile;
    } else {
      currentUser = _donorProfile;
    }
    notifyListeners();
  }

  // ─── Donations ─────────────────────────────
  List<Donation> donations = [
    Donation(
      id: 'DON-1001',
      donorId: 'u1',
      donorName: 'FreshBite Restaurant',
      title: 'Vegetable Rice',
      description: 'Fresh vegetable biryani from our lunch catering event. Prepared with premium basmati rice, seasonal vegetables, and aromatic spices.',
      quantity: 50,
      unit: 'meals',
      category: 'Meals',
      consumeBefore: 'Today 10:30 PM',
      consumeBeforeTime: DateTime.now().add(const Duration(hours: 6)),
      storageCondition: 'Refrigerated',
      packagingCondition: 'Sealed',
      hasAllergens: true,
      allergenInfo: 'Contains nuts and dairy',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(hours: 3)),
      pickupAddress: '42 MG Road, Bengaluru',
      pickupWindowStart: '4:00 PM',
      pickupWindowEnd: '8:00 PM',
      pickupInstructions: 'Use the side entrance. Ask for the kitchen manager.',
      status: 'AVAILABLE',
      createdAt: DateTime.now().subtract(const Duration(hours: 1)),
      categoryIcon: Icons.rice_bowl,
      distance: 1.8,
    ),
    Donation(
      id: 'DON-1002',
      donorId: 'u1',
      donorName: 'FreshBite Restaurant',
      title: 'Assorted Sandwiches',
      description: 'Leftover sandwiches from a corporate meeting. Includes veg, paneer, and egg varieties.',
      quantity: 30,
      unit: 'portions',
      category: 'Bakery',
      consumeBefore: 'Today 9:00 PM',
      consumeBeforeTime: DateTime.now().add(const Duration(hours: 5)),
      storageCondition: 'Room Temperature',
      packagingCondition: 'Sealed',
      hasAllergens: true,
      allergenInfo: 'Contains gluten and eggs',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(hours: 4)),
      pickupAddress: '42 MG Road, Bengaluru',
      pickupWindowStart: '3:00 PM',
      pickupWindowEnd: '7:00 PM',
      pickupInstructions: 'Available at the front desk.',
      status: 'REQUESTED',
      createdAt: DateTime.now().subtract(const Duration(hours: 3)),
      categoryIcon: Icons.bakery_dining,
      distance: 1.8,
    ),
    Donation(
      id: 'DON-1003',
      donorId: 'u1',
      donorName: 'FreshBite Restaurant',
      title: 'Fresh Fruit Platter',
      description: 'Seasonal fruit platter with mangoes, grapes, and watermelon slices.',
      quantity: 20,
      unit: 'portions',
      category: 'Fruits',
      consumeBefore: 'Tomorrow 12:00 PM',
      consumeBeforeTime: DateTime.now().add(const Duration(hours: 18)),
      storageCondition: 'Refrigerated',
      packagingCondition: 'Sealed',
      hasAllergens: false,
      allergenInfo: '',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(hours: 1)),
      pickupAddress: '42 MG Road, Bengaluru',
      pickupWindowStart: '5:00 PM',
      pickupWindowEnd: '9:00 PM',
      pickupInstructions: 'Cold storage unit near loading dock.',
      status: 'APPROVED',
      createdAt: DateTime.now().subtract(const Duration(days: 1)),
      categoryIcon: Icons.local_dining,
      distance: 1.8,
    ),
    Donation(
      id: 'DON-1004',
      donorId: 'u3',
      donorName: 'Green Garden Café',
      title: 'Dal Tadka & Roti',
      description: 'Home-style dal tadka with fresh rotis. Nutritious and filling meal.',
      quantity: 40,
      unit: 'meals',
      category: 'Meals',
      consumeBefore: 'Today 11:00 PM',
      consumeBeforeTime: DateTime.now().add(const Duration(hours: 7)),
      storageCondition: 'Room Temperature',
      packagingCondition: 'Sealed',
      hasAllergens: false,
      allergenInfo: '',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(hours: 2)),
      pickupAddress: '15 Brigade Road, Bengaluru',
      pickupWindowStart: '4:30 PM',
      pickupWindowEnd: '8:30 PM',
      pickupInstructions: 'Ring the bell at the back entrance.',
      status: 'AVAILABLE',
      createdAt: DateTime.now().subtract(const Duration(hours: 2)),
      categoryIcon: Icons.restaurant,
      distance: 3.2,
    ),
    Donation(
      id: 'DON-1005',
      donorId: 'u3',
      donorName: 'Green Garden Café',
      title: 'Vegetable Soup',
      description: 'Hearty vegetable soup with seasonal greens. Perfect for shelter homes.',
      quantity: 25,
      unit: 'portions',
      category: 'Meals',
      consumeBefore: 'Today 8:00 PM',
      consumeBeforeTime: DateTime.now().add(const Duration(hours: 3)),
      storageCondition: 'Room Temperature',
      packagingCondition: 'Partially Sealed',
      hasAllergens: false,
      allergenInfo: '',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(hours: 1)),
      pickupAddress: '15 Brigade Road, Bengaluru',
      pickupWindowStart: '3:00 PM',
      pickupWindowEnd: '6:00 PM',
      pickupInstructions: 'Ask for Chef Ramesh.',
      status: 'AVAILABLE',
      createdAt: DateTime.now().subtract(const Duration(minutes: 45)),
      categoryIcon: Icons.soup_kitchen,
      distance: 3.2,
    ),
    Donation(
      id: 'DON-1006',
      donorId: 'u1',
      donorName: 'FreshBite Restaurant',
      title: 'Paneer Butter Masala',
      description: 'Rich and creamy paneer butter masala. A crowd favorite from today\'s lunch service.',
      quantity: 35,
      unit: 'meals',
      category: 'Meals',
      consumeBefore: 'Yesterday',
      consumeBeforeTime: DateTime.now().subtract(const Duration(hours: 12)),
      storageCondition: 'Refrigerated',
      packagingCondition: 'Sealed',
      hasAllergens: true,
      allergenInfo: 'Contains dairy and nuts',
      foodSafetyDeclared: true,
      preparationTime: DateTime.now().subtract(const Duration(days: 1, hours: 5)),
      pickupAddress: '42 MG Road, Bengaluru',
      pickupWindowStart: '12:00 PM',
      pickupWindowEnd: '4:00 PM',
      pickupInstructions: 'Use the side entrance.',
      status: 'COMPLETED',
      createdAt: DateTime.now().subtract(const Duration(days: 2)),
      categoryIcon: Icons.restaurant,
      distance: 1.8,
    ),
  ];

  // ─── Requests ──────────────────────────────
  List<FoodRequest> requests = [
    FoodRequest(
      id: 'REQ-2001',
      donationId: 'DON-1002',
      receiverId: 'u2',
      receiverName: 'Hope Foundation',
      quantity: 30,
      message: 'We can distribute these to our evening shelter program.',
      distance: 2.5,
      status: 'PENDING',
      requestedAt: DateTime.now().subtract(const Duration(minutes: 30)),
    ),
    FoodRequest(
      id: 'REQ-2002',
      donationId: 'DON-1003',
      receiverId: 'u2',
      receiverName: 'Hope Foundation',
      quantity: 20,
      message: 'Perfect for our children\'s nutrition program.',
      distance: 2.5,
      status: 'APPROVED',
      requestedAt: DateTime.now().subtract(const Duration(hours: 2)),
    ),
    FoodRequest(
      id: 'REQ-2003',
      donationId: 'DON-1006',
      receiverId: 'u2',
      receiverName: 'Hope Foundation',
      quantity: 35,
      message: 'Thank you for the donation!',
      distance: 2.5,
      status: 'COMPLETED',
      requestedAt: DateTime.now().subtract(const Duration(days: 2)),
    ),
  ];

  // ─── Pickups ───────────────────────────────
  List<Pickup> pickups = [
    Pickup(
      id: 'PCK-3001',
      donationId: 'DON-1003',
      donationTitle: 'Fresh Fruit Platter',
      donorName: 'FreshBite Restaurant',
      receiverName: 'Hope Foundation',
      pickupAddress: '42 MG Road, Bengaluru',
      deliveryAddress: '18 Residency Road, Bengaluru',
      scheduledTime: DateTime.now().add(const Duration(hours: 2)),
      status: 'ASSIGNED',
      otpCode: '4829',
    ),
  ];

  // ─── Notifications (Donor) ─────────────────
  List<AppNotification> donorNotifications = [
    AppNotification(
      id: 'n1',
      title: 'New request received',
      description: 'Hope Foundation requested 30 Assorted Sandwiches from DON-1002.',
      timestamp: DateTime.now().subtract(const Duration(minutes: 2)),
      type: 'request',
      icon: Icons.inbox,
      isRead: false,
    ),
    AppNotification(
      id: 'n2',
      title: 'Pickup scheduled',
      description: 'Pickup scheduled for Hope Foundation for Fresh Fruit Platter.',
      timestamp: DateTime.now().subtract(const Duration(minutes: 45)),
      type: 'pickup',
      icon: Icons.local_shipping,
      isRead: false,
    ),
    AppNotification(
      id: 'n3',
      title: 'Pickup completed',
      description: 'Paneer Butter Masala was successfully delivered to Hope Foundation.',
      timestamp: DateTime.now().subtract(const Duration(days: 1)),
      type: 'pickup',
      icon: Icons.check_circle,
      isRead: true,
    ),
    AppNotification(
      id: 'n4',
      title: 'Donation expired',
      description: 'Your donation Pasta Salad (DON-0998) has expired and been removed.',
      timestamp: DateTime.now().subtract(const Duration(days: 1, hours: 3)),
      type: 'donation',
      icon: Icons.timer_off,
      isRead: true,
    ),
    AppNotification(
      id: 'n5',
      title: 'Thank you!',
      description: 'Hope Foundation sent you a thank you note for your continued support.',
      timestamp: DateTime.now().subtract(const Duration(days: 2)),
      type: 'system',
      icon: Icons.favorite,
      isRead: true,
    ),
    AppNotification(
      id: 'n6',
      title: 'Impact milestone',
      description: 'Congratulations! You have donated over 1,000 meals through MealShare.',
      timestamp: DateTime.now().subtract(const Duration(days: 3)),
      type: 'system',
      icon: Icons.emoji_events,
      isRead: true,
    ),
    AppNotification(
      id: 'n7',
      title: 'New request received',
      description: 'Shelter Home Trust requested 15 meals from your Dal Tadka donation.',
      timestamp: DateTime.now().subtract(const Duration(days: 4)),
      type: 'request',
      icon: Icons.inbox,
      isRead: true,
    ),
    AppNotification(
      id: 'n8',
      title: 'Profile verified',
      description: 'Your organization profile has been verified by MealShare admin.',
      timestamp: DateTime.now().subtract(const Duration(days: 7)),
      type: 'system',
      icon: Icons.verified,
      isRead: true,
    ),
  ];

  // ─── Notifications (Receiver) ──────────────
  List<AppNotification> receiverNotifications = [
    AppNotification(
      id: 'rn1',
      title: 'Request approved!',
      description: 'FreshBite Restaurant approved your request for Fresh Fruit Platter.',
      timestamp: DateTime.now().subtract(const Duration(minutes: 15)),
      type: 'request',
      icon: Icons.check_circle,
      isRead: false,
    ),
    AppNotification(
      id: 'rn2',
      title: 'Pickup scheduled',
      description: 'Pickup scheduled for your Fresh Fruit Platter at 6:00 PM.',
      timestamp: DateTime.now().subtract(const Duration(minutes: 10)),
      type: 'pickup',
      icon: Icons.local_shipping,
      isRead: false,
    ),
    AppNotification(
      id: 'rn3',
      title: 'New food available nearby',
      description: 'Vegetable Rice (50 meals) available 1.8 km from you.',
      timestamp: DateTime.now().subtract(const Duration(hours: 1)),
      type: 'donation',
      icon: Icons.restaurant,
      isRead: false,
    ),
    AppNotification(
      id: 'rn4',
      title: 'Delivery completed',
      description: 'Paneer Butter Masala was delivered successfully. Thank you!',
      timestamp: DateTime.now().subtract(const Duration(days: 1)),
      type: 'pickup',
      icon: Icons.check_circle,
      isRead: true,
    ),
    AppNotification(
      id: 'rn5',
      title: 'Request rejected',
      description: 'Your request for Pasta Salad was rejected. The food was already claimed.',
      timestamp: DateTime.now().subtract(const Duration(days: 2)),
      type: 'request',
      icon: Icons.cancel,
      isRead: true,
    ),
    AppNotification(
      id: 'rn6',
      title: 'Profile verified',
      description: 'Your organization has been verified. You can now request food donations.',
      timestamp: DateTime.now().subtract(const Duration(days: 5)),
      type: 'system',
      icon: Icons.verified,
      isRead: true,
    ),
  ];

  // ─── Computed Stats ────────────────────────
  int get donorTotalDonations => ApiConfig.isRealMode
      ? donations.length
      : donations.where((d) => d.donorId == 'u1').length;

  int get donorTotalMeals => ApiConfig.isRealMode
      ? donations.fold(0, (sum, d) => sum + d.quantity)
      : donations.where((d) => d.donorId == 'u1').fold(0, (sum, d) => sum + d.quantity);

  int get donorPeopleHelped => (donorTotalMeals * 0.9).round(); // ~90% distribution rate

  int get donorSuccessfulPickups => ApiConfig.isRealMode
      ? donations.where((d) => d.status == 'COMPLETED').length
      : donations.where((d) => d.donorId == 'u1' && d.status == 'COMPLETED').length;

  int get donorPendingRequests => ApiConfig.isRealMode
      ? requests.where((r) => r.status == 'PENDING').length
      : requests.where((r) => r.status == 'PENDING' && donations.any((d) => d.id == r.donationId && d.donorId == 'u1')).length;

  int get receiverMealsReceived => ApiConfig.isRealMode
      ? requests.where((r) => r.status == 'COMPLETED').fold(0, (sum, r) => sum + r.quantity)
      : requests.where((r) => r.receiverId == 'u2' && r.status == 'COMPLETED').fold(0, (sum, r) => sum + r.quantity);

  int get receiverFoodReceived => ApiConfig.isRealMode
      ? requests.where((r) => r.status == 'COMPLETED' || r.status == 'APPROVED').length
      : requests.where((r) => r.receiverId == 'u2' && (r.status == 'COMPLETED' || r.status == 'APPROVED')).length;

  int get receiverSuccessfulPickups => ApiConfig.isRealMode
      ? requests.where((r) => r.status == 'COMPLETED').length
      : requests.where((r) => r.receiverId == 'u2' && r.status == 'COMPLETED').length;

  int get unreadDonorNotifications => donorNotifications.where((n) => !n.isRead).length;
  int get unreadReceiverNotifications => receiverNotifications.where((n) => !n.isRead).length;

  // ─── Recent Activity ───────────────────────
  List<ActivityItem> get donorRecentActivity {
    if (ApiConfig.isRealMode) {
      return donorNotifications.map((n) => ActivityItem(
        title: n.title,
        subtitle: n.description,
        icon: n.icon,
        time: n.timestamp,
        iconColor: const Color(0xFF2E7D32),
      )).toList();
    }
    return [
      ActivityItem(
        title: 'Request received',
        subtitle: 'Hope Foundation requested Assorted Sandwiches',
        icon: Icons.inbox,
        time: DateTime.now().subtract(const Duration(minutes: 30)),
        iconColor: Colors.orange,
      ),
      ActivityItem(
        title: 'Donation created',
        subtitle: 'Vegetable Rice - 50 meals',
        icon: Icons.add_circle,
        time: DateTime.now().subtract(const Duration(hours: 1)),
        iconColor: Colors.green,
      ),
      ActivityItem(
        title: 'Pickup completed',
        subtitle: 'Paneer Butter Masala delivered to Hope Foundation',
        icon: Icons.check_circle,
        time: DateTime.now().subtract(const Duration(days: 1)),
        iconColor: Colors.green,
      ),
      ActivityItem(
        title: 'Request approved',
        subtitle: 'Fresh Fruit Platter approved for Hope Foundation',
        icon: Icons.thumb_up,
        time: DateTime.now().subtract(const Duration(days: 1, hours: 2)),
        iconColor: Colors.blue,
      ),
    ];
  }

  List<ActivityItem> get receiverRecentActivity {
    if (ApiConfig.isRealMode) {
      return receiverNotifications.map((n) => ActivityItem(
        title: n.title,
        subtitle: n.description,
        icon: n.icon,
        time: n.timestamp,
        iconColor: const Color(0xFF2E7D32),
      )).toList();
    }
    return [
      ActivityItem(
        title: 'Request approved',
        subtitle: 'Fresh Fruit Platter from FreshBite Restaurant',
        icon: Icons.check_circle,
        time: DateTime.now().subtract(const Duration(minutes: 15)),
        iconColor: Colors.green,
      ),
      ActivityItem(
        title: 'New food nearby',
        subtitle: 'Vegetable Rice - 50 meals, 1.8 km away',
        icon: Icons.restaurant,
        time: DateTime.now().subtract(const Duration(hours: 1)),
        iconColor: Colors.orange,
      ),
      ActivityItem(
        title: 'Delivery completed',
        subtitle: 'Paneer Butter Masala received',
        icon: Icons.check_circle,
        time: DateTime.now().subtract(const Duration(days: 1)),
        iconColor: Colors.green,
      ),
    ];
  }

  // ─── Mutations ─────────────────────────────
  void addDonation(Donation d) {
    donations.insert(0, d);
    notifyListeners();
  }

  void requestDonation(String donationId, {int? quantity, String? message}) {
    final donation = donations.firstWhere((d) => d.id == donationId);
    requests.add(FoodRequest(
      id: 'REQ-${DateTime.now().millisecondsSinceEpoch}',
      donationId: donationId,
      receiverId: currentUser.id,
      receiverName: currentUser.name,
      quantity: quantity ?? donation.quantity,
      message: message,
      distance: donation.distance,
      status: 'PENDING',
      requestedAt: DateTime.now(),
    ));
    int idx = donations.indexWhere((d) => d.id == donationId);
    if (idx != -1) {
      donations[idx] = donations[idx].copyWith(status: 'REQUESTED');
    }
    // Add notification
    donorNotifications.insert(0, AppNotification(
      id: 'n-${DateTime.now().millisecondsSinceEpoch}',
      title: 'New request received',
      description: '${currentUser.name} requested ${donation.quantity} ${donation.unit} of ${donation.title}.',
      timestamp: DateTime.now(),
      type: 'request',
      icon: Icons.inbox,
      isRead: false,
    ));
    notifyListeners();
  }

  void updateRequestStatus(String requestId, String newStatus) {
    int reqIdx = requests.indexWhere((r) => r.id == requestId);
    if (reqIdx != -1) {
      requests[reqIdx] = requests[reqIdx].copyWith(status: newStatus);
      String donationId = requests[reqIdx].donationId;
      int donIdx = donations.indexWhere((d) => d.id == donationId);
      if (donIdx != -1) {
        if (newStatus == 'APPROVED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'APPROVED');
        } else if (newStatus == 'REJECTED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'AVAILABLE');
        } else if (newStatus == 'COMPLETED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'COMPLETED');
        }
      }
      notifyListeners();
    }
  }

  void markNotificationAsRead(String id) {
    if (currentUser.role == 'DONOR') {
      int idx = donorNotifications.indexWhere((n) => n.id == id);
      if (idx != -1) {
        donorNotifications[idx] = donorNotifications[idx].copyWith(isRead: true);
        notifyListeners();
      }
    } else {
      int idx = receiverNotifications.indexWhere((n) => n.id == id);
      if (idx != -1) {
        receiverNotifications[idx] = receiverNotifications[idx].copyWith(isRead: true);
        notifyListeners();
      }
    }
  }

  void markAllNotificationsAsRead() {
    if (currentUser.role == 'DONOR') {
      donorNotifications = donorNotifications.map((n) => n.copyWith(isRead: true)).toList();
    } else {
      receiverNotifications = receiverNotifications.map((n) => n.copyWith(isRead: true)).toList();
    }
    notifyListeners();
  }

  Donation _mapBackendDonation(Map<String, dynamic> json) {
    IconData icon = Icons.restaurant;
    final cat = (json['foodCategory'] ?? 'Meals').toString();
    switch (cat.toUpperCase()) {
      case 'MEALS':
      case 'PREPARED_MEALS':
        icon = Icons.rice_bowl;
        break;
      case 'PACKAGED_FOOD':
        icon = Icons.inventory_2;
        break;
      case 'BAKERY':
        icon = Icons.bakery_dining;
        break;
      case 'FRUITS':
      case 'VEGETABLES':
        icon = Icons.eco;
        break;
      case 'GRAINS':
        icon = Icons.grain;
        break;
      case 'DAIRY':
        icon = Icons.egg_alt;
        break;
      case 'BEVERAGES':
        icon = Icons.local_drink;
        break;
      default:
        icon = Icons.restaurant;
    }

    DateTime? consumeBeforeTime;
    if (json['consumeBefore'] != null) {
      try {
        consumeBeforeTime = DateTime.parse(json['consumeBefore'].toString()).toLocal();
      } catch (_) {}
    }

    DateTime createdAt = DateTime.now();
    if (json['createdAt'] != null) {
      try {
        createdAt = DateTime.parse(json['createdAt'].toString()).toLocal();
      } catch (_) {}
    }

    return Donation(
      id: json['id']?.toString() ?? '',
      donorId: json['donorId']?.toString() ?? currentUser.id,
      donorName: json['donorEntityName']?.toString() ?? currentUser.name,
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      quantity: (json['quantity'] is num) ? (json['quantity'] as num).toInt() : 0,
      unit: json['quantityUnit']?.toString() ?? 'meals',
      category: cat,
      consumeBefore: consumeBeforeTime != null ? DateFormat('MMM d, h:mm a').format(consumeBeforeTime) : 'Today',
      consumeBeforeTime: consumeBeforeTime,
      storageCondition: json['storageCondition']?.toString() ?? 'Room Temperature',
      packagingCondition: json['packagingCondition']?.toString() ?? 'Sealed',
      hasAllergens: json['hasAllergens'] == true,
      allergenInfo: json['allergenInfo']?.toString() ?? '',
      foodSafetyDeclared: json['foodSafetyDeclared'] == true,
      pickupAddress: json['pickupAddress']?.toString() ?? '',
      pickupWindowStart: json['pickupAvailableFrom'] != null ? json['pickupAvailableFrom'].toString() : '',
      pickupWindowEnd: json['pickupAvailableUntil'] != null ? json['pickupAvailableUntil'].toString() : '',
      pickupInstructions: json['pickupInstructions']?.toString() ?? '',
      status: (json['status'] ?? 'AVAILABLE').toString().toUpperCase(),
      createdAt: createdAt,
      categoryIcon: icon,
      distance: (json['distanceKm'] is num) ? (json['distanceKm'] as num).toDouble() : null,
    );
  }

  FoodRequest _mapBackendRequest(Map<String, dynamic> json) {
    DateTime reqAt = DateTime.now();
    if (json['requestedAt'] != null) {
      try {
        reqAt = DateTime.parse(json['requestedAt'].toString()).toLocal();
      } catch (_) {}
    }
    return FoodRequest(
      id: json['id']?.toString() ?? json['requestId']?.toString() ?? '',
      donationId: json['donationId']?.toString() ?? '',
      donationTitle: json['donationTitle']?.toString(),
      receiverId: json['receiverOrgId']?.toString() ?? json['receiverOrganizationId']?.toString() ?? currentUser.id,
      receiverName: json['receiverOrgName']?.toString() ?? json['receiverOrganizationName']?.toString() ?? 'Receiver',
      quantity: (json['requestedQuantity'] is num) ? (json['requestedQuantity'] as num).toInt() : 0,
      message: json['requestReason']?.toString(),
      status: (json['status'] ?? 'PENDING').toString().toUpperCase(),
      requestedAt: reqAt,
    );
  }

  // ─── Real Backend Integration Methods ──────
  Future<void> fetchDonorDonations() async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = "API client not initialized. Cannot reach backend.";
        donations = [];
        notifyListeners();
      }
      return;
    }
    isLoading = true;
    donorDonationsError = null;
    notifyListeners();
    try {
      final res = await apiClient!.get(ApiConfig.donorDonationsUrl);
      if (res is Map<String, dynamic> && res['content'] is List) {
        donations = (res['content'] as List).map((json) => _mapBackendDonation(json as Map<String, dynamic>)).toList();
      } else {
        donations = [];
      }
    } on ApiException catch (e) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = e.message;
        donations = [];
      }
    } catch (e) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = "Backend unavailable. Could not fetch donations.";
        donations = [];
      }
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  String _mapCategoryToBackend(String cat) {
    switch (cat.toUpperCase()) {
      case 'MEALS':
      case 'PREPARED MEALS':
      case 'PREPARED_MEALS':
        return 'PREPARED_MEALS';
      case 'PACKAGED':
      case 'PACKAGED FOOD':
      case 'PACKAGED_FOOD':
        return 'PACKAGED_FOOD';
      case 'BAKERY':
        return 'BAKERY';
      case 'FRUITS':
        return 'FRUITS';
      case 'VEGETABLES':
        return 'VEGETABLES';
      case 'GRAINS':
        return 'GRAINS';
      case 'DAIRY':
        return 'DAIRY';
      default:
        return 'OTHER';
    }
  }

  Future<Donation> createDonationRemote({
    required String title,
    required String description,
    required int quantity,
    required String unit,
    required String category,
    required String storageCondition,
    required String packagingCondition,
    required bool hasAllergens,
    required String allergenInfo,
    required bool foodSafetyDeclared,
    required String pickupAddress,
    required DateTime pickupAvailableFrom,
    required DateTime pickupAvailableUntil,
    required DateTime consumeBefore,
    DateTime? preparationDateTime,
    double latitude = 12.9716,
    double longitude = 77.5946,
  }) async {
    final now = DateTime.now();
    final effectivePrep = preparationDateTime ??
        (pickupAvailableFrom.isAfter(now)
            ? now
            : pickupAvailableFrom.subtract(const Duration(minutes: 30)));

    final payload = {
      'title': title,
      'description': description,
      'foodCategory': _mapCategoryToBackend(category),
      'quantity': quantity.toDouble(),
      'quantityUnit': unit,
      'preparationDateTime': effectivePrep.toUtc().toIso8601String(),
      'storageCondition': storageCondition,
      'packagingCondition': packagingCondition,
      'hasAllergens': hasAllergens,
      'allergenInfo': allergenInfo,
      'foodSafetyDeclared': foodSafetyDeclared,
      'foodSafetyDeclarationText': 'I declare this food was prepared following standard food hygiene protocols.',
      'pickupAddress': pickupAddress,
      'pickupAvailableFrom': pickupAvailableFrom.toUtc().toIso8601String(),
      'pickupAvailableUntil': pickupAvailableUntil.toUtc().toIso8601String(),
      'consumeBefore': consumeBefore.toUtc().toIso8601String(),
      'latitude': latitude,
      'longitude': longitude,
    };

    if (apiClient != null) {
      final res = await apiClient!.post(ApiConfig.donorDonationsUrl, body: payload);
      if (res is Map<String, dynamic>) {
        final d = _mapBackendDonation(res);
        donations.insert(0, d);
        notifyListeners();
        return d;
      }
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot connect to backend.");
    }

    if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 500, message: "Failed to create donation on real backend.");
    }

    final localDonation = Donation(
      id: 'DON-${DateTime.now().millisecondsSinceEpoch}',
      donorId: currentUser.id,
      donorName: currentUser.name,
      title: title,
      description: description,
      quantity: quantity,
      unit: unit,
      category: category,
      consumeBefore: DateFormat('MMM d, h:mm a').format(consumeBefore),
      consumeBeforeTime: consumeBefore,
      storageCondition: storageCondition,
      packagingCondition: packagingCondition,
      hasAllergens: hasAllergens,
      allergenInfo: allergenInfo,
      foodSafetyDeclared: foodSafetyDeclared,
      pickupAddress: pickupAddress,
      pickupWindowStart: DateFormat('h:mm a').format(pickupAvailableFrom),
      pickupWindowEnd: DateFormat('h:mm a').format(pickupAvailableUntil),
      status: 'AVAILABLE',
      createdAt: DateTime.now(),
    );
    donations.insert(0, localDonation);
    notifyListeners();
    return localDonation;
  }

  Future<void> cancelDonationRemote(String donationId) async {
    if (apiClient != null) {
      await apiClient!.post(ApiConfig.donorDonationCancelUrl(donationId));
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    final idx = donations.indexWhere((d) => d.id == donationId);
    if (idx != -1) {
      donations[idx] = donations[idx].copyWith(status: 'CANCELLED');
      notifyListeners();
    }
  }

  Future<List<FoodRequest>> fetchDonorRequestsForDonation(String donationId) async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
      }
      return requests.where((r) => r.donationId == donationId).toList();
    }
    try {
      final res = await apiClient!.get(ApiConfig.donorRequestsForDonationUrl(donationId));
      if (res is Map<String, dynamic> && res['content'] is List) {
        final items = (res['content'] as List).map((j) => _mapBackendRequest(j as Map<String, dynamic>)).toList();
        for (final item in items) {
          final existingIdx = requests.indexWhere((r) => r.id == item.id);
          if (existingIdx != -1) {
            requests[existingIdx] = item;
          } else {
            requests.add(item);
          }
        }
        notifyListeners();
        return items;
      }
    } on ApiException catch (_) {
      if (ApiConfig.isRealMode) rethrow;
    } catch (e) {
      if (ApiConfig.isRealMode) throw ApiException(statusCode: 500, message: "Failed to load requests from backend.");
    }
    if (ApiConfig.isRealMode) return [];
    return requests.where((r) => r.donationId == donationId).toList();
  }

  Future<void> fetchIncomingRequestsForDonor() async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = "API client not initialized. Cannot reach backend.";
        requests = [];
        notifyListeners();
      }
      return;
    }
    isLoading = true;
    notifyListeners();
    try {
      if (donations.isEmpty) {
        await fetchDonorDonations();
      }
      final List<FoodRequest> allReqs = [];
      for (final d in donations) {
        try {
          final res = await apiClient!.get(ApiConfig.donorRequestsForDonationUrl(d.id));
          if (res is Map<String, dynamic> && res['content'] is List) {
            final items = (res['content'] as List)
                .map((j) => _mapBackendRequest(j as Map<String, dynamic>))
                .toList();
            allReqs.addAll(items);
          }
        } catch (_) {}
      }
      requests = allReqs;
    } on ApiException catch (e) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = e.message;
      }
    } catch (_) {
      if (ApiConfig.isRealMode) {
        donorDonationsError = "Failed to load requests from backend.";
      }
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<void> approveDonorRequestRemote(String requestId) async {
    if (apiClient != null) {
      await apiClient!.post(ApiConfig.donorApproveRequestUrl(requestId));
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    updateRequestStatus(requestId, 'APPROVED');
  }

  Future<void> rejectDonorRequestRemote(String requestId, [String? reason]) async {
    if (apiClient != null) {
      await apiClient!.post(
        ApiConfig.donorRejectRequestUrl(requestId),
        body: reason != null ? {'rejectionReason': reason} : null,
      );
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    updateRequestStatus(requestId, 'REJECTED');
  }

  Future<List<Map<String, dynamic>>> fetchDonorPickupsForDonationRemote(String donationId) async {
    if (apiClient != null) {
      final res = await apiClient!.get(ApiConfig.donorPickupsByDonationUrl(donationId));
      if (res is List) {
        return List<Map<String, dynamic>>.from(res);
      }
    }
    return [];
  }

  Future<List<Map<String, dynamic>>> fetchDonorPickupsRemote() async {
    if (apiClient != null) {
      final res = await apiClient!.get(ApiConfig.donorPickupsUrl);
      if (res is Map<String, dynamic> && res['content'] is List) {
        return List<Map<String, dynamic>>.from(res['content']);
      } else if (res is List) {
        return List<Map<String, dynamic>>.from(res);
      }
    }
    return [];
  }

  Future<Map<String, dynamic>> fetchDonorPickupOtpRemote(String pickupId) async {
    if (apiClient != null) {
      final res = await apiClient!.get(ApiConfig.donorPickupOtpUrl(pickupId));
      if (res is Map<String, dynamic>) {
        return res;
      }
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    return {};
  }

  Future<void> updateDonationRemote(String id, {
    String? title,
    String? description,
    double? quantity,
    String? pickupAddress,
    DateTime? pickupAvailableUntil,
    DateTime? consumeBefore,
  }) async {
    if (apiClient != null) {
      final body = <String, dynamic>{};
      if (title != null) body['title'] = title;
      if (description != null) body['description'] = description;
      if (quantity != null) body['quantity'] = quantity;
      if (pickupAddress != null) body['pickupAddress'] = pickupAddress;
      if (pickupAvailableUntil != null) body['pickupAvailableUntil'] = pickupAvailableUntil.toUtc().toIso8601String();
      if (consumeBefore != null) body['consumeBefore'] = consumeBefore.toUtc().toIso8601String();
      await apiClient!.patch(ApiConfig.donorDonationDetailUrl(id), body: body);
      await fetchDonorDonations();
    }
  }

  Future<void> fetchDonorProfileRemote() async {
    if (apiClient == null) return;
    try {
      final prof = await apiClient!.get(ApiConfig.donorProfileUrl);
      final me = await apiClient!.get(ApiConfig.usersMeUrl);
      if (prof is Map<String, dynamic>) {
        final meMap = me is Map<String, dynamic> ? me : <String, dynamic>{};
        currentUser = UserProfile(
          id: prof['id']?.toString() ?? currentUser.id,
          name: prof['entityName']?.toString() ?? meMap['name']?.toString() ?? currentUser.name,
          role: 'DONOR',
          email: meMap['email']?.toString() ?? currentUser.email,
          phone: meMap['phone']?.toString() ?? currentUser.phone,
          address: prof['addressText']?.toString() ?? currentUser.address,
          organizationType: prof['donorType']?.toString() ?? currentUser.organizationType,
          isVerified: meMap['status']?.toString() == 'ACTIVE',
          joinedAt: prof['createdAt'] != null
              ? DateTime.tryParse(prof['createdAt'].toString()) ?? currentUser.joinedAt
              : currentUser.joinedAt,
          ratingAverage: (prof['ratingAverage'] as num?)?.toDouble() ?? 5.0,
          completedCount: (prof['completedDonationsCount'] as num?)?.toInt() ?? 0,
          latitude: (prof['latitude'] as num?)?.toDouble(),
          longitude: (prof['longitude'] as num?)?.toDouble(),
        );
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> updateDonorProfileRemote({
    required String entityName,
    required String donorType,
    required String addressText,
    double latitude = 12.9716,
    double longitude = 77.5946,
    bool policyAccepted = true,
  }) async {
    if (apiClient != null) {
      await apiClient!.put(
        ApiConfig.donorProfileUrl,
        body: {
          'donorType': donorType,
          'entityName': entityName,
          'addressText': addressText,
          'latitude': latitude,
          'longitude': longitude,
          'policyAccepted': policyAccepted,
        },
      );
      await fetchDonorProfileRemote();
    }
  }

  Future<void> fetchReceiverProfileRemote() async {
    if (apiClient == null) return;
    try {
      final prof = await apiClient!.get(ApiConfig.receiverProfileUrl);
      final me = await apiClient!.get(ApiConfig.usersMeUrl);
      if (prof is Map<String, dynamic>) {
        final meMap = me is Map<String, dynamic> ? me : <String, dynamic>{};
        currentUser = UserProfile(
          id: prof['id']?.toString() ?? currentUser.id,
          name: prof['orgName']?.toString() ?? meMap['name']?.toString() ?? currentUser.name,
          role: 'RECEIVER',
          email: meMap['email']?.toString() ?? currentUser.email,
          phone: meMap['phone']?.toString() ?? currentUser.phone,
          address: prof['addressText']?.toString() ?? currentUser.address,
          organizationType: prof['orgType']?.toString() ?? currentUser.organizationType,
          isVerified: prof['verificationStatus']?.toString() == 'APPROVED',
          joinedAt: prof['createdAt'] != null
              ? DateTime.tryParse(prof['createdAt'].toString()) ?? currentUser.joinedAt
              : currentUser.joinedAt,
          ratingAverage: (prof['ratingAverage'] as num?)?.toDouble() ?? 5.0,
          completedCount: 0,
          registrationNumber: prof['registrationNumber']?.toString(),
          beneficiaryCapacity: (prof['beneficiaryCapacity'] as num?)?.toInt(),
          serviceRadiusMeters: (prof['serviceRadiusMeters'] as num?)?.toDouble(),
          latitude: (prof['serviceLatitude'] as num?)?.toDouble(),
          longitude: (prof['serviceLongitude'] as num?)?.toDouble(),
        );
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> updateReceiverProfileRemote({
    required String orgName,
    required String orgType,
    required String registrationNumber,
    required String addressText,
    double serviceLatitude = 12.9716,
    double serviceLongitude = 77.5946,
    double serviceRadiusMeters = 10000.0,
    int beneficiaryCapacity = 50,
  }) async {
    if (apiClient != null) {
      await apiClient!.put(
        ApiConfig.receiverProfileUrl,
        body: {
          'orgType': orgType,
          'orgName': orgName,
          'registrationNumber': registrationNumber,
          'addressText': addressText,
          'serviceLatitude': serviceLatitude,
          'serviceLongitude': serviceLongitude,
          'serviceRadiusMeters': serviceRadiusMeters,
          'beneficiaryCapacity': beneficiaryCapacity,
        },
      );
      await fetchReceiverProfileRemote();
    }
  }

  Future<void> fetchNearbyDonations({
    double latitude = 12.9716,
    double longitude = 77.5946,
    double radiusKm = 15.0,
    String? category,
  }) async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        nearbyDonationsError = "API client not initialized. Cannot reach backend.";
        donations = [];
        notifyListeners();
      }
      return;
    }
    isLoading = true;
    nearbyDonationsError = null;
    notifyListeners();
    try {
      final qParams = <String, dynamic>{
        'latitude': latitude,
        'longitude': longitude,
        'radiusKm': radiusKm,
        if (category != null && category != 'All') 'foodCategory': category.toUpperCase(),
      };
      final res = await apiClient!.get(ApiConfig.receiverNearbyDonationsUrl, queryParameters: qParams);
      if (res is Map<String, dynamic> && res['content'] is List) {
        final items = (res['content'] as List).map((j) => _mapBackendDonation(j as Map<String, dynamic>)).toList();
        donations = items;
      } else {
        donations = [];
      }
    } on ApiException catch (e) {
      if (ApiConfig.isRealMode) {
        nearbyDonationsError = e.message;
        donations = [];
      }
    } catch (e) {
      if (ApiConfig.isRealMode) {
        nearbyDonationsError = "Backend unavailable. Could not fetch nearby donations.";
        donations = [];
      }
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<FoodRequest> requestDonationRemote(String donationId, {required double quantity, String? notes}) async {
    if (apiClient != null) {
      final res = await apiClient!.post(
        ApiConfig.receiverCreateRequestUrl(donationId),
        body: {
          'requestedQuantity': quantity,
          if (notes != null && notes.isNotEmpty) 'requestReason': notes,
        },
      );
      if (res is Map<String, dynamic>) {
        final req = _mapBackendRequest(res);
        requests.insert(0, req);
        final dIdx = donations.indexWhere((d) => d.id == donationId);
        if (dIdx != -1) {
          donations[dIdx] = donations[dIdx].copyWith(status: 'REQUESTED');
        }
        notifyListeners();
        return req;
      }
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot connect to backend.");
    }

    if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 500, message: "Failed to submit request on real backend.");
    }

    requestDonation(donationId, quantity: quantity.toInt(), message: notes);
    return requests.firstWhere((r) => r.donationId == donationId);
  }

  Future<void> fetchReceiverRequests() async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        receiverRequestsError = "API client not initialized. Cannot reach backend.";
        requests = [];
        notifyListeners();
      }
      return;
    }
    isLoading = true;
    receiverRequestsError = null;
    notifyListeners();
    try {
      final res = await apiClient!.get(ApiConfig.receiverRequestsUrl);
      if (res is Map<String, dynamic> && res['content'] is List) {
        final items = (res['content'] as List).map((j) => _mapBackendRequest(j as Map<String, dynamic>)).toList();
        requests = items;
      } else {
        requests = [];
      }
    } on ApiException catch (e) {
      if (ApiConfig.isRealMode) {
        receiverRequestsError = e.message;
        requests = [];
      }
    } catch (e) {
      if (ApiConfig.isRealMode) {
        receiverRequestsError = "Backend unavailable. Could not fetch requests.";
        requests = [];
      }
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<void> withdrawReceiverRequestRemote(String requestId) async {
    if (apiClient != null) {
      await apiClient!.post(ApiConfig.receiverWithdrawRequestUrl(requestId));
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    updateRequestStatus(requestId, 'WITHDRAWN');
  }

  Future<void> fetchNotificationsRemote() async {
    if (apiClient == null) {
      if (ApiConfig.isRealMode) {
        notificationsError = "API client not initialized. Cannot reach backend.";
        donorNotifications = [];
        receiverNotifications = [];
        notifyListeners();
      }
      return;
    }
    isNotificationsLoading = true;
    notificationsError = null;
    notifyListeners();
    try {
      final res = await apiClient!.get(ApiConfig.notificationsUrl);
      if (res is Map<String, dynamic> && res['content'] is List) {
        final items = (res['content'] as List)
            .map((j) => AppNotification.fromJson(j as Map<String, dynamic>))
            .toList();
        if (currentUser.role == 'DONOR') {
          donorNotifications = items;
        } else {
          receiverNotifications = items;
        }
        unreadNotificationsCount = items.where((n) => !n.isRead).length;
      } else {
        if (currentUser.role == 'DONOR') {
          donorNotifications = [];
        } else {
          receiverNotifications = [];
        }
        unreadNotificationsCount = 0;
      }
    } on ApiException catch (e) {
      if (ApiConfig.isRealMode) {
        notificationsError = e.message;
        if (currentUser.role == 'DONOR') {
          donorNotifications = [];
        } else {
          receiverNotifications = [];
        }
      }
    } catch (e) {
      if (ApiConfig.isRealMode) {
        notificationsError = "Backend unavailable. Could not fetch notifications.";
        if (currentUser.role == 'DONOR') {
          donorNotifications = [];
        } else {
          receiverNotifications = [];
        }
      }
    } finally {
      isNotificationsLoading = false;
      notifyListeners();
    }
  }

  Future<void> markNotificationAsReadRemote(String id) async {
    if (apiClient != null) {
      try {
        await apiClient!.patch(ApiConfig.notificationReadUrl(id));
      } on ApiException catch (_) {
        if (ApiConfig.isRealMode) rethrow;
      }
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    markNotificationAsRead(id);
    if (unreadNotificationsCount > 0) {
      unreadNotificationsCount--;
    }
  }

  Future<void> markAllNotificationsAsReadRemote() async {
    if (apiClient != null) {
      try {
        await apiClient!.post(ApiConfig.notificationsReadAllUrl);
      } on ApiException catch (_) {
        if (ApiConfig.isRealMode) rethrow;
      }
    } else if (ApiConfig.isRealMode) {
      throw ApiException(statusCode: 503, message: "API client not initialized. Cannot reach backend.");
    }
    markAllNotificationsAsRead();
    unreadNotificationsCount = 0;
  }

  Future<int> fetchUnreadCountRemote() async {
    if (apiClient == null) return unreadNotificationsCount;
    try {
      final res = await apiClient!.get(ApiConfig.notificationsUnreadCountUrl);
      if (res is Map<String, dynamic> && res['unreadCount'] != null) {
        unreadNotificationsCount = (res['unreadCount'] as num).toInt();
        notifyListeners();
        return unreadNotificationsCount;
      }
    } catch (_) {}
    return unreadNotificationsCount;
  }
}
