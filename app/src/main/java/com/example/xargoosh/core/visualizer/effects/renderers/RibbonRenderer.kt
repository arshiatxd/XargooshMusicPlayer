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
import kotlin.math.cos
import kotlin.math.sin

class RibbonRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.RIBBON
    override val displayNameRes = com.example.xargoosh.R.string.style_ribbon
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val segments = 50
    private val pts = FloatArray(segments * 3) 
    private val ribbonPath = Path()
    private val frontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val time = if (state.reduceMotion) 0.0 else state.animationTimeNanos / 1_000_000_000.0

        frontPaint.color = palette.primary.copy(alpha = 0.8f).toArgb()
        backPaint.color = palette.secondary.copy(alpha = 0.6f).toArgb()

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            val segmentWidth = width / (segments - 1).toFloat()
            for (i in 0 until segments) {
                val x = i * segmentWidth
                val waveIdx = (i * 10).coerceIn(0, 1023)
                val waveVal = state.waveform[waveIdx]
                val yOffset = sin(time + i * 0.1) * 50f + waveVal * 50f
                val zOffset = cos(time + i * 0.15) * 50f

                pts[i * 3] = x
                pts[i * 3 + 1] = cy + yOffset.toFloat()
                pts[i * 3 + 2] = zOffset.toFloat()
            }

            for (i in 0 until segments - 1) {
                val x1 = pts[i * 3]
                val y1 = pts[i * 3 + 1]
                val z1 = pts[i * 3 + 2]

                val x2 = pts[(i + 1) * 3]
                val y2 = pts[(i + 1) * 3 + 1]
                val z2 = pts[(i + 1) * 3 + 2]

                val w1 = 20f + (state.waveform[(i * 10).coerceIn(0, 1023)] * 40f * state.midEnergy).coerceAtLeast(0f)
                val w2 = 20f + (state.waveform[((i + 1) * 10).coerceIn(0, 1023)] * 40f * state.midEnergy).coerceAtLeast(0f)

                val projW1 = w1 * (1f + z1 * 0.01f)
                val projW2 = w2 * (1f + z2 * 0.01f)

                ribbonPath.reset()
                ribbonPath.moveTo(x1, y1 - projW1)
                ribbonPath.lineTo(x2, y2 - projW2)
                ribbonPath.lineTo(x2, y2 + projW2)
                ribbonPath.lineTo(x1, y1 + projW1)
                ribbonPath.close()

                val paint = if (z1 > 0) frontPaint else backPaint
                nativeCanvas.drawPath(ribbonPath, paint)
            }
        }
    }
}

