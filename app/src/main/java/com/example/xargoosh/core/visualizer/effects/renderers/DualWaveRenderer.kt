package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import kotlin.math.max

class DualWaveRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.DUAL_WAVE
    override val displayNameRes = com.example.xargoosh.R.string.style_dual_wave
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val topPath = Path()
    private val bottomPath = Path()
    private val fillTopPath = Path()
    private val fillBottomPath = Path()

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var lastWidth = 0f
    private var lastPrimary = 0
    private var lastSecondary = 0

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerY = height / 2f

        val primaryArgb = palette.primary.toArgb()
        val secondaryArgb = palette.secondary.toArgb()

        if (width != lastWidth || primaryArgb != lastPrimary || secondaryArgb != lastSecondary) {
            val lineGradient = LinearGradient(0f, 0f, width, 0f, primaryArgb, secondaryArgb, Shader.TileMode.CLAMP)
            linePaint.shader = lineGradient

            lastWidth = width
            lastPrimary = primaryArgb
            lastSecondary = secondaryArgb
        }

        linePaint.strokeWidth = 2f * drawScope.density

        val data = state.waveform
        val amplitudeMult = if (state.reduceMotion) 1f else 1f + (state.midEnergy * 2f)

        topPath.reset()
        bottomPath.reset()
        fillTopPath.reset()
        fillBottomPath.reset()

        if (data.isNotEmpty()) {
            val stepX = width / (data.size - 1)

            fillTopPath.moveTo(0f, centerY)
            fillBottomPath.moveTo(0f, centerY)

            for (i in data.indices) {
                val x = i * stepX
                val v = Math.abs(data[i] * height * 0.2f * amplitudeMult)

                val finalYTop = centerY - v
                val finalYBottom = centerY + v

                if (i == 0) {
                    topPath.moveTo(x, finalYTop)
                    bottomPath.moveTo(x, finalYBottom)
                } else {
                    topPath.lineTo(x, finalYTop)
                    bottomPath.lineTo(x, finalYBottom)
                }

                fillTopPath.lineTo(x, finalYTop)
                fillBottomPath.lineTo(x, finalYBottom)
            }

            fillTopPath.lineTo(width, centerY)
            fillBottomPath.lineTo(width, centerY)
            fillTopPath.close()
            fillBottomPath.close()
        } else {
            topPath.moveTo(0f, centerY)
            topPath.lineTo(width, centerY)
            bottomPath.moveTo(0f, centerY)
            bottomPath.lineTo(width, centerY)
        }

        drawScope.drawIntoCanvas { canvas ->
            val topFillGradient = LinearGradient(0f, centerY - (height * 0.2f * amplitudeMult), 0f, centerY,
                ColorUtils.setAlphaComponent(primaryArgb, (255 * 0.2f).toInt()),
                ColorUtils.setAlphaComponent(primaryArgb, 0), Shader.TileMode.CLAMP)

            val bottomFillGradient = LinearGradient(0f, centerY + (height * 0.2f * amplitudeMult), 0f, centerY,
                ColorUtils.setAlphaComponent(primaryArgb, (255 * 0.2f).toInt()),
                ColorUtils.setAlphaComponent(primaryArgb, 0), Shader.TileMode.CLAMP)

            fillPaint.shader = topFillGradient
            canvas.nativeCanvas.drawPath(fillTopPath, fillPaint)

            fillPaint.shader = bottomFillGradient
            canvas.nativeCanvas.drawPath(fillBottomPath, fillPaint)

            canvas.nativeCanvas.drawPath(topPath, linePaint)
            canvas.nativeCanvas.drawPath(bottomPath, linePaint)
        }
    }
}

