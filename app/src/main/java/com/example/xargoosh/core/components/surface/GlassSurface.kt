package com.example.xargoosh.core.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.xargoosh.core.design.themes.XargooshTheme

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = XargooshTheme.shapes.medium,
    elevation: Dp = XargooshTheme.elevation.none,
    blurRadius: Dp = XargooshTheme.blur.medium,
    color: Color = XargooshTheme.colors.surface,
    borderColor: Color = XargooshTheme.colors.glassBorder,
    opaque: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val usesGlass = XargooshTheme.appTheme.usesGlass && !opaque
    var baseModifier = modifier
        .then(
            if (elevation > XargooshTheme.elevation.none && !usesGlass) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = XargooshTheme.colors.glow,
                    spotColor = XargooshTheme.colors.primary.copy(alpha = 0.25f)
                )
            } else Modifier
        )
        .clip(shape)

    baseModifier = if (usesGlass) {
        baseModifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (color.alpha < 0.5f) 0.14f else 0.2f),
                    color.copy(alpha = (color.alpha + 0.1f).coerceAtMost(1f)),
                    color
                )
            )
        ).then(
            if (borderColor != Color.Transparent) Modifier.border(
                width = XargooshTheme.elevation.level1,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.72f), borderColor.copy(alpha = 0.85f), borderColor.copy(alpha = 0.28f))
                ),
                shape = shape
            ) else Modifier
        )
    } else {
        baseModifier
            .background(if (opaque) color.copy(alpha = 1f) else color)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(
                    XargooshTheme.elevation.level1,
                    XargooshTheme.colors.outline.copy(alpha = 0.45f),
                    shape
                ) else Modifier
            )
    }

    if (onClick != null) baseModifier = baseModifier.clickable(onClick = onClick)

    Box(
        modifier = baseModifier,
        content = content
    )
}
