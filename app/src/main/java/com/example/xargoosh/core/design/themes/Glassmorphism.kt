package com.example.xargoosh.core.design.themes

import com.example.xargoosh.core.design.themes.XargooshTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import androidx.compose.material3.MaterialTheme

fun Modifier.glassmorphism(
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 10.dp,
    lightMode: Boolean = true
): Modifier = composed {
    val usesGlass = XargooshTheme.appTheme.usesGlass
    val backgroundColor = if (lightMode) XargooshTheme.colors.surface else XargooshTheme.colors.surface
    val borderColors = if (lightMode) {
        listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f))
    } else {
        listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
    }

    if (!usesGlass) return@composed this
        .clip(RoundedCornerShape(cornerRadius))
        .background(XargooshTheme.colors.surface)
        .border(1.dp, XargooshTheme.colors.outline.copy(alpha = 0.45f), RoundedCornerShape(cornerRadius))

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(colors = borderColors),
            shape = RoundedCornerShape(cornerRadius)
        )
}
