import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:firebase_auth/firebase_auth.dart' as fb;
import 'package:mealshare_mobile/config/api_config.dart';

enum UserRole {
  donor,
  receiver,
  admin,
}

extension UserRoleExtension on UserRole {
  String get backendRole {
    switch (this) {
      case UserRole.donor:
        return 'DONOR';
      case UserRole.receiver:
        return 'RECEIVER';
      case UserRole.admin:
        return 'ADMIN';
    }
  }
}

enum AuthState {
  initial,
  authenticated,
  unauthenticated,
}

class AuthService extends ChangeNotifier {
  AuthState _authState = AuthState.initial;
  String? _uid;
  String? _email;
  String? _displayName;
  UserRole? _role;
  String? _devToken;

  AuthState get authState => _authState;
  String? get uid => _uid;
  String? get email => _email;
  String? get displayName => _displayName;
  UserRole? get role => _role;
  bool get isAuthenticated => _authState == AuthState.authenticated;

  bool _firebaseInitialized = false;

  AuthService() {
    _checkFirebase();
  }

  void _checkFirebase() {
    try {
      final auth = fb.FirebaseAuth.instance;
      _firebaseInitialized = true;
      final current = auth.currentUser;
      if (current != null) {
        _uid = current.uid;
        _email = current.email;
        _displayName = current.displayName;
        _authState = AuthState.authenticated;
      } else {
        _authState = AuthState.unauthenticated;
      }
      auth.authStateChanges().listen(_onFirebaseAuthStateChanged);
    } catch (e) {
      debugPrint('Firebase Auth initialization check error: $e');
      _firebaseInitialized = false;
      _authState = AuthState.unauthenticated;
      notifyListeners();
    }
  }

  void _onFirebaseAuthStateChanged(fb.User? user) {
    if (user != null) {
      _uid = user.uid;
      _email = user.email;
      _displayName = user.displayName;
      _authState = AuthState.authenticated;
    } else if (_devToken == null) {
      _uid = null;
      _email = null;
      _displayName = null;
      _role = null;
      _authState = AuthState.unauthenticated;
    }
    notifyListeners();
  }

  /// Sign in with email and password
  Future<void> signIn(String email, String password) async {
    try {
      if (_firebaseInitialized) {
        debugPrint('[AUTH_FLOW] Firebase sign-in started for $email');
        final stopwatch = Stopwatch()..start();
        final credential = await fb.FirebaseAuth.instance
            .signInWithEmailAndPassword(
              email: email.trim(),
              password: password,
            )
            .timeout(
              const Duration(seconds: 15),
              onTimeout: () {
                stopwatch.stop();
                debugPrint('[AUTH_FLOW] Firebase sign-in timed out after 15 seconds for $email');
                throw TimeoutException(
                  'Firebase sign-in timed out after 15 seconds. Please check your internet connection.',
                );
              },
            );
        stopwatch.stop();
        _uid = credential.user?.uid;
        _email = credential.user?.email;
        _displayName = credential.user?.displayName;
        _authState = AuthState.authenticated;
        _devToken = null;
        debugPrint(
          '[AUTH_FLOW] Firebase sign-in completed in ${stopwatch.elapsedMilliseconds}ms. UID: $_uid',
        );
      } else if (ApiConfig.isMockMode) {
        // Fallback simulated session strictly for offline prototype mode
        _uid = 'mock-user-${email.split('@').first}';
        _email = email;
        _displayName = email.split('@').first;
        _devToken = 'mock-dev-token-$_uid';
        _authState = AuthState.authenticated;

        // Auto-detect role from email hint if not set
        if (email.contains('donor')) {
          _role = UserRole.donor;
        } else if (email.contains('admin')) {
          _role = UserRole.admin;
        } else {
          _role = _role ?? UserRole.receiver;
        }
      } else {
        throw StateError('Firebase Authentication failed to initialize. Real authentication required.');
      }
      notifyListeners();
    } catch (e) {
      debugPrint('[AUTH_FLOW] Firebase sign-in failed: $e');
      _authState = AuthState.unauthenticated;
      notifyListeners();
      rethrow;
    }
  }

  /// Register a new user
  Future<void> register(String email, String password, String name, UserRole role) async {
    try {
      if (_firebaseInitialized) {
        debugPrint('[AUTH_FLOW] Firebase user registration started for $email');
        final stopwatch = Stopwatch()..start();
        fb.User? user = fb.FirebaseAuth.instance.currentUser;
        if (user == null || user.email?.toLowerCase() != email.trim().toLowerCase()) {
          try {
            final credential = await fb.FirebaseAuth.instance
                .createUserWithEmailAndPassword(
                  email: email.trim(),
                  password: password,
                )
                .timeout(
                  const Duration(seconds: 15),
                  onTimeout: () => throw TimeoutException('Firebase registration timed out after 15s.'),
                );
            user = credential.user;
          } on fb.FirebaseAuthException catch (e) {
            if (e.code == 'email-already-in-use') {
              // Account already exists in Firebase Auth: sign in to acquire valid session
              final credential = await fb.FirebaseAuth.instance
                  .signInWithEmailAndPassword(
                    email: email.trim(),
                    password: password,
                  )
                  .timeout(
                    const Duration(seconds: 15),
                    onTimeout: () => throw TimeoutException('Firebase sign-in timed out after 15s.'),
                  );
              user = credential.user;
            } else {
              rethrow;
            }
          }
        }
        await user?.updateDisplayName(name);
        stopwatch.stop();
        _uid = user?.uid;
        _email = user?.email;
        _displayName = name;
        _role = role;
        _authState = AuthState.authenticated;
        _devToken = null;
        debugPrint('[AUTH_FLOW] Firebase user registered/linked in ${stopwatch.elapsedMilliseconds}ms. UID: $_uid');
      } else if (ApiConfig.isMockMode) {
        _uid = 'mock-user-${email.split('@').first}';
        _email = email;
        _displayName = name;
        _role = role;
        _devToken = 'mock-dev-token-$_uid';
        _authState = AuthState.authenticated;
      } else {
        throw StateError('Firebase Authentication failed to initialize. Real authentication required.');
      }
      notifyListeners();
    } catch (e) {
      debugPrint('[AUTH_FLOW] Firebase registration failed: $e');
      _authState = AuthState.unauthenticated;
      notifyListeners();
      rethrow;
    }
  }

  /// Sign out
  Future<void> signOut() async {
    if (_firebaseInitialized) {
      try {
        await fb.FirebaseAuth.instance.signOut();
      } catch (_) {}
    }
    _uid = null;
    _email = null;
    _displayName = null;
    _role = null;
    _devToken = null;
    _authState = AuthState.unauthenticated;
    notifyListeners();
  }

  /// Get Firebase ID token for API calls
  Future<String?> getIdToken({bool forceRefresh = false}) async {
    if (_firebaseInitialized) {
      debugPrint('[AUTH_FLOW] Firebase ID token retrieval started (forceRefresh: $forceRefresh)...');
      final stopwatch = Stopwatch()..start();
      try {
        final token = await fb.FirebaseAuth.instance.currentUser
            ?.getIdToken(forceRefresh)
            .timeout(
              const Duration(seconds: 10),
              onTimeout: () {
                debugPrint('[AUTH_FLOW] Firebase ID token retrieval timed out after 10 seconds.');
                throw TimeoutException('Firebase ID token retrieval timed out after 10 seconds.');
              },
            );
        stopwatch.stop();
        debugPrint(
          '[AUTH_FLOW] Firebase ID token retrieval completed in ${stopwatch.elapsedMilliseconds}ms (token acquired: ${token != null && token.isNotEmpty})',
        );
        return token;
      } catch (e) {
        stopwatch.stop();
        debugPrint('[AUTH_FLOW] Firebase ID token retrieval failed after ${stopwatch.elapsedMilliseconds}ms: $e');
        rethrow;
      }
    }
    if (ApiConfig.isMockMode) {
      return _devToken;
    }
    return null;
  }

  void setRole(UserRole role) {
    _role = role;
    notifyListeners();
  }

  /// Update user profile from backend GET /api/v1/users/me response
  void updateFromBackendProfile(Map<String, dynamic> profile) {
    if (profile['name'] != null) _displayName = profile['name'].toString();
    if (profile['email'] != null) _email = profile['email'].toString();
    final roleStr = profile['role']?.toString().toUpperCase();
    if (roleStr != null) {
      switch (roleStr) {
        case 'DONOR':
          _role = UserRole.donor;
          break;
        case 'RECEIVER':
          _role = UserRole.receiver;
          break;
        case 'ADMIN':
          _role = UserRole.admin;
          break;
      }
    }
    notifyListeners();
  }
}

