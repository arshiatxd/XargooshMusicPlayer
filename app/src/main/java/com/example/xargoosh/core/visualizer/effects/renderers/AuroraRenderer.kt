package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle

class AuroraRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.AURORA
    override val displayNameRes = com.example.xargoosh.R.string.style_aurora
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val ribbonCount = 12
    private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ribbonPath = Path()
    private val colors = IntArray(2)

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val time = if (state.reduceMotion) 0.0 else state.animationTimeNanos / 1_000_000_000.0
        val segmentWidth = width / (ribbonCount - 1).toFloat()

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            for (i in 0 until ribbonCount - 1) {
                ribbonPath.reset()

                val x1 = i * segmentWidth
                val x2 = (i + 1) * segmentWidth

                val band1 = state.smoothedBands[(i * 20).coerceIn(0, 255)]
                val band2 = state.smoothedBands[((i + 1) * 20).coerceIn(0, 255)]

                val offset1 = if (state.reduceMotion) 0f else Math.sin(time + i).toFloat() * 50f
                val offset2 = if (state.reduceMotion) 0f else Math.sin(time + i + 1).toFloat() * 50f

                val h1 = band1 * height * state.bassEnergy
                val h2 = band2 * height * state.bassEnergy

                val y1 = height - h1 + offset1
                val y2 = height - h2 + offset2

                ribbonPath.moveTo(x1, height)
                ribbonPath.lineTo(x1, y1)

                val cx = (x1 + x2) / 2f
                ribbonPath.cubicTo(cx, y1, cx, y2, x2, y2)
                ribbonPath.lineTo(x2, height)
                ribbonPath.close()

                colors[0] = palette.secondary.copy(alpha = 0.3f).toArgb()
                colors[1] = palette.primary.copy(alpha = 0.6f).toArgb()

                val topY = minOf(y1, y2)
                if (height > topY) {
                    ribbonPaint.shader = LinearGradient(
                        0f, topY, 0f, height,
                        colors, null, Shader.TileMode.CLAMP
                    )
                    nativeCanvas.drawPath(ribbonPath, ribbonPaint)
                }
            }
        }
    }
}

