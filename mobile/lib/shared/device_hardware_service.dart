import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
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
