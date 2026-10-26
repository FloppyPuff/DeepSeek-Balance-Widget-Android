package com.example.deepseekwidget.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.ThemeMode

val DisplayFontFamily = FontFamily(
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.space_grotesk_bold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resId = R.font.space_grotesk_bold,
        weight = FontWeight.Bold,
    ),
)

val DeepSeekTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
        displayMedium = base.displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    )
}

val MicroLabelStyle = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 1.5.sp,
)

val NumberStyle = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

@Composable
fun DeepSeekTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = resolveDarkTheme(themeMode)
    val palette = paletteFor(dark)
    val colorScheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.canvas,
            onBackground = palette.primaryInk,
            surface = palette.surface,
            onSurface = palette.primaryInk,
            surfaceVariant = palette.surfaceHighlight,
            onSurfaceVariant = palette.secondaryInk,
            outline = palette.tertiaryInk,
            error = palette.warning,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.canvas,
            onBackground = palette.primaryInk,
            surface = palette.surface,
            onSurface = palette.primaryInk,
            surfaceVariant = palette.surfaceHighlight,
            onSurfaceVariant = palette.secondaryInk,
            outline = palette.tertiaryInk,
            error = palette.warning,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            window.setBackgroundDrawable(ColorDrawable(palette.canvas.toArgb()))
        }
    }

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DeepSeekTypography,
            content = content,
        )
    }
}

@Composable
fun DeepSeekWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val mode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    DeepSeekTheme(themeMode = mode, content = content)
}
