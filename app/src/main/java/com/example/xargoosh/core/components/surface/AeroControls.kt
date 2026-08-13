package com.example.xargoosh.core.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.xargoosh.core.design.themes.XargooshTheme
import androidx.compose.material3.LocalContentColor

@Composable
fun AeroIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val aero = XargooshTheme.appTheme.isAero
    if (aero) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .background(aeroGlossBrush())
                .border(1.dp, XargooshTheme.colors.glassBorder.copy(alpha = 0.9f), CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else 0.5f),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides XargooshTheme.colors.onSurface, content = content)
        }
        return
    }
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = if (aero) IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = XargooshTheme.colors.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
        ) else colors,
        content = content
    )
}

@Composable
fun AeroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val aero = XargooshTheme.appTheme.isAero
    if (aero) {
        Row(
            modifier = modifier
                .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
                .clip(shape)
                .background(aeroGlossBrush())
                .border(1.dp, XargooshTheme.colors.glassBorder.copy(alpha = 0.9f), shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(contentPadding)
                .alpha(if (enabled) 1f else 0.5f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides XargooshTheme.colors.onSurface) { content() }
        }
        return
    }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = if (aero) {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = XargooshTheme.colors.onSurface,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = XargooshTheme.colors.onSurfaceVariant
            )
        } else colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AeroFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = XargooshTheme.colors.primary,
    contentColor: Color = XargooshTheme.colors.onPrimary,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val aero = XargooshTheme.appTheme.isAero
    if (aero) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                .clip(shape)
                .background(aeroGlossBrush())
                .border(1.dp, XargooshTheme.colors.glassBorder.copy(alpha = 0.9f), shape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides XargooshTheme.colors.onSurface, content = content)
        }
        return
    }
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = if (aero) Color.Transparent else containerColor,
        contentColor = if (aero) XargooshTheme.colors.onSurface else contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
private fun aeroGlossBrush() = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.42f),
    0.28f to XargooshTheme.colors.primary.copy(alpha = 0.26f),
    0.52f to XargooshTheme.colors.surfaceVariant.copy(alpha = 0.86f),
    1f to XargooshTheme.colors.surface.copy(alpha = 0.96f)
)
