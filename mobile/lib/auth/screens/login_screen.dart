import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _loading = false;
  bool _obscurePassword = true;
  String? _statusMessage;
  String? _error;
  bool _needsRegistration = false;
  bool _canRetry = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        final authService = context.read<AuthService>();
        if (authService.isAuthenticated && authService.role == null) {
          if (_emailController.text.isEmpty && authService.email != null) {
            _emailController.text = authService.email!;
          }
        }
      }
    });
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _fetchProfileAndNavigate() async {
    debugPrint('[AUTH_FLOW] _fetchProfileAndNavigate started...');
    setState(() {
      _loading = true;
      _error = null;
      _needsRegistration = false;
      _canRetry = false;
      _statusMessage = 'Connecting to MealShare backend...';
    });

    final authService = context.read<AuthService>();
    final client = context.read<ApiClient>();

    try {
      debugPrint('[AUTH_FLOW] Backend profile lookup GET ${ApiConfig.usersMeUrl} started...');
      final stopwatch = Stopwatch()..start();

      // Bounded retry with explicit timeout so interactive login never hangs
      dynamic me = await client.get(
        ApiConfig.usersMeUrl,
        maxRetries: 1,
        timeout: const Duration(seconds: 15),
        onRetry: (attempt, maxRetries, delay, error) {
          debugPrint(
            '[AUTH_FLOW] Backend profile lookup retry attempt $attempt of $maxRetries after ${delay.inMilliseconds}ms: $error',
          );
          if (mounted) {
            setState(() {
              _statusMessage = 'Waking up backend server (attempt $attempt/$maxRetries)...';
            });
          }
        },
      ).timeout(
        const Duration(seconds: 35),
        onTimeout: () {
          debugPrint('[AUTH_FLOW] Overall _fetchProfileAndNavigate timed out after 35 seconds');
          throw TimeoutException('Backend connection timed out after 35 seconds. Please tap Retry.');
        },
      );

      stopwatch.stop();
      debugPrint('[AUTH_FLOW] Backend profile lookup completed in ${stopwatch.elapsedMilliseconds}ms');

      if (me is Map<String, dynamic>) {
        authService.updateFromBackendProfile(me);
      }

      if (!mounted) {
        debugPrint('[AUTH_FLOW] Widget not mounted after profile fetch.');
        return;
      }

      debugPrint('[AUTH_FLOW] Resolved user role: ${authService.role}');

      if (authService.role == UserRole.admin) {
        debugPrint('[AUTH_FLOW] Admin user detected on mobile. Signing out and displaying admin modal notice.');
        await authService.signOut();
        if (!mounted) return;
        showDialog(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Admin Portal Notice'),
            content: const Text(
              'Administrator access is exclusively supported on the MealShare React Web portal.\n\nPlease log in using the web console.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Understood'),
              ),
            ],
          ),
        );
        return;
      }

      switch (authService.role) {
        case UserRole.donor:
          debugPrint('[AUTH_FLOW] Navigating to /donor/home');
          context.go('/donor/home');
          break;
        case UserRole.receiver:
          debugPrint('[AUTH_FLOW] Navigating to /receiver/home');
          context.go('/receiver/home');
          break;
        default:
          debugPrint('[AUTH_FLOW] Unknown role fallback, navigating to /donor/home');
          context.go('/donor/home');
      }
    } on ApiException catch (e) {
      debugPrint('[AUTH_FLOW] ApiException caught: statusCode=${e.statusCode}, message=${e.message}');
      if (e.statusCode == 401) {
        // Attempt force token refresh once before treating as permanent failure
        debugPrint('[AUTH_FLOW] 401 Unauthorized: Attempting force token refresh...');
        try {
          final refreshedToken = await authService.getIdToken(forceRefresh: true);
          if (refreshedToken != null) {
            final retryMe = await client.get(
              ApiConfig.usersMeUrl,
              retry: false,
              timeout: const Duration(seconds: 10),
            );
            if (retryMe is Map<String, dynamic>) {
              authService.updateFromBackendProfile(retryMe);
              if (!mounted) return;
              if (authService.role == UserRole.admin) {
                await authService.signOut();
                if (!mounted) return;
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('Admin Portal Notice'),
                    content: const Text(
                      'Administrator access is exclusively supported on the MealShare React Web portal.\n\nPlease log in using the web console.',
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(ctx),
                        child: const Text('Understood'),
                      ),
                    ],
                  ),
                );
                return;
              }
              switch (authService.role) {
                case UserRole.donor:
                  debugPrint('[AUTH_FLOW] Navigating to /donor/home after token refresh');
                  context.go('/donor/home');
                  break;
                case UserRole.receiver:
                  debugPrint('[AUTH_FLOW] Navigating to /receiver/home after token refresh');
                  context.go('/receiver/home');
                  break;
                default:
                  debugPrint('[AUTH_FLOW] Unknown role fallback after token refresh');
                  context.go('/donor/home');
              }
              return;
            }
          }
        } catch (refreshErr) {
          debugPrint('[AUTH_FLOW] Token refresh failed: $refreshErr');
        }

        // Token permanently rejected or revoked - sign out for security
        debugPrint('[AUTH_FLOW] Signing out due to permanent 401 token rejection');
        await authService.signOut();
        if (mounted) {
          setState(() {
            _error = 'Authentication failed or session expired. Please sign in again.';
          });
        }
      } else if (e.statusCode == 403) {
        // Account suspended - sign out for security
        debugPrint('[AUTH_FLOW] 403 Forbidden: Account suspended. Signing out.');
        await authService.signOut();
        if (mounted) {
          setState(() {
            _error = 'Access denied: Your MealShare account has been suspended by an administrator.';
          });
        }
      } else if (e.statusCode == 404) {
        // RECOVERABLE: Account authenticated in Firebase, but profile not in DB yet
        debugPrint('[AUTH_FLOW] 404 Not Found: Profile missing from database. Preserving Firebase session for registration recovery.');
        if (mounted) {
          setState(() {
            _needsRegistration = true;
            _error = 'Your account is authenticated, but no MealShare profile exists yet in the database. Please complete registration below to set up your profile.';
          });
        }
      } else {
        // RECOVERABLE: Transient or backend error (408, 500, 502, 503, 504, etc.)
        debugPrint('[AUTH_FLOW] Recoverable backend error ${e.statusCode}: ${e.message}. Preserving session and enabling retry.');
        final detail = e.statusCode > 0 ? '${e.statusCode}: ${e.message}' : e.message;
        if (mounted) {
          setState(() {
            _canRetry = true;
            _error = 'MealShare backend is temporarily unavailable ($detail). Your login session is saved.';
          });
        }
      }
    } catch (e, stackTrace) {
      debugPrint('[AUTH_FLOW] Unexpected error caught in _fetchProfileAndNavigate: $e\n$stackTrace');
      if (mounted) {
        setState(() {
          _canRetry = true;
          _error = 'Backend connection issue ($e). Your login session is saved. Please check your connection and tap Retry.';
        });
      }
    } finally {
      debugPrint('[AUTH_FLOW] Cleanup finally block in _fetchProfileAndNavigate. mounted=$mounted');
      if (mounted) {
        setState(() {
          _loading = false;
          _statusMessage = null;
        });
      }
    }
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    final email = _emailController.text.trim();
    debugPrint('[AUTH_FLOW] Login button pressed for email: $email');

    setState(() {
      _loading = true;
      _error = null;
      _needsRegistration = false;
      _canRetry = false;
      _statusMessage = 'Signing in with Firebase...';
    });

    try {
      final authService = context.read<AuthService>();
      await authService.signIn(
        email,
        _passwordController.text,
      );

      // Fetch profile from Spring Boot backend and navigate
      if (mounted) {
        await _fetchProfileAndNavigate();
      }
    } catch (e, stackTrace) {
      debugPrint('[AUTH_FLOW] Error caught in _handleLogin: $e\n$stackTrace');
      if (mounted) {
        setState(() {
          _error = e.toString().replaceAll('Exception: ', '');
        });
      }
    } finally {
      debugPrint('[AUTH_FLOW] Cleanup finally block in _handleLogin. mounted=$mounted');
      if (mounted) {
        setState(() {
          _loading = false;
          _statusMessage = null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Logo
                  const Text(
                    '🍽️',
                    style: TextStyle(fontSize: 64),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'MealShare',
                    style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: Colors.green.shade700,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Surplus Food Redistribution',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.grey,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 40),

                  // Error message with actionable options
                  if (_error != null)
                    Container(
                      padding: const EdgeInsets.all(12),
                      margin: const EdgeInsets.only(bottom: 16),
                      decoration: BoxDecoration(
                        color: Colors.red.shade50,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.red.shade200),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(Icons.info_outline, color: Colors.red.shade700, size: 20),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  _error!,
                                  style: TextStyle(color: Colors.red.shade800, fontSize: 13, height: 1.3),
                                ),
                              ),
                            ],
                          ),
                          if (_needsRegistration) ...[
                            const SizedBox(height: 12),
                            FilledButton.icon(
                              onPressed: () => context.push('/register'),
                              icon: const Icon(Icons.person_add_outlined, size: 16),
                              label: const Text('Complete Registration Now'),
                              style: FilledButton.styleFrom(
                                backgroundColor: Colors.green.shade700,
                                visualDensity: VisualDensity.compact,
                              ),
                            ),
                            const SizedBox(height: 4),
                            TextButton(
                              onPressed: () async {
                                final authService = context.read<AuthService>();
                                await authService.signOut();
                                setState(() {
                                  _needsRegistration = false;
                                  _error = null;
                                });
                              },
                              child: const Text('Sign out and use another account', style: TextStyle(fontSize: 12)),
                            ),
                          ],
                          if (_canRetry) ...[
                            const SizedBox(height: 12),
                            FilledButton.icon(
                              onPressed: _loading ? null : _fetchProfileAndNavigate,
                              icon: const Icon(Icons.refresh, size: 16),
                              label: const Text('Retry Connection'),
                              style: FilledButton.styleFrom(
                                visualDensity: VisualDensity.compact,
                              ),
                            ),
                            const SizedBox(height: 4),
                            TextButton(
                              onPressed: () async {
                                final authService = context.read<AuthService>();
                                await authService.signOut();
                                setState(() {
                                  _canRetry = false;
                                  _error = null;
                                });
                              },
                              child: const Text('Sign out', style: TextStyle(fontSize: 12)),
                            ),
                          ],
                        ],
                      ),
                    ),

                  // Email field
                  TextFormField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(
                      labelText: 'Email',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) return 'Email is required';
                      if (!value.contains('@')) return 'Enter a valid email';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

                  // Password field
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      labelText: 'Password',
                      border: const OutlineInputBorder(),
                      prefixIcon: const Icon(Icons.lock_outlined),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                        ),
                        tooltip: _obscurePassword ? 'Show password' : 'Hide password',
                        onPressed: () {
                          setState(() {
                            _obscurePassword = !_obscurePassword;
                          });
                        },
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) return 'Password is required';
                      if (value.length < 6) return 'Password must be at least 6 characters';
                      return null;
                    },
                  ),
                  const SizedBox(height: 24),

                  // Login button
                  FilledButton(
                    onPressed: _loading ? null : _handleLogin,
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _loading
                        ? Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              ),
                              if (_statusMessage != null) ...[
                                const SizedBox(width: 12),
                                Flexible(
                                  child: Text(
                                    _statusMessage!,
                                    style: const TextStyle(fontSize: 14, color: Colors.white),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ],
                          )
                        : const Text('Sign In', style: TextStyle(fontSize: 16)),
                  ),
                  const SizedBox(height: 16),

                  // Register link
                  TextButton(
                    onPressed: () => context.push('/register'),
                    child: const Text("Don't have an account? Create Account"),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
