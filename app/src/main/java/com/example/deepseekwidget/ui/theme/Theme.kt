package com.example.deepseekwidget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentDeepSeek,
    onPrimary = ObsidianPrimaryInk,
    secondary = ObsidianSecondaryInk,
    background = ObsidianSurfaceBg,
    surface = ObsidianCardBg,
    onBackground = ObsidianPrimaryInk,
    onSurface = ObsidianPrimaryInk,
    error = StatusWarningDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentDeepSeekLight,
    onPrimary = AlabasterSurfaceBg,
    secondary = AlabasterSecondaryInk,
    background = AlabasterSurfaceBg,
    surface = AlabasterCardBg,
    onBackground = AlabasterPrimaryInk,
    onSurface = AlabasterPrimaryInk,
    error = StatusWarningLight
)

@Composable
fun DeepSeekWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
