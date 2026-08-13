package com.example.xargoosh.domain.visualizer

import androidx.compose.ui.graphics.Color
import com.example.xargoosh.core.design.themes.AppTheme

enum class ParticleStyle { ORBITAL, SCATTER, STREAM, NONE }
enum class GradientStyle { RADIAL, LINEAR, SWEEP, NONE }

data class VisualizerThemeConfig(
    val palette: VisualizerPalette,
    val particleStyle: ParticleStyle,
    val defaultWaveThickness: Float,
    val defaultGlowRadius: Float,
    val defaultAnimationSpeed: Float,
    val defaultBlurAmount: Float,
    val gradientStyle: GradientStyle,
    val defaultRenderer: VisualizerStyle,

    val glowRadius: Float = defaultGlowRadius,
    val particleDensity: Float = 0.5f,
    val waveThickness: Float = defaultWaveThickness,
    val animationSpeed: Float = defaultAnimationSpeed
)

object ThemeAdapter {
    fun getConfig(theme: AppTheme): VisualizerThemeConfig = when (theme) {
        AppTheme.AMOLED -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFFFF3366),
                secondary = Color(0xFFE50914),
                glow = Color(0xFFFF3366),
                background = Color.Black,
                accent = Color.White
            ),
            particleStyle = ParticleStyle.ORBITAL,
            defaultWaveThickness = 2f, defaultGlowRadius = 36f,
            defaultAnimationSpeed = 0.9f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.RADIAL,
            defaultRenderer = VisualizerStyle.CIRCULAR_SPECTRUM,
            glowRadius = 36f, particleDensity = 0.3f, waveThickness = 2f, animationSpeed = 0.9f
        )
        AppTheme.MODERN_DARK -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFF00CFFF),
                secondary = Color(0xFF10B981),
                glow = Color(0xFF00CFFF),
                background = Color(0xFF121212),
                accent = Color(0xFF80FFEA)
            ),
            particleStyle = ParticleStyle.STREAM,
            defaultWaveThickness = 3f, defaultGlowRadius = 24f,
            defaultAnimationSpeed = 1.1f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.LINEAR,
            defaultRenderer = VisualizerStyle.NIER_WAVE,
            glowRadius = 24f, particleDensity = 0.5f, waveThickness = 3f, animationSpeed = 1.1f
        )
        AppTheme.AERO, AppTheme.GREEN_VISTA, AppTheme.BLUE_WINDOWS_7,
        AppTheme.CRYSTAL_SKY_AERO, AppTheme.PEARL_AERO, AppTheme.MEADOW_AERO -> VisualizerThemeConfig(
            palette = when (theme) {
                AppTheme.GREEN_VISTA -> VisualizerPalette(
                    primary = Color(0xFF8BE35B), secondary = Color(0xFF48D8B0),
                    glow = Color(0xFF8BE35B), background = Color(0xFF061B18), accent = Color(0xFFE9FFD8)
                )
                AppTheme.BLUE_WINDOWS_7 -> VisualizerPalette(
                    primary = Color(0xFF66C7FF), secondary = Color(0xFFB2E3FF),
                    glow = Color(0xFF66C7FF), background = Color(0xFF07182D), accent = Color.White
                )
                AppTheme.CRYSTAL_SKY_AERO -> VisualizerPalette(
                    primary = Color(0xFF35BCE7), secondary = Color(0xFF62B96A),
                    glow = Color(0xFF8EDDF3), background = Color(0xFFE8F7FC), accent = Color.White
                )
                AppTheme.PEARL_AERO -> VisualizerPalette(
                    primary = Color(0xFF7D929E), secondary = Color(0xFFA66F87),
                    glow = Color(0xFFC7D2D8), background = Color(0xFFF0F2F4), accent = Color.White
                )
                AppTheme.MEADOW_AERO -> VisualizerPalette(
                    primary = Color(0xFF4FAE55), secondary = Color(0xFF26A5A9),
                    glow = Color(0xFF8ED99A), background = Color(0xFFF0F8E8), accent = Color.White
                )
                else -> VisualizerPalette(
                    primary = Color(0xFF5AB6FF), secondary = Color(0xFF28547C),
                    glow = Color(0xFF5AB6FF), background = Color(0xFF0F172A), accent = Color.White
                )
            },
            particleStyle = ParticleStyle.SCATTER,
            defaultWaveThickness = 5f, defaultGlowRadius = 18f,
            defaultAnimationSpeed = 1.4f, defaultBlurAmount = 20f,
            gradientStyle = GradientStyle.SWEEP,
            defaultRenderer = VisualizerStyle.NEBULA,
            glowRadius = 18f, particleDensity = 1.0f, waveThickness = 5f, animationSpeed = 1.4f
        )
        AppTheme.FROSTED -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFFBFDBFE),
                secondary = Color(0xFF93C5FD),
                glow = Color(0xFFE0F2FE),
                background = Color(0xFFF1F5F9),
                accent = Color(0xFF3B82F6)
            ),
            particleStyle = ParticleStyle.NONE,
            defaultWaveThickness = 3f, defaultGlowRadius = 12f,
            defaultAnimationSpeed = 0.8f, defaultBlurAmount = 25f,
            gradientStyle = GradientStyle.LINEAR,
            defaultRenderer = VisualizerStyle.DUAL_WAVE,
            glowRadius = 12f, particleDensity = 0f, waveThickness = 3f, animationSpeed = 0.8f
        )
        AppTheme.RETRO -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFFFF00FF),
                secondary = Color(0xFF00FFFF),
                glow = Color(0xFFFF00FF),
                background = Color(0xFF2B003E),
                accent = Color(0xFFFFD700)
            ),
            particleStyle = ParticleStyle.STREAM,
            defaultWaveThickness = 6f, defaultGlowRadius = 28f,
            defaultAnimationSpeed = 1.2f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.SWEEP,
            defaultRenderer = VisualizerStyle.SPECTRUM_BARS,
            glowRadius = 28f, particleDensity = 0.6f, waveThickness = 6f, animationSpeed = 1.2f
        )
        AppTheme.MINIMAL -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFF111111),
                secondary = Color(0xFF666666),
                glow = Color.Transparent,
                background = Color.White,
                accent = Color.Black
            ),
            particleStyle = ParticleStyle.NONE,
            defaultWaveThickness = 1.5f, defaultGlowRadius = 0f,
            defaultAnimationSpeed = 0.7f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.NONE,
            defaultRenderer = VisualizerStyle.MINIMAL_LINE,
            glowRadius = 0f, particleDensity = 0f, waveThickness = 1.5f, animationSpeed = 0.7f
        )
        AppTheme.HIGH_CONTRAST -> VisualizerThemeConfig(
            palette = VisualizerPalette(
                primary = Color(0xFFFFFF00),
                secondary = Color(0xFF00FFFF),
                glow = Color(0xFFFFFF00),
                background = Color.Black,
                accent = Color.White
            ),
            particleStyle = ParticleStyle.NONE,
            defaultWaveThickness = 8f, defaultGlowRadius = 0f,
            defaultAnimationSpeed = 1.0f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.NONE,
            defaultRenderer = VisualizerStyle.PULSE_RING,
            glowRadius = 0f, particleDensity = 0f, waveThickness = 8f, animationSpeed = 1.0f
        )
        AppTheme.PASTEL, AppTheme.SUNLIT_PAPER, AppTheme.LAVENDER_DAY -> VisualizerThemeConfig(
            palette = when (theme) {
                AppTheme.SUNLIT_PAPER -> VisualizerPalette(
                    primary = Color(0xFFD99B43), secondary = Color(0xFF718C55),
                    glow = Color(0xFFFFD58A), background = Color(0xFFFFF7E8), accent = Color(0xFF80551E)
                )
                AppTheme.LAVENDER_DAY -> VisualizerPalette(
                    primary = Color(0xFF9A70BC), secondary = Color(0xFF6E98A0),
                    glow = Color(0xFFD9B8FF), background = Color(0xFFF8F3FF), accent = Color(0xFF6B4A86)
                )
                else -> VisualizerPalette(
                    primary = Color(0xFFFFB7B2), secondary = Color(0xFFFFDAC1),
                    glow = Color(0xFFFFC8C4), background = Color(0xFFFAF3F0), accent = Color(0xFFFF8FA3)
                )
            },
            particleStyle = ParticleStyle.SCATTER,
            defaultWaveThickness = 4f, defaultGlowRadius = 16f,
            defaultAnimationSpeed = 0.9f, defaultBlurAmount = 10f,
            gradientStyle = GradientStyle.RADIAL,
            defaultRenderer = VisualizerStyle.PULSE_RING,
            glowRadius = 16f, particleDensity = 0.4f, waveThickness = 4f, animationSpeed = 0.9f
        )
        AppTheme.DYNAMIC_SYSTEM -> VisualizerThemeConfig(
            palette = VisualizerPalette.DEFAULT, 
            particleStyle = ParticleStyle.ORBITAL,
            defaultWaveThickness = 3f, defaultGlowRadius = 20f,
            defaultAnimationSpeed = 1.0f, defaultBlurAmount = 0f,
            gradientStyle = GradientStyle.RADIAL,
            defaultRenderer = VisualizerStyle.NIER_WAVE,
            glowRadius = 20f, particleDensity = 0.5f, waveThickness = 3f, animationSpeed = 1.0f
        )
    }
}
