import 'package:flutter/material.dart';
import 'routing/router.dart';
import 'theme/app_theme.dart';
import 'config/api_config.dart';

class MealShareApp extends StatelessWidget {
  const MealShareApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'MealShare',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      routerConfig: router,
      builder: (context, child) {
        if (ApiConfig.isMockMode) {
          return Directionality(
            textDirection: TextDirection.ltr,
            child: Column(
              children: [
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                  color: Colors.amber.shade900,
                  child: const SafeArea(
                    bottom: false,
                    child: Text(
                      '⚠️ DEV MOCK MODE — Offline Prototype (No Real Backend)',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        decoration: TextDecoration.none,
                      ),
                    ),
                  ),
                ),
                Expanded(child: child ?? const SizedBox.shrink()),
              ],
            ),
          );
        }
        return child ?? const SizedBox.shrink();
      },
    );
  }
}
