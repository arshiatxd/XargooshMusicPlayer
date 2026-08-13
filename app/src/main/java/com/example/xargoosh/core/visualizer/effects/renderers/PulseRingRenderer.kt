package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.min

class PulseRingRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.PULSE_RING
    override val displayNameRes = com.example.xargoosh.R.string.style_pulse_ring
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = true

    private val MAX_RINGS = 6
    private val ringRadii = FloatArray(MAX_RINGS) { -1f }
    private val ringAlphas = FloatArray(MAX_RINGS) { 0f }

    private val ringPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var lastTimestamp = 0L

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val minDim = min(width, height)
        val baseRadius = minDim * 0.3f

        if (lastTimestamp == 0L) lastTimestamp = state.animationTimeNanos
        val delta = (state.animationTimeNanos - lastTimestamp).coerceIn(0L, 50_000_000L)
        lastTimestamp = state.animationTimeNanos
        val frames = delta / 16_666_666f

        if (state.isOnBeat && !state.reduceMotion) {
            for (i in 0 until MAX_RINGS) {
                if (ringRadii[i] < 0f) {
                    ringRadii[i] = baseRadius
                    ringAlphas[i] = 1f
                    break
                }
            }
        }

        val glowArgb = palette.glow.toArgb()
        val thickness = 2f * drawScope.density + (state.bassEnergy * 6f * drawScope.density)
        ringPaint.strokeWidth = thickness

        drawScope.drawIntoCanvas { canvas ->
            val glowIntensity = state.rmsLoudness
            if (glowIntensity > 0) {
                val outerColor = ColorUtils.setAlphaComponent(glowArgb, 0)
                val innerColor = ColorUtils.setAlphaComponent(glowArgb, (255 * 0.4f * glowIntensity).toInt())
                glowPaint.shader = RadialGradient(cx, cy, baseRadius * 1.5f, innerColor, outerColor, Shader.TileMode.CLAMP)
                canvas.nativeCanvas.drawCircle(cx, cy, baseRadius * 1.5f, glowPaint)
            }

            ringPaint.color = ColorUtils.setAlphaComponent(glowArgb, (255 * (0.3f + 0.7f * glowIntensity)).toInt())
            canvas.nativeCanvas.drawCircle(cx, cy, baseRadius, ringPaint)

            if (!state.reduceMotion) {
                for (i in 0 until MAX_RINGS) {
                    if (ringRadii[i] >= 0f) {
                        ringRadii[i] += 2f * drawScope.density * frames
                        ringAlphas[i] -= 0.02f * frames

                        if (ringAlphas[i] <= 0f) {
                            ringRadii[i] = -1f
                        } else {
                            ringPaint.color = ColorUtils.setAlphaComponent(glowArgb, (255 * ringAlphas[i]).toInt())
                            canvas.nativeCanvas.drawCircle(cx, cy, ringRadii[i], ringPaint)
                        }
                    }
                }
            }
        }
    }
}

