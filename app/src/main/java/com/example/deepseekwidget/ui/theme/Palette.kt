package com.example.deepseekwidget.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Palette(
    val isDark: Boolean,
    val canvas: Color,
    val canvasEdge: Color,
    val surface: Color,
    val surfaceHighlight: Color,
    val primaryInk: Color,
    val secondaryInk: Color,
    val tertiaryInk: Color,
    val accent: Color,
    val warning: Color,
    val hairline: Color,
    val chipFill: Color,
    val fieldFill: Color,
    val onAccent: Color,
)

fun paletteFor(dark: Boolean): Palette = if (dark) {
    Palette(
        isDark = true,
        canvas = Color(0xFF0B0C0F),
        canvasEdge = Color(0xFF101218),
        surface = Color(0xFF121316),
        surfaceHighlight = Color(0xFF181A20),
        primaryInk = Color(0xFFFFFFFF),
        secondaryInk = Color(0xFF8E95A5),
        tertiaryInk = Color(0xFF636B78),
        accent = Color(0xFF4D6BFE),
        warning = Color(0xFFFF5252),
        hairline = Color(0x14FFFFFF),
        chipFill = Color(0x1AFFFFFF),
        fieldFill = Color(0xFF14161B),
        onAccent = Color(0xFFFFFFFF),
    )
} else {
    Palette(
        isDark = false,
        canvas = Color(0xFFF7F8FA),
        canvasEdge = Color(0xFFEFF2F6),
        surface = Color(0xFFFFFFFF),
        surfaceHighlight = Color(0xFFFAFBFD),
        primaryInk = Color(0xFF111318),
        secondaryInk = Color(0xFF636B78),
        tertiaryInk = Color(0xFF8A919E),
        accent = Color(0xFF3B5BFF),
        warning = Color(0xFFD32F2F),
        hairline = Color(0x14000000),
        chipFill = Color(0x0F000000),
        fieldFill = Color(0xFFF1F3F7),
        onAccent = Color(0xFFFFFFFF),
    )
}

val LocalPalette = staticCompositionLocalOf { paletteFor(true) }
val LocalDarkTheme = staticCompositionLocalOf { true }
