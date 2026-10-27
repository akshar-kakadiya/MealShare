import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
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
  bool get isTransient =>
      statusCode == 0 ||
      statusCode == 408 ||
      statusCode == 502 ||
      statusCode == 503 ||
      statusCode == 504;

  @override
  String toString() => 'ApiException ($statusCode): $message';
}

typedef RetryCallback = void Function(
  int attempt,
  int maxRetries,
  Duration delay,
  dynamic error,
);

class ApiClient {
  final AuthService _authService;
  final http.Client _client;
  static const Duration timeoutDuration = Duration(seconds: 30);

  ApiClient(this._authService, {http.Client? httpClient})
      : _client = httpClient ?? http.Client();

  /// Determine if an error is transient (e.g., server spin-up / waking up, network timeout).
  static bool isTransientError(dynamic error) {
    if (error is SocketException) return true;
    if (error is TimeoutException) return true;
    if (error is http.ClientException) return true;
    if (error is ApiException) {
      return error.isTransient;
    }
    final msg = error.toString().toLowerCase();
    return msg.contains('connection timed out') ||
        msg.contains('connection refused') ||
        msg.contains('connection reset') ||
        msg.contains('network is unreachable') ||
        msg.contains('socketexception') ||
        msg.contains('clientexception') ||
        msg.contains('failed host lookup') ||
        msg.contains('handshakeexception');
  }

  /// Bounded retry loop with exponential backoff for transient failures.
  static Future<T> requestWithRetry<T>(
    Future<T> Function() requestFn, {
    int maxRetries = 3,
    Duration initialDelay = const Duration(milliseconds: 1500),
    double backoffFactor = 2.0,
    RetryCallback? onRetry,
  }) async {
    int attempt = 0;
    Duration currentDelay = initialDelay;

    while (true) {
      try {
        return await requestFn();
      } catch (e) {
        if (!isTransientError(e) || attempt >= maxRetries) {
          debugPrint('[AUTH_FLOW] Non-transient error or retries exhausted (attempt $attempt of $maxRetries): $e');
          rethrow;
        }
        attempt++;
        debugPrint(
          '[AUTH_FLOW] Transient failure detected ($e). Scheduling retry attempt $attempt of $maxRetries in ${currentDelay.inMilliseconds}ms...',
        );
        if (onRetry != null) {
          onRetry(attempt, maxRetries, currentDelay, e);
        }
        await Future.delayed(currentDelay);
        currentDelay = Duration(
          milliseconds: (currentDelay.inMilliseconds * backoffFactor).round(),
        );
      }
    }
  }

  Future<Map<String, String>> _getHeaders() async {
    final token = await _authService.getIdToken().timeout(
      const Duration(seconds: 10),
      onTimeout: () {
        debugPrint('[AUTH_FLOW] _getHeaders timed out waiting for ID token after 10s');
        throw TimeoutException('Timed out waiting for authentication token.');
      },
    );
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

  Future<dynamic> _executeRequest(
    Future<http.Response> Function() call, {
    Duration? timeout,
    String? requestDescription,
  }) async {
    final effectiveTimeout = timeout ?? timeoutDuration;
    final desc = requestDescription ?? 'HTTP request';
    final stopwatch = Stopwatch()..start();
    try {
      debugPrint('[AUTH_FLOW] $desc started (timeout: ${effectiveTimeout.inSeconds}s)...');
      final response = await call().timeout(effectiveTimeout);
      stopwatch.stop();
      debugPrint('[AUTH_FLOW] $desc completed in ${stopwatch.elapsedMilliseconds}ms with status ${response.statusCode}');
      return _handleResponse(response);
    } on SocketException catch (e) {
      stopwatch.stop();
      debugPrint('[AUTH_FLOW] $desc failed after ${stopwatch.elapsedMilliseconds}ms with SocketException: ${e.message}');
      throw ApiException(statusCode: 0, message: 'Cannot reach MealShare backend: ${e.message}');
    } on TimeoutException {
      stopwatch.stop();
      debugPrint('[AUTH_FLOW] $desc timed out after ${stopwatch.elapsedMilliseconds}ms (exceeded ${effectiveTimeout.inSeconds}s)');
      throw ApiException(statusCode: 408, message: 'Connection timed out after ${effectiveTimeout.inSeconds}s. Please check backend status.');
    } on http.ClientException catch (e) {
      stopwatch.stop();
      debugPrint('[AUTH_FLOW] $desc failed after ${stopwatch.elapsedMilliseconds}ms with ClientException: ${e.message}');
      throw ApiException(statusCode: 0, message: 'Network error communicating with backend: ${e.message}');
    } catch (e) {
      stopwatch.stop();
      debugPrint('[AUTH_FLOW] $desc failed after ${stopwatch.elapsedMilliseconds}ms with error: $e');
      rethrow;
    }
  }

  Future<dynamic> get(
    String pathOrUrl, {
    Map<String, dynamic>? queryParameters,
    bool retry = true,
    int maxRetries = 3,
    Duration? timeout,
    RetryCallback? onRetry,
  }) async {
    Future<dynamic> execute() async {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      return _executeRequest(
        () => _client.get(uri, headers: headers),
        timeout: timeout,
        requestDescription: 'GET $uri',
      );
    }

    if (!retry) return execute();
    return requestWithRetry(execute, maxRetries: maxRetries, onRetry: onRetry);
  }

  Future<dynamic> post(
    String pathOrUrl, {
    dynamic body,
    Map<String, dynamic>? queryParameters,
    bool retry = false,
    int maxRetries = 3,
    RetryCallback? onRetry,
  }) async {
    Future<dynamic> execute() async {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      return _executeRequest(() => _client.post(uri, headers: headers, body: encodedBody));
    }

    if (!retry) return execute();
    return requestWithRetry(execute, maxRetries: maxRetries, onRetry: onRetry);
  }

  Future<dynamic> put(
    String pathOrUrl, {
    dynamic body,
    Map<String, dynamic>? queryParameters,
    bool retry = true,
    int maxRetries = 3,
    RetryCallback? onRetry,
  }) async {
    Future<dynamic> execute() async {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      return _executeRequest(() => _client.put(uri, headers: headers, body: encodedBody));
    }

    if (!retry) return execute();
    return requestWithRetry(execute, maxRetries: maxRetries, onRetry: onRetry);
  }

  Future<dynamic> patch(
    String pathOrUrl, {
    dynamic body,
    Map<String, dynamic>? queryParameters,
    bool retry = false,
    int maxRetries = 3,
    RetryCallback? onRetry,
  }) async {
    Future<dynamic> execute() async {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      final encodedBody = body != null ? jsonEncode(body) : null;
      return _executeRequest(() => _client.patch(uri, headers: headers, body: encodedBody));
    }

    if (!retry) return execute();
    return requestWithRetry(execute, maxRetries: maxRetries, onRetry: onRetry);
  }

  Future<dynamic> delete(
    String pathOrUrl, {
    Map<String, dynamic>? queryParameters,
    bool retry = false,
    int maxRetries = 3,
    RetryCallback? onRetry,
  }) async {
    Future<dynamic> execute() async {
      final headers = await _getHeaders();
      final uri = _buildUri(pathOrUrl, queryParameters);
      return _executeRequest(() => _client.delete(uri, headers: headers));
    }

    if (!retry) return execute();
    return requestWithRetry(execute, maxRetries: maxRetries, onRetry: onRetry);
  }

  Future<Map<String, dynamic>> healthCheck({
    int maxRetries = 3,
    RetryCallback? onRetry,
  }) async {
    final res = await get(
      ApiConfig.healthUrl,
      retry: true,
      maxRetries: maxRetries,
      onRetry: onRetry,
    );
    if (res is Map<String, dynamic>) return res;
    return {'status': 'UP'};
  }
}

