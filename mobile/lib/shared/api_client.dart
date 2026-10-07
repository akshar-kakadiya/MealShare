import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final dynamic body;

  ApiException({
    required this.statusCode,
    required this.message,
    this.body,
  });

  bool get isBadRequest => statusCode == 400;
  bool get isUnauthorized => statusCode == 401;
  bool get isForbidden => statusCode == 403;
  bool get isNotFound => statusCode == 404;
  bool get isConflict => statusCode == 409;
  bool get isRateLimited => statusCode == 429;
  bool get isServerError => statusCode >= 500;

  @override
  String toString() => 'ApiException ($statusCode): $message';
}

class ApiClient {
  final AuthService _authService;
  static const Duration timeoutDuration = Duration(seconds: 10);

  ApiClient(this._authService);

  Future<Map<String, String>> _getHeaders() async {
    final token = await _authService.getIdToken();
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };
  }

  Uri _buildUri(String pathOrUrl, [Map<String, dynamic>? queryParameters]) {
    if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
      final uri = Uri.parse(pathOrUrl);
      if (queryParameters != null && queryParameters.isNotEmpty) {
        return uri.replace(
          queryParameters: {
            ...uri.queryParameters,
            ...queryParameters.map((k, v) => MapEntry(k, v.toString())),
          },
        );
      }
      return uri;
    }
    final normalizedPath = pathOrUrl.startsWith('/') ? pathOrUrl : '/$pathOrUrl';
    final baseUri = Uri.parse(ApiConfig.baseUrl);
    return Uri(
      scheme: baseUri.scheme,
      host: baseUri.host,
      port: baseUri.port,
      path: '${baseUri.path}$normalizedPath'.replaceAll('//', '/'),
      queryParameters: queryParameters?.map((k, v) => MapEntry(k, v.toString())),
    );
  }

  dynamic _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      try {
        return jsonDecode(response.body);
      } catch (_) {
        return response.body;
      }
    }

    String message = 'Request failed with status ${response.statusCode}';
    dynamic body;
    if (response.body.isNotEmpty) {
      try {
        body = jsonDecode(response.body);
        if (body is Map<String, dynamic>) {
          if (body['message'] != null) {
            message = body['message'].toString();
          } else if (body['error'] != null) {
            message = body['error'].toString();
          }
        } else if (body is String) {
          message = body;
        }
      } catch (_) {
        message = response.body;
      }
    }

    throw ApiException(
      statusCode: response.statusCode,
      message: message,
      body: body,
    );
  }

  Future<dynamic> get(String pathOrUrl, {Map<String, dynamic>? queryParameters}) async {
    try {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final response = await http.get(uri, headers: headers).timeout(timeoutDuration);
      return _handleResponse(response);
    } on SocketException catch (e) {
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      throw ApiException(statusCode: 408, message: 'Connection timed out. Please check backend status.');
    }
  }

  Future<dynamic> post(String pathOrUrl, {dynamic body, Map<String, dynamic>? queryParameters}) async {
    try {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      final response = await http.post(uri, headers: headers, body: encodedBody).timeout(timeoutDuration);
      return _handleResponse(response);
    } on SocketException catch (e) {
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      throw ApiException(statusCode: 408, message: 'Connection timed out. Please check backend status.');
    }
  }

  Future<dynamic> put(String pathOrUrl, {dynamic body, Map<String, dynamic>? queryParameters}) async {
    try {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      final response = await http.put(uri, headers: headers, body: encodedBody).timeout(timeoutDuration);
      return _handleResponse(response);
    } on SocketException catch (e) {
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      throw ApiException(statusCode: 408, message: 'Connection timed out. Please check backend status.');
    }
  }

  Future<dynamic> patch(String pathOrUrl, {dynamic body, Map<String, dynamic>? queryParameters}) async {
    try {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      final response = await http.patch(uri, headers: headers, body: encodedBody).timeout(timeoutDuration);
      return _handleResponse(response);
    } on SocketException catch (e) {
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      throw ApiException(statusCode: 408, message: 'Connection timed out. Please check backend status.');
    }
  }

  Future<dynamic> delete(String pathOrUrl, {Map<String, dynamic>? queryParameters}) async {
    try {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final response = await http.delete(uri, headers: headers).timeout(timeoutDuration);
      return _handleResponse(response);
    } on SocketException catch (e) {
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      throw ApiException(statusCode: 408, message: 'Connection timed out. Please check backend status.');
    }
  }

  Future<Map<String, dynamic>> healthCheck() async {
    final res = await get(ApiConfig.healthUrl);
    if (res is Map<String, dynamic>) return res;
    return {'status': 'UP'};
  }
}

