package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class GalaxyRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.GALAXY
    override val displayNameRes = com.example.xargoosh.R.string.style_galaxy
    override val supportsFFT = true
    override val supportsBlurredBackground = false
    override val supportsAlbumArtwork = true

    private val maxParticles = 150

    private val particles = FloatArray(maxParticles * 3)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = IntArray(2)

    private var currentAngle = 0f
    private var lastTimeNanos = 0L

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos

        val speedBurst = if (state.isOnBeat) 2f else 1f
        val rotationSpeed = if (state.reduceMotion) 0f else deltaMs * 0.0001f * (1f + state.rmsLoudness) * speedBurst
        currentAngle += rotationSpeed

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            val centerRadius = (width.coerceAtMost(height) / 4f) + (state.bassEnergy * 50f)
            if (centerRadius > 0) {
                colors[0] = palette.glow.copy(alpha = state.bassEnergy * 0.8f).toArgb()
                colors[1] = palette.glow.copy(alpha = 0f).toArgb()
                centerGlowPaint.shader = RadialGradient(
                    cx, cy, centerRadius, colors, null, Shader.TileMode.CLAMP
                )
                nativeCanvas.drawCircle(cx, cy, centerRadius, centerGlowPaint)
            }

            val a = 5f
            val b = 0.15f

            for (i in 0 until maxParticles) {
                val theta = i * 0.2f + currentAngle
                val r = a * exp(b * theta)

                val px = cx + (r * cos(theta)).toFloat()
                val py = cy + (r * sin(theta)).toFloat()

                val brightness = state.smoothedBands[i % 256]

                particles[i * 3] = px
                particles[i * 3 + 1] = py
                particles[i * 3 + 2] = brightness

                val fraction = brightness.coerceIn(0f, 1f)
                val pColor = lerpColor(palette.primary, palette.accent, fraction)

                particlePaint.color = pColor.copy(alpha = fraction * 0.8f + 0.2f).toArgb()

                val pSize = 3f + (brightness * 8f)
                nativeCanvas.drawCircle(px, py, pSize, particlePaint)
            }
        }
    }

    private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        return Color(
            red = c1.red + (c2.red - c1.red) * fraction,
            green = c1.green + (c2.green - c1.green) * fraction,
            blue = c1.blue + (c2.blue - c1.blue) * fraction,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * fraction
        )
    }
}

