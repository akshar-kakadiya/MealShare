import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

void main() {
  group('ApiClient Retry & Transient Error Tests', () {
    late AuthService mockAuthService;

    setUp(() {
      mockAuthService = AuthService();
    });

    test('isTransientError accurately identifies transient vs permanent failures', () {
      // Transient
      expect(ApiClient.isTransientError(const SocketException('Connection refused')), isTrue);
      expect(ApiClient.isTransientError(TimeoutException('Request timed out')), isTrue);
      expect(ApiClient.isTransientError(ApiException(statusCode: 0, message: 'Unreachable')), isTrue);
      expect(ApiClient.isTransientError(ApiException(statusCode: 408, message: 'Timed out')), isTrue);
      expect(ApiClient.isTransientError(ApiException(statusCode: 502, message: 'Bad Gateway')), isTrue);
      expect(ApiClient.isTransientError(ApiException(statusCode: 503, message: 'Service Unavailable')), isTrue);
      expect(ApiClient.isTransientError(ApiException(statusCode: 504, message: 'Gateway Timeout')), isTrue);

      // Permanent - MUST NOT retry
      expect(ApiClient.isTransientError(ApiException(statusCode: 400, message: 'Bad Request')), isFalse);
      expect(ApiClient.isTransientError(ApiException(statusCode: 401, message: 'Unauthorized')), isFalse);
      expect(ApiClient.isTransientError(ApiException(statusCode: 403, message: 'Forbidden')), isFalse);
      expect(ApiClient.isTransientError(ApiException(statusCode: 404, message: 'Not Found')), isFalse);
      expect(ApiClient.isTransientError(ApiException(statusCode: 409, message: 'Conflict')), isFalse);
      expect(ApiClient.isTransientError(FormatException('Malformed JSON')), isFalse);
    });

    test('Permanent HTTP error (404 Not Found) fails immediately without retry', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        return http.Response(jsonEncode({'message': 'Profile not found'}), 404);
      });

      final client = ApiClient(mockAuthService, httpClient: mockClient);

      int retryCallbackCount = 0;
      try {
        await client.get(
          'users/me',
          onRetry: (attempt, max, delay, error) {
            retryCallbackCount++;
          },
        );
        fail('Expected ApiException 404');
      } on ApiException catch (e) {
        expect(e.statusCode, 404);
        expect(e.isNotFound, isTrue);
      }

      // Exactly 1 request made, 0 retries triggered
      expect(requestCount, 1);
      expect(retryCallbackCount, 0);
    });

    test('Permanent HTTP error (401 Unauthorized) fails immediately without retry', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        return http.Response(jsonEncode({'message': 'Token expired'}), 401);
      });

      final client = ApiClient(mockAuthService, httpClient: mockClient);

      int retryCallbackCount = 0;
      try {
        await client.get(
          'donor/profile',
          onRetry: (attempt, max, delay, error) {
            retryCallbackCount++;
          },
        );
        fail('Expected ApiException 401');
      } on ApiException catch (e) {
        expect(e.statusCode, 401);
        expect(e.isUnauthorized, isTrue);
      }

      expect(requestCount, 1);
      expect(retryCallbackCount, 0);
    });

    test('Permanent HTTP error (403 Forbidden) fails immediately without retry', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        return http.Response(jsonEncode({'message': 'Access restricted'}), 403);
      });

      final client = ApiClient(mockAuthService, httpClient: mockClient);

      int retryCallbackCount = 0;
      try {
        await client.get(
          'admin/users',
          onRetry: (attempt, max, delay, error) {
            retryCallbackCount++;
          },
        );
        fail('Expected ApiException 403');
      } on ApiException catch (e) {
        expect(e.statusCode, 403);
        expect(e.isForbidden, isTrue);
      }

      expect(requestCount, 1);
      expect(retryCallbackCount, 0);
    });

    test('Transient error (503 Service Unavailable) retries and succeeds on subsequent attempt', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        if (requestCount < 3) {
          // First 2 attempts return 503 (e.g. backend spin up)
          return http.Response(jsonEncode({'error': 'Backend waking up'}), 503);
        }
        // 3rd attempt succeeds
        return http.Response(jsonEncode({'status': 'UP'}), 200);
      });

      final client = ApiClient(mockAuthService, httpClient: mockClient);

      final retryAttempts = <int>[];
      final res = await ApiClient.requestWithRetry(
        () => client.get('health', retry: false),
        maxRetries: 3,
        initialDelay: const Duration(milliseconds: 10), // fast for unit testing
        backoffFactor: 1.5,
        onRetry: (attempt, max, delay, error) {
          retryAttempts.add(attempt);
        },
      );

      expect(res, isA<Map<String, dynamic>>());
      expect(res['status'], 'UP');
      expect(requestCount, 3);
      expect(retryAttempts, [1, 2]);
    });

    test('Exhausted retries on transient errors throws the final error', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        return http.Response(jsonEncode({'error': 'Server unavailable'}), 502);
      });

      final client = ApiClient(mockAuthService, httpClient: mockClient);

      final retryAttempts = <int>[];
      try {
        await ApiClient.requestWithRetry(
          () => client.get('health', retry: false),
          maxRetries: 3,
          initialDelay: const Duration(milliseconds: 10),
          onRetry: (attempt, max, delay, error) {
            retryAttempts.add(attempt);
          },
        );
        fail('Expected ApiException 502');
      } on ApiException catch (e) {
        expect(e.statusCode, 502);
        expect(e.isTransient, isTrue);
      }

      // Initial attempt + 3 retries = 4 requests
      expect(requestCount, 4);
      expect(retryAttempts, [1, 2, 3]);
    });
  });
}
