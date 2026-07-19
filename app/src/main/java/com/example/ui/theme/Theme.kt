package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class AppThemeColors(
    val cardBack: Color,
    val deepIndigo: Color,
    val lightIndigo: Color,
    val cobaltBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppThemeColors(
        cardBack = Color(0xFF0F172A),
        deepIndigo = Color(0xFF1E293B),
        lightIndigo = Color(0xFF334155),
        cobaltBlue = Color(0xFF2563EB),
        textPrimary = Color.White,
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF64748B),
        border = Color(0xFF334155),
        isDark = true
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6), // Cobalt Blue Accent
    secondary = Color(0xFF6366F1), // Indigo
    tertiary = Color(0xFF10B981),  // Mint Green
    background = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFF1E293B),    // Slate 800
    surfaceVariant = Color(0xFF334155), // Slate 700
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB), // Cobalt Blue Accent
    secondary = Color(0xFF4F46E5), // Indigo
    tertiary = Color(0xFF059669),  // Mint Green
    background = Color(0xFFF8FAFC), // Slate 50
    surface = Color(0xFFFFFFFF),    // White
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val appColors = if (darkTheme) {
        AppThemeColors(
            cardBack = Color(0xFF0F172A),
            deepIndigo = Color(0xFF1E293B),
            lightIndigo = Color(0xFF334155),
            cobaltBlue = Color(0xFF2563EB),
            textPrimary = Color.White,
            textSecondary = Color(0xFF94A3B8),
            textTertiary = Color(0xFF64748B),
            border = Color(0xFF334155),
            isDark = true
        )
    } else {
        AppThemeColors(
            cardBack = Color(0xFFF8FAFC), // Slate 50
            deepIndigo = Color(0xFFFFFFFF), // White
            lightIndigo = Color(0xFFE2E8F0), // Slate 200
            cobaltBlue = Color(0xFF2563EB), // Blue-600
            textPrimary = Color(0xFF0F172A), // Slate 900
            textSecondary = Color(0xFF475569), // Slate 600
            textTertiary = Color(0xFF64748B), // Slate 500
            border = Color(0xFFCBD5E1), // Slate 300
            isDark = false
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
