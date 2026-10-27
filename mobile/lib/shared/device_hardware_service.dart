import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

class DeviceHardwareService {
  static final ImagePicker _picker = ImagePicker();

  /// Retrieve real device GPS coordinates using Geolocator
  static Future<Position?> getCurrentLocation() async {
    try {
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        // Location service is turned off on device
        return null;
      }

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) {
          return null;
        }
      }

      if (permission == LocationPermission.deniedForever) {
        return null;
      }

      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 10),
        ),
      );
    } catch (e) {
      debugPrint('Geolocator error: $e');
      return null;
    }
  }

  /// Launch external turn-by-turn navigation (Google Maps) to destination coordinates.
  /// Accepts optional origin coordinates (from receiver GPS) and ensures valid donor destination.
  static Future<bool> openTurnByTurnNavigation({
    required double latitude,
    required double longitude,
    double? originLatitude,
    double? originLongitude,
    String? destinationLabel,
  }) async {
    // Validate destination coordinates
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0 || (latitude == 0.0 && longitude == 0.0)) {
      debugPrint('Invalid navigation coordinates: lat=$latitude, lon=$longitude');
      return false;
    }

    try {
      // 1. Google Maps Navigation scheme (standard on Android devices)
      final googleMapsNavUri = Uri.parse('google.navigation:q=$latitude,$longitude&mode=d');
      if (await canLaunchUrl(googleMapsNavUri)) {
        return await launchUrl(googleMapsNavUri, mode: LaunchMode.externalApplication);
      }

      // 2. Official Google Maps Directions Web/App URL
      final originQuery = (originLatitude != null && originLongitude != null)
          ? '&origin=$originLatitude,$originLongitude'
          : '';
      final labelQuery = destinationLabel != null ? '&destination_place_id=${Uri.encodeComponent(destinationLabel)}' : '';
      final mapsWebUri = Uri.parse(
        'https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude$originQuery$labelQuery&travelmode=driving',
      );
      if (await canLaunchUrl(mapsWebUri)) {
        return await launchUrl(mapsWebUri, mode: LaunchMode.externalApplication);
      }

      // 3. Fallback Geo URI scheme
      final geoUri = Uri.parse(
        'geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encodeComponent(destinationLabel ?? "Pickup Destination")})',
      );
      if (await canLaunchUrl(geoUri)) {
        return await launchUrl(geoUri, mode: LaunchMode.externalApplication);
      }

      return false;
    } catch (e) {
      debugPrint('Error launching turn-by-turn navigation: $e');
      return false;
    }
  }

  /// Capture a real camera photo (with gallery fallback) and upload to backend evidence storage
  static Future<String?> capturePhotoAndUpload({
    required BuildContext context,
    required ApiClient apiClient,
    ImageSource source = ImageSource.camera,
  }) async {
    try {
      final XFile? photo = await _picker.pickImage(
        source: source,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 75,
      );

      if (photo == null) return null;

      final bytes = await photo.readAsBytes();
      final base64Str = base64Encode(bytes);

      final response = await apiClient.post(
        ApiConfig.evidenceUploadBase64Url,
        body: {'photoBase64': base64Str},
      );

      if (response is Map<String, dynamic> && response['photoUrl'] != null) {
        return response['photoUrl'].toString();
      }
      return null;
    } catch (e) {
      debugPrint('Photo upload error: $e');
      rethrow;
    }
  }
}
