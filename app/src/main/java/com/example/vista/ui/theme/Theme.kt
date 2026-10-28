package com.example.vista.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.vista.domain.model.AppTheme

// Light theme colors
private val LightBackground = Color(0xFFF5F5F5)
private val LightRaisedSurface = Color(0xFFFFFFFF)
private val LightCardSurface = Color(0xFFEEEEEE)
private val LightPrimaryText = Color(0xFF1A1A1A)
private val LightSecondaryText = Color(0xFF4D4D4D)
private val LightMutedText = Color(0xFF808080)

// OLED color scheme (Pure Black)
private val OledColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = PrimaryText,
    secondary = Accent,
    onSecondary = PrimaryText,
    tertiary = Accent,
    onTertiary = PrimaryText,
    background = Color.Black,
    onBackground = PrimaryText,
    surface = Color.Black,
    onSurface = PrimaryText,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = SecondaryText,
    error = ErrorAccent,
    onError = Color(0xFF1A1A1A),
)

// Color schemes
private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = PrimaryText,
    secondary = Accent,
    onSecondary = PrimaryText,
    tertiary = Accent,
    onTertiary = PrimaryText,
    background = Background,
    onBackground = PrimaryText,
    surface = RaisedSurface,
    onSurface = PrimaryText,
    surfaceVariant = CardSurface,
    onSurfaceVariant = SecondaryText,
    error = ErrorAccent,
    onError = Color(0xFF1A1A1A),
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    tertiary = Accent,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightRaisedSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightSecondaryText,
    error = ErrorAccent,
    onError = Color(0xFF1A1A1A),
)

// CompositionLocal for current theme
val LocalAppTheme = staticCompositionLocalOf { AppTheme.DARK }

// Helper functions to get theme-aware colors
object ThemeColors {
    val background: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> Background
            AppTheme.LIGHT -> LightBackground
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) Background else LightBackground
            AppTheme.OLED -> Color.Black
        }

    val raisedSurface: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> RaisedSurface
            AppTheme.LIGHT -> LightRaisedSurface
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) RaisedSurface else LightRaisedSurface
            AppTheme.OLED -> Color.Black
        }

    val cardSurface: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> CardSurface
            AppTheme.LIGHT -> LightCardSurface
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) CardSurface else LightCardSurface
            AppTheme.OLED -> Color(0xFF111111)
        }

    val primaryText: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> PrimaryText
            AppTheme.LIGHT -> LightPrimaryText
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) PrimaryText else LightPrimaryText
            AppTheme.OLED -> PrimaryText
        }

    val secondaryText: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> SecondaryText
            AppTheme.LIGHT -> LightSecondaryText
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) SecondaryText else LightSecondaryText
            AppTheme.OLED -> SecondaryText
        }

    val mutedText: Color
        @Composable
        get() = when (LocalAppTheme.current) {
            AppTheme.DARK -> MutedText
            AppTheme.LIGHT -> LightMutedText
            AppTheme.SYSTEM -> if (isSystemInDarkTheme()) MutedText else LightMutedText
            AppTheme.OLED -> MutedText
        }
}

@Composable
fun VistaTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.OLED -> OledColorScheme
        AppTheme.SYSTEM -> {
            if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        }
    }

    // Update status bar color
    val view = LocalView.current
    val isSystemDark = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val context = view.context
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) break
                currentContext = currentContext.baseContext
            }
            
            val activity = currentContext as? Activity
            val isTv = activity?.packageManager?.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) == true

            if (!isTv) {
                activity?.window?.let { window ->
                    window.statusBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        appTheme == AppTheme.LIGHT || (appTheme == AppTheme.SYSTEM && !isSystemDark)
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
