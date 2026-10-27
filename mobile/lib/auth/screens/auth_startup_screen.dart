import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../config/api_config.dart';
import '../../mock/app_state.dart';
import '../../shared/api_client.dart';
import '../../theme/app_colors.dart';
import '../auth_service.dart';

class AuthStartupScreen extends StatefulWidget {
  const AuthStartupScreen({super.key});

  @override
  State<AuthStartupScreen> createState() => _AuthStartupScreenState();
}

class _AuthStartupScreenState extends State<AuthStartupScreen> {
  bool _isLoading = true;
  String? _errorMessage;
  bool _canRetry = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _evaluateSession();
    });
  }

  Future<void> _evaluateSession() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _canRetry = false;
    });

    final authService = context.read<AuthService>();
    final client = context.read<ApiClient>();
    final appState = context.read<AppState>();

    // 1. In mock mode, if not authenticated, go to login; if authenticated, navigate by role
    if (ApiConfig.isMockMode) {
      if (!authService.isAuthenticated) {
        if (!mounted) return;
        context.go('/login');
        return;
      }
      _navigateForRole(authService.role);
      return;
    }

    // 2. Check for active Firebase session
    if (!authService.isAuthenticated) {
      debugPrint('[STARTUP] No active session found. Navigating to /login');
      if (!mounted) return;
      context.go('/login');
      return;
    }

    // 3. User is authenticated with Firebase: fetch backend profile
    debugPrint('[STARTUP] Active session found for ${authService.email} (${authService.uid}). Fetching backend profile...');
    try {
      final me = await client.get(
        ApiConfig.usersMeUrl,
        retry: true,
        timeout: const Duration(seconds: 15),
      );

      if (!mounted) return;

      if (me is Map<String, dynamic>) {
        authService.updateFromBackendProfile(me);
        final role = authService.role;
        debugPrint('[STARTUP] Profile loaded successfully. Role: $role');

        if (role == UserRole.admin) {
          debugPrint('[STARTUP] Admin account detected on mobile. Signing out.');
          await authService.signOut();
          if (!mounted) return;
          setState(() {
            _isLoading = false;
            _errorMessage = 'Administrator access is exclusively supported on the MealShare React Web portal. Please log in using the web console.';
            _canRetry = false;
          });
          return;
        }

        // Fetch initial role data
        if (role == UserRole.donor) {
          await appState.fetchDonorProfileRemote();
        } else if (role == UserRole.receiver) {
          await appState.fetchReceiverProfileRemote();
        }

        if (!mounted) return;
        _navigateForRole(role);
      } else {
        throw ApiException(
          statusCode: 500,
          message: 'Unexpected response format from backend profile endpoint',
        );
      }
    } on ApiException catch (e) {
      debugPrint('[STARTUP] ApiException caught: statusCode=${e.statusCode}, message=${e.message}');
      if (!mounted) return;

      if (e.statusCode == 404) {
        // Explicitly handle missing backend profile: sign out stale Firebase user and direct to login
        debugPrint('[STARTUP] 404 Not Found: Profile missing from database. Signing out and routing to /login.');
        await authService.signOut();
        if (!mounted) return;
        context.go('/login');
        return;
      }

      if (e.statusCode == 401) {
        // Attempt force token refresh
        debugPrint('[STARTUP] 401 Unauthorized: Attempting token refresh...');
        try {
          final refreshed = await authService.getIdToken(forceRefresh: true);
          if (refreshed != null) {
            final retryMe = await client.get(
              ApiConfig.usersMeUrl,
              retry: false,
              timeout: const Duration(seconds: 10),
            );
            if (retryMe is Map<String, dynamic>) {
              authService.updateFromBackendProfile(retryMe);
              if (!mounted) return;
              _navigateForRole(authService.role);
              return;
            }
          }
        } catch (refreshErr) {
          debugPrint('[STARTUP] Token refresh failed: $refreshErr');
        }

        // Token permanently rejected - sign out and redirect to login
        await authService.signOut();
        if (!mounted) return;
        context.go('/login');
        return;
      }

      if (e.statusCode == 403) {
        // User suspended
        await authService.signOut();
        if (!mounted) return;
        setState(() {
          _isLoading = false;
          _errorMessage = 'Access denied: Your MealShare account has been suspended by an administrator.';
          _canRetry = false;
        });
        return;
      }

      // Transient backend failure (timeout, 500, 502, 503, etc.)
      // DO NOT silently sign out, DO NOT hang in an infinite spinner!
      final detail = e.statusCode > 0 ? '(${e.statusCode}: ${e.message})' : '(${e.message})';
      setState(() {
        _isLoading = false;
        _errorMessage = 'The MealShare backend server is waking up or temporarily unavailable $detail.\n\nYour login session is saved. You can retry connecting now.';
        _canRetry = true;
      });
    } catch (e) {
      debugPrint('[STARTUP] Unexpected startup error: $e');
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _errorMessage = 'Failed to connect to MealShare servers ($e).\n\nYour login session is saved. Please check your connection and retry.';
        _canRetry = true;
      });
    }
  }

  void _navigateForRole(UserRole? role) {
    switch (role) {
      case UserRole.donor:
        context.go('/donor/home');
        break;
      case UserRole.receiver:
        context.go('/receiver/home');
        break;
      default:
        context.go('/donor/home');
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 32.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // App Logo / Icon
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.volunteer_activism,
                    size: 42,
                    color: AppColors.primary,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'MealShare',
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.textPrimary,
                      ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Bridging Surplus Food to Communities',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppColors.textSecondary,
                      ),
                ),
                const SizedBox(height: 48),

                if (_isLoading) ...[
                  const CircularProgressIndicator(),
                  const SizedBox(height: 20),
                  Text(
                    'Connecting to MealShare...',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                ],

                if (_errorMessage != null) ...[
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.amber.shade50,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.amber.shade400),
                    ),
                    child: Column(
                      children: [
                        Icon(
                          Icons.cloud_off_outlined,
                          color: Colors.amber.shade800,
                          size: 32,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          _errorMessage!,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.amber.shade900,
                            height: 1.4,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  if (_canRetry) ...[
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: _evaluateSession,
                        icon: const Icon(Icons.refresh),
                        label: const Text('Retry Connection'),
                      ),
                    ),
                    const SizedBox(height: 12),
                  ],
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        final authService = context.read<AuthService>();
                        await authService.signOut();
                        if (context.mounted) {
                          context.go('/login');
                        }
                      },
                      icon: const Icon(Icons.logout),
                      label: const Text('Log Out'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
