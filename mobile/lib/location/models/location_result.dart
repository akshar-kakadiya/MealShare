class LocationResult {
  final double latitude;
  final double longitude;
  final String label;
  final String houseBuilding;
  final String streetArea;
  final String? landmark;
  final String fullAddress;

  const LocationResult({
    required this.latitude,
    required this.longitude,
    required this.label,
    required this.houseBuilding,
    required this.streetArea,
    this.landmark,
    required this.fullAddress,
  });

  /// Format a clean, human-readable address from structured components
  static String formatFullAddress({
    required String label,
    required String houseBuilding,
    required String streetArea,
    String? landmark,
  }) {
    final parts = <String>[];
    if (label.isNotEmpty && label.toLowerCase() != 'other') {
      parts.add('[$label]');
    }
    if (houseBuilding.trim().isNotEmpty) {
      parts.add(houseBuilding.trim());
    }
    if (streetArea.trim().isNotEmpty) {
      parts.add(streetArea.trim());
    }
    if (landmark != null && landmark.trim().isNotEmpty) {
      parts.add('Near ${landmark.trim()}');
    }
    return parts.join(', ');
  }

  @override
  String toString() {
    return 'LocationResult(lat: $latitude, lon: $longitude, address: $fullAddress)';
  }
}
