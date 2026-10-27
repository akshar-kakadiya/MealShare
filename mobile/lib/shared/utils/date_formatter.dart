import 'package:intl/intl.dart';

/// Centralized Date and Time formatting utility for MealShare.
///
/// Rules:
/// - Date format: dd-MM-yyyy (e.g. 26-10-2026)
/// - Time format: 12-hour clock with AM/PM (e.g. 8:00 PM)
/// - Combined DateTime: 12-hour clock, dd-MM-yyyy (e.g. 11:00 PM, 26-10-2026)
/// - Always converted to the device's local time zone (.toLocal())
/// - Never displays raw ISO timestamps, UTC 'Z' suffixes, or fractional seconds.
/// - Gracefully handles missing/invalid values with clear fallback ("Not specified").
class AppDateFormatter {
  static final DateFormat _dateFormat = DateFormat('dd-MM-yyyy');
  static final DateFormat _timeFormat = DateFormat('h:mm a');
  static final DateFormat _dateTimeFormat = DateFormat('h:mm a, dd-MM-yyyy');

  /// Parse any dynamic input (DateTime, ISO-8601 String) safely into local DateTime
  static DateTime? parseToLocal(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value.toLocal();
    final str = value.toString().trim();
    if (str.isEmpty || str == 'null' || str == 'Not specified') return null;
    try {
      return DateTime.parse(str).toLocal();
    } catch (_) {
      return null;
    }
  }

  /// Format as date: dd-MM-yyyy (e.g. 26-10-2026)
  static String formatDate(dynamic value, {String fallback = 'Not specified'}) {
    final dt = parseToLocal(value);
    if (dt == null) return fallback;
    return _dateFormat.format(dt);
  }

  /// Format as 12-hour time: h:mm a (e.g. 8:00 PM)
  static String formatTime(dynamic value, {String fallback = 'Not specified'}) {
    final dt = parseToLocal(value);
    if (dt == null) return fallback;
    return _timeFormat.format(dt);
  }

  /// Format as date and time: h:mm a, dd-MM-yyyy (e.g. 11:00 PM, 26-10-2026)
  static String formatDateTime(dynamic value, {String fallback = 'Not specified'}) {
    final dt = parseToLocal(value);
    if (dt == null) return fallback;
    return _dateTimeFormat.format(dt);
  }

  /// Format pickup time range: 8:00 PM – 9:00 PM (or with date if different days)
  static String formatPickupTimeRange(dynamic startVal, dynamic endVal, {String fallback = 'Not specified'}) {
    final start = parseToLocal(startVal);
    final end = parseToLocal(endVal);

    if (start == null && end == null) return fallback;
    if (start != null && end == null) return '${formatTime(start)} onwards';
    if (start == null && end != null) return 'Until ${formatTime(end)}';

    // If on the same date: "8:00 PM – 9:00 PM"
    if (start!.year == end!.year && start.month == end.month && start.day == end.day) {
      return '${_timeFormat.format(start)} – ${_timeFormat.format(end)}';
    }

    // If spanning different dates
    return '${_timeFormat.format(start)} (${_dateFormat.format(start)}) – ${_timeFormat.format(end)} (${_dateFormat.format(end)})';
  }

  /// Format "Consume Before" deadline: 11:00 PM, 26-10-2026
  static String formatConsumeBefore(dynamic value, {String fallback = 'Not specified'}) {
    final dt = parseToLocal(value);
    if (dt == null) return fallback;
    return _dateTimeFormat.format(dt);
  }
}
