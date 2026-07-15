package com.health.nutriscan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NutriPrimary,
    onPrimary = Color.White,
    secondary = NutriSecondary,
    onSecondary = Color.White,
    tertiary = NutriPrimaryLight,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9), // Slate 100
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B), // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = OutlineDark,
    error = RiskHigh,
    errorContainer = RiskHighContainerDark,
    onError = Color.White,
    onErrorContainer = Color(0xFFFCA5A5) // Red 300
)

private val LightColorScheme = lightColorScheme(
    primary = NutriPrimary,
    onPrimary = Color.White,
    secondary = NutriSecondary,
    onSecondary = Color.White,
    tertiary = NutriPrimaryDark,
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF64748B), // Slate 500
    outline = OutlineLight,
    error = RiskHigh,
    errorContainer = RiskHighContainerLight,
    onError = Color.White,
    onErrorContainer = Color(0xFF991B1B) // Red 800
)

@Composable
fun NutriScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default to false to ensure our premium brand colors are shown,
    // rather than generic Material You wallpaper-extracted dynamic colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}