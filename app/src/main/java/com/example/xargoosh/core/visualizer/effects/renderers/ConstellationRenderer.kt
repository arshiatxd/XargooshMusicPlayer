package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class ConstellationRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.CONSTELLATION
    override val displayNameRes = com.example.xargoosh.R.string.style_constellation
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val maxDots = 60
    private val positions = FloatArray(maxDots * 2)
    private val basePositions = FloatArray(maxDots * 2)

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var initialized = false

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        if (!initialized || width == 0f || height == 0f) {
            initGrid(width, height)
            initialized = true
        }

        val time = if (state.reduceMotion) 0.0 else state.animationTimeNanos / 1_000_000_000.0

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            for (i in 0 until maxDots) {
                val bandVal = state.smoothedBands[(i * 4).coerceIn(0, 255)]
                val bx = basePositions[i * 2]
                val by = basePositions[i * 2 + 1]

                if (state.reduceMotion) {
                    positions[i * 2] = bx
                    positions[i * 2 + 1] = by
                } else {
                    positions[i * 2] = bx + (cos(time + i) * bandVal * 40f).toFloat()
                    positions[i * 2 + 1] = by + (sin(time + i) * bandVal * 40f).toFloat()
                }
            }

            val maxDist = 120f
            val beatBoost = if (state.isOnBeat) 0.5f else 0f

            for (i in 0 until maxDots) {
                val x1 = positions[i * 2]
                val y1 = positions[i * 2 + 1]

                for (j in i + 1 until maxDots) {
                    val x2 = positions[j * 2]
                    val y2 = positions[j * 2 + 1]
                    val dist = hypot(x2 - x1, y2 - y1)

                    if (dist < maxDist) {
                        val alphaRaw = (1f - (dist / maxDist)) * 0.6f + beatBoost
                        val alpha = alphaRaw.coerceIn(0f, 1f)

                        linePaint.color = palette.secondary.copy(alpha = alpha).toArgb()
                        nativeCanvas.drawLine(x1, y1, x2, y2, linePaint)
                    }
                }
            }

            dotPaint.color = palette.primary.toArgb()
            for (i in 0 until maxDots) {
                val bandVal = state.smoothedBands[(i * 4).coerceIn(0, 255)]
                val size = 3f + (bandVal * 8f)
                nativeCanvas.drawCircle(positions[i * 2], positions[i * 2 + 1], size, dotPaint)
            }
        }
    }

    private fun initGrid(width: Float, height: Float) {
        val cols = 10
        val rows = 6
        val cellW = width / cols
        val cellH = height / rows

        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (idx < maxDots) {
                    basePositions[idx * 2] = c * cellW + cellW / 2f
                    basePositions[idx * 2 + 1] = r * cellH + cellH / 2f
                    idx++
                }
            }
        }
    }
}

