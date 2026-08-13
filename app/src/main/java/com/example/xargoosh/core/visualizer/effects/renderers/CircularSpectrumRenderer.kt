package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.LinearGradient
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CircularSpectrumRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.CIRCULAR_SPECTRUM
    override val displayNameRes = com.example.xargoosh.R.string.style_circular_spectrum
    override val supportsFFT = true
    override val supportsBlurredBackground = false
    override val supportsAlbumArtwork = true

    private val barPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var currentRadiusMult = 1f
    private val BARS_COUNT = 120

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val minDim = min(width, height)
        val baseRadius = minDim * 0.4f

        if (state.isOnBeat && !state.reduceMotion) {
            currentRadiusMult = 1.1f
        } else {
            currentRadiusMult += (1f - currentRadiusMult) * 0.1f
        }

        val radius = baseRadius * currentRadiusMult
        val maxBarLength = minDim * 0.2f

        val primaryArgb = palette.primary.toArgb()
        val secondaryArgb = palette.secondary.toArgb()

        if (barPaint.shader == null || currentRadiusMult != 1.0f) {


            barPaint.shader = android.graphics.SweepGradient(cx, cy, intArrayOf(primaryArgb, secondaryArgb, primaryArgb), floatArrayOf(0f, 0.5f, 1f))
        }

        drawScope.drawIntoCanvas { canvas ->
            val glowIntensity = state.bassEnergy
            if (glowIntensity > 0) {
                val outerColor = ColorUtils.setAlphaComponent(primaryArgb, 0)
                val innerColor = ColorUtils.setAlphaComponent(primaryArgb, (255 * 0.3f * glowIntensity).toInt())
                glowPaint.shader = RadialGradient(cx, cy, radius * 1.5f, innerColor, outerColor, Shader.TileMode.CLAMP)
                canvas.nativeCanvas.drawCircle(cx, cy, radius * 1.5f, glowPaint)
            }

            barPaint.strokeWidth = (Math.PI * radius * 2 / BARS_COUNT).toFloat() * 0.7f
            barPaint.setShadowLayer(5f, 0f, 0f, ColorUtils.setAlphaComponent(primaryArgb, (255 * 0.5f).toInt()))

            for (i in 0 until BARS_COUNT) {
                val angle = (i.toFloat() / BARS_COUNT) * Math.PI * 2 - Math.PI / 2

                val barLen = if (state.smoothedBands.isNotEmpty()) {
                    state.smoothedBands[i % 256] * maxBarLength
                } else if (state.waveform.isNotEmpty()) {
                    Math.abs(state.waveform[(i * state.waveform.size / BARS_COUNT) % state.waveform.size]) * maxBarLength
                } else {
                    0f
                }

                val startX = cx + cos(angle).toFloat() * radius
                val startY = cy + sin(angle).toFloat() * radius

                val endX = cx + cos(angle).toFloat() * (radius + barLen)
                val endY = cy + sin(angle).toFloat() * (radius + barLen)



                canvas.nativeCanvas.drawLine(startX, startY, endX, endY, barPaint)
            }
        }
    }
}


