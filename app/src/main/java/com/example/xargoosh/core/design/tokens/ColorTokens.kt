package com.example.xargoosh.core.design.tokens

import androidx.compose.ui.graphics.Color

data class ColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val glow: Color,
    val glassTint: Color,
    val glassBorder: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onPrimary: Color,
    val error: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val onSecondary: Color = onPrimary,
    val onHighlight: Color = onSurface,
    val onError: Color = onPrimary,
    val primaryContainer: Color = primary.copy(alpha = 0.2f),
    val onPrimaryContainer: Color = onSurface,
    val errorContainer: Color = error.copy(alpha = 0.2f),
    val onErrorContainer: Color = onSurface
)
