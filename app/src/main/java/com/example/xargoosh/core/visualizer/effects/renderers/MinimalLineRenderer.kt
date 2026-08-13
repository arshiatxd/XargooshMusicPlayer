package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.max

class MinimalLineRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.MINIMAL_LINE
    override val displayNameRes = com.example.xargoosh.R.string.style_minimal_line
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val path = Path()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerY = height / 2f

        paint.color = palette.primary.toArgb()

        val baseThickness = 1.5f * drawScope.density
        val thickness = if (state.reduceMotion) {
            baseThickness
        } else {
            baseThickness * (1f + state.rmsLoudness)
        }
        paint.strokeWidth = max(1f, thickness)

        path.reset()
        val data = state.waveform
        if (data.isEmpty() || state.reduceMotion) {
            path.moveTo(0f, centerY)
            path.lineTo(width, centerY)
        } else {
            val stepX = width / (data.size - 1)
            for (i in data.indices) {
                val x = i * stepX
                val y = centerY + (data[i] * height * 0.4f)
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
        }

        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(path, paint)
        }
    }
}

