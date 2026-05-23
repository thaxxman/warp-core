package com.redshirt.warpcore.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun WarpCoreTheme(
    content: @Composable () -> Unit
) {
    // Always use dark theme — LCARS-style
    val colorScheme = darkColorScheme(
        primary = TrekBlue,
        onPrimary = DarkOnPrimary,
        secondary = TrekOrange,
        onSecondary = DarkOnPrimary,
        tertiary = TrekPurple,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        error = TrekRed,
        onError = DarkOnPrimary
    )

    // Status bar styling
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WarpCoreTypography,
        content = content
    )
}