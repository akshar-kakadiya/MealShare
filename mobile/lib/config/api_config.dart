import 'package:flutter/foundation.dart';
enum DataMode {
  real,
  mock,
}

class ApiConfig {
  static String? _customBaseUrl;

  /// Operational data mode: `real` (connect to Spring Boot) or `mock` (explicit offline prototype).
  /// Default is `real`.
  static DataMode dataMode = const String.fromEnvironment('DATA_MODE', defaultValue: 'real').toLowerCase() == 'mock'
      ? DataMode.mock
      : DataMode.real;

  static bool get isRealMode => dataMode == DataMode.real;
  static bool get isMockMode => dataMode == DataMode.mock;

  /// Allows overriding the backend base URL at runtime (e.g. for physical devices or custom environments)
  static void setBaseUrl(String url) {
    _customBaseUrl = url;
  }

  /// Default cloud backend URL deployed on Render
  static const String defaultCloudUrl = 'https://mealshare-backend.onrender.com';

  /// Automatically resolves base URL depending on platform:
  /// - Environment define: `BACKEND_URL` (highest priority)
  /// - Cloud switch: `USE_CLOUD=true` -> https://mealshare-backend.onrender.com
  /// - Web local: http://localhost:8081
  /// - Android emulator: http://10.0.2.2:8081
  /// - iOS / Desktop / Reverse-tethered physical phone: http://127.0.0.1:8081
  static String get baseUrl {
    if (_customBaseUrl != null && _customBaseUrl!.isNotEmpty) {
      return _customBaseUrl!;
    }
    const envUrl = String.fromEnvironment('BACKEND_URL', defaultValue: '');
    if (envUrl.isNotEmpty) {
      return envUrl;
    }
    const useCloud = bool.fromEnvironment('USE_CLOUD', defaultValue: false);
    if (useCloud) {
      return defaultCloudUrl;
    }
    const port = String.fromEnvironment('BACKEND_PORT', defaultValue: '8081');
    if (kIsWeb) {
      return 'http://localhost:$port';
    }
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:$port';
    }
    return 'http://127.0.0.1:$port';
  }

  static const String apiPrefix = '/api/v1';

  // Common endpoints
  static String get healthUrl => '$baseUrl$apiPrefix/health';
  static String get usersMeUrl => '$baseUrl$apiPrefix/users/me';
  static String get usersRegisterUrl => '$baseUrl$apiPrefix/users/register';

  // Donor endpoints
  static String get donorDonationsUrl => '$baseUrl$apiPrefix/donor/donations';
  static String donorDonationDetailUrl(String id) => '$donorDonationsUrl/$id';
  static String donorDonationCancelUrl(String id) => '$donorDonationsUrl/$id/cancel';
  static String donorRequestsForDonationUrl(String donationId) => '$donorDonationsUrl/$donationId/requests';
  static String donorApproveRequestUrl(String requestId) => '$baseUrl$apiPrefix/donor/requests/$requestId/approve';
  static String donorRejectRequestUrl(String requestId) => '$baseUrl$apiPrefix/donor/requests/$requestId/reject';
  static String get donorPickupsUrl => '$baseUrl$apiPrefix/donor/pickups';
  static String donorPickupsByDonationUrl(String donationId) => '$donorPickupsUrl/by-donation/$donationId';
  static String donorPickupDetailUrl(String id) => '$donorPickupsUrl/$id';
  static String donorPickupOtpUrl(String id) => '$donorPickupsUrl/$id/otp';
  static String get donorProfileUrl => '$baseUrl$apiPrefix/donor/profile';

  // Receiver endpoints
  static String get receiverNearbyDonationsUrl => '$baseUrl$apiPrefix/receiver/donations/nearby';
  static String receiverDonationDetailUrl(String id) => '$baseUrl$apiPrefix/receiver/donations/$id';
  static String receiverCreateRequestUrl(String donationId) => '$baseUrl$apiPrefix/receiver/donations/$donationId/requests';
  static String get receiverRequestsUrl => '$baseUrl$apiPrefix/receiver/requests';
  static String receiverRequestDetailUrl(String id) => '$receiverRequestsUrl/$id';
  static String receiverWithdrawRequestUrl(String id) => '$receiverRequestsUrl/$id/withdraw';
  static String get receiverProfileUrl => '$baseUrl$apiPrefix/receiver/profile';

  // Receiver pickups endpoints (direct food collection & operational redistribution)
  static String get receiverPickupsUrl => '$baseUrl$apiPrefix/receiver/pickups';
  static String receiverPickupDetailUrl(String id) => '$receiverPickupsUrl/$id';
  static String receiverEnsurePickupForRequestUrl(String requestId) => '$receiverPickupsUrl/ensure-for-request/$requestId';
  static String receiverStartJourneyUrl(String id) => '$receiverPickupsUrl/$id/start-journey';
  static String receiverLocationUrl(String id) => '$receiverPickupsUrl/$id/location';
  static String receiverReachedLocationUrl(String id) => '$receiverPickupsUrl/$id/reached-location';
  static String receiverInspectFoodUrl(String id) => '$receiverPickupsUrl/$id/inspect-food';
  static String receiverVerifyAndPickupUrl(String id) => '$receiverPickupsUrl/$id/verify-and-pickup';
  static String receiverStartDeliveryUrl(String id) => '$receiverPickupsUrl/$id/start-delivery';
  static String receiverCompleteDeliveryUrl(String id) => '$receiverPickupsUrl/$id/complete-delivery';
  static String receiverTrackingUrl(String id) => '$receiverPickupsUrl/$id/tracking';
  static String receiverVerifyOtpUrl(String id) => '$receiverPickupsUrl/$id/verify-otp';
  static String receiverCompletePickupUrl(String id) => '$receiverPickupsUrl/$id/complete';

  // Donor tracking endpoint
  static String donorTrackingUrl(String id) => '$donorPickupsUrl/$id/tracking';

  // Evidence photo endpoints
  static String get evidenceUploadUrl => '$baseUrl$apiPrefix/evidence/upload';
  static String get evidenceUploadBase64Url => '$baseUrl$apiPrefix/evidence/upload-base64';
  static String evidenceFileUrl(String filename) => '$baseUrl$apiPrefix/evidence/$filename';

  // Admin endpoints (for reference)
  static String get adminPickupsUrl => '$baseUrl$apiPrefix/admin/pickups';
  static String adminPickupDetailUrl(String id) => '$adminPickupsUrl/$id';
  static String adminTrackingUrl(String id) => '$adminPickupsUrl/$id/tracking';

  // Ratings endpoints
  static String get ratingsUrl => '$baseUrl$apiPrefix/ratings';
  static String pickupRatingStatusUrl(String pickupId) => '$ratingsUrl/pickup/$pickupId';

  // Notifications endpoints
  static String get notificationsUrl => '$baseUrl$apiPrefix/notifications';
  static String get notificationsUnreadCountUrl => '$baseUrl$apiPrefix/notifications/unread-count';
  static String notificationReadUrl(String id) => '$notificationsUrl/$id/read';
  static String get notificationsReadAllUrl => '$notificationsUrl/read-all';
  static String get notificationsDeviceTokenUrl => '$notificationsUrl/device-token';
}
