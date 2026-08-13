package com.example.xargoosh.core.design.themes

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.example.xargoosh.core.design.tokens.*

object XargooshTheme {
    val appTheme: AppTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current

    val colors: ColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalColorTokens.current

    val typography: TypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTypographyTokens.current

    val shapes: ShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalShapeTokens.current

    val spacing: SpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacingTokens.current

    val blur: BlurTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalBlurTokens.current

    val elevation: ElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalElevationTokens.current
}

val LocalColorTokens = staticCompositionLocalOf<ColorTokens> { AeroDarkColors }
val LocalAppTheme = staticCompositionLocalOf { AppTheme.AERO }
val LocalTypographyTokens = staticCompositionLocalOf { TypographyTokens() }
val LocalShapeTokens = staticCompositionLocalOf { ShapeTokens() }
val LocalSpacingTokens = staticCompositionLocalOf { SpacingTokens() }
val LocalBlurTokens = staticCompositionLocalOf { BlurTokens() }
val LocalElevationTokens = staticCompositionLocalOf { ElevationTokens() }

@Composable
fun XargooshTheme(
    appTheme: AppTheme = AppTheme.AERO,
    content: @Composable () -> Unit
) {
    val isDark = !appTheme.isLight
    val systemDark = isSystemInDarkTheme()
    val dynamicScheme = if (appTheme == AppTheme.DYNAMIC_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val colors = dynamicScheme?.toTokens() ?: when (appTheme) {
        AppTheme.AERO -> AeroDarkColors
        AppTheme.GREEN_VISTA -> GreenVistaColors
        AppTheme.BLUE_WINDOWS_7 -> BlueWindows7Colors
        AppTheme.CRYSTAL_SKY_AERO -> CrystalSkyAeroColors
        AppTheme.PEARL_AERO -> PearlAeroColors
        AppTheme.MEADOW_AERO -> MeadowAeroColors
        AppTheme.FROSTED -> FrostedColors
        AppTheme.MINIMAL -> MinimalColors
        AppTheme.SUNLIT_PAPER -> SunlitPaperColors
        AppTheme.LAVENDER_DAY -> LavenderDayColors
        AppTheme.AMOLED -> AmoledColors
        AppTheme.MODERN_DARK -> ModernDarkColors
        AppTheme.RETRO -> RetroColors
        AppTheme.PASTEL -> PastelColors
        AppTheme.HIGH_CONTRAST -> HighContrastColors
        AppTheme.DYNAMIC_SYSTEM -> if (systemDark) DynamicFallbackColors else FrostedColors
    }

    val useDarkScheme = if (appTheme == AppTheme.DYNAMIC_SYSTEM) systemDark else isDark
    val baseScheme = if (useDarkScheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    val materialColorScheme = baseScheme.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondary.copy(alpha = 0.2f),
        onSecondaryContainer = colors.onSurface,
        tertiary = colors.highlight,
        onTertiary = colors.onHighlight,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        error = colors.error,
        onError = colors.onError,
        errorContainer = colors.errorContainer,
        onErrorContainer = colors.onErrorContainer,
        outline = colors.outline,
        outlineVariant = colors.glassBorder,
        inverseSurface = colors.onSurface,
        inverseOnSurface = colors.surface,
        inversePrimary = colors.secondary,
        scrim = androidx.compose.ui.graphics.Color.Black
    )

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalColorTokens provides colors,
        LocalTypographyTokens provides TypographyTokens(),
        LocalShapeTokens provides ShapeTokens(),
        LocalSpacingTokens provides SpacingTokens(),
        LocalBlurTokens provides BlurTokens(),
        LocalElevationTokens provides ElevationTokens()
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

private fun ColorScheme.toTokens() = ColorTokens(
    background = background,
    surface = surface,
    surfaceVariant = surfaceVariant,
    primary = primary,
    secondary = secondary,
    highlight = tertiary,
    glow = primary.copy(alpha = 0.34f),
    glassTint = primary.copy(alpha = 0.1f),
    glassBorder = outlineVariant.copy(alpha = 0.55f),
    onBackground = onBackground,
    onSurface = onSurface,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onHighlight = onTertiary,
    onError = onError,
    error = error,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer
)
