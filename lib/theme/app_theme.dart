import 'package:flutter/material.dart';

class AppColors {
  static const background = Color(0xFF0A1628);
  static const panel = Color(0xFF0F1E35);
  static const card = Color(0xFF152640);
  static const cardStroke = Color(0xFF1E3455);
  static const overlay = Color(0xCC0A1628);

  static const blue = Color(0xFF2196F3);
  static const cyan = Color(0xFF00BCD4);
  static const lightBlue = Color(0xFF4FC3F7);
  static const red = Color(0xFFEF5350);

  static const textPrimary = Color(0xFFFFFFFF);
  static const textSecondary = Color(0xFFB0C4DE);
}

class AppTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.background,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.blue,
        secondary: AppColors.cyan,
        surface: AppColors.panel,
        error: AppColors.red,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.panel,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.blue,
          foregroundColor: AppColors.textPrimary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          textStyle: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      textTheme: const TextTheme(
        headlineLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 34,
          fontWeight: FontWeight.bold,
        ),
        titleLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 24,
          fontWeight: FontWeight.bold,
        ),
        bodyLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 18,
        ),
        bodyMedium: TextStyle(
          color: AppColors.textSecondary,
          fontSize: 16,
        ),
      ),
    );
  }
}
