package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.max

class SpectrumBarsRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.SPECTRUM_BARS
    override val displayNameRes = com.example.xargoosh.R.string.style_spectrum_bars
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val BARS_COUNT = 64
    private val peakPositions = FloatArray(BARS_COUNT)
    private val peakVelocities = FloatArray(BARS_COUNT)

    private val barPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val peakPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private var lastHeight = 0f
    private var lastPrimary = 0
    private var lastSecondary = 0

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val primaryArgb = palette.primary.toArgb()
        val secondaryArgb = palette.secondary.toArgb()

        if (height != lastHeight || primaryArgb != lastPrimary || secondaryArgb != lastSecondary) {
            val gradient = LinearGradient(0f, 0f, 0f, height, primaryArgb, secondaryArgb, Shader.TileMode.CLAMP)
            barPaint.shader = gradient
            lastHeight = height
            lastPrimary = primaryArgb
            lastSecondary = secondaryArgb
        }

        peakPaint.color = primaryArgb

        val barWidth = width / BARS_COUNT
        val strokeWidth = barWidth * 0.8f
        barPaint.strokeWidth = strokeWidth
        peakPaint.strokeWidth = strokeWidth

        val brightness = if (state.isOnBeat && !state.reduceMotion) 1.2f else 1.0f

        drawScope.drawIntoCanvas { canvas ->
            for (i in 0 until BARS_COUNT) {
                val dataIndex = (i * 4) % 256
                val rawVal = if (state.smoothedBands.isNotEmpty()) state.smoothedBands[dataIndex] else 0f
                var barHeight = rawVal * height * 0.9f
                barHeight *= brightness

                val x = i * barWidth + barWidth / 2f
                val startY = height
                val endY = height - barHeight

                canvas.nativeCanvas.drawLine(x, startY, x, endY, barPaint)

                if (!state.reduceMotion) {
                    if (barHeight >= peakPositions[i]) {
                        peakPositions[i] = barHeight
                        peakVelocities[i] = 0f
                    } else {
                        peakVelocities[i] += 0.3f
                        peakPositions[i] -= peakVelocities[i]
                        if (peakPositions[i] < 0f) peakPositions[i] = 0f
                    }
                } else {
                    peakPositions[i] = barHeight
                }

                val peakY = height - peakPositions[i]
                canvas.nativeCanvas.drawLine(x, peakY, x, peakY - 1f, peakPaint)
            }
        }
    }
}

