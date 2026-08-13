package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
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

class LiquidRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.LIQUID
    override val displayNameRes = com.example.xargoosh.R.string.style_liquid
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val blobPath = Path()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val colors = IntArray(2)
    private val numPoints = 64

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f
        val minDim = minOf(width, height)

        val time = if (state.reduceMotion) 0.0 else state.animationTimeNanos / 1_000_000_000.0
        val baseRadius = minDim * (0.3f + state.bassEnergy * 0.3f)

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            blobPath.reset()

            for (i in 0..numPoints) {
                val angle = (i.toFloat() / numPoints) * 2 * Math.PI

                var radiusOffset = 0f
                if (!state.reduceMotion) {
                    for (f in 1..8) {
                        val bandVal = state.smoothedBands[(f * 8).coerceIn(0, 255)]
                        val freqOffset = sin(angle * f + time * f) * (bandVal * 30f)
                        radiusOffset += freqOffset.toFloat()
                    }
                }

                val r = baseRadius + radiusOffset
                val px = cx + (cos(angle) * r).toFloat()
                val py = cy + (sin(angle) * r).toFloat()

                if (i == 0) blobPath.moveTo(px, py)
                else blobPath.lineTo(px, py)
            }
            blobPath.close()

            colors[0] = palette.primary.toArgb()
            colors[1] = palette.secondary.toArgb()
            blobPaint.shader = RadialGradient(
                cx, cy, baseRadius * 1.5f, colors, null, Shader.TileMode.CLAMP
            )

            nativeCanvas.drawPath(blobPath, blobPaint)

            outlinePaint.color = palette.glow.copy(alpha = 0.7f).toArgb()
            nativeCanvas.drawPath(blobPath, outlinePaint)
        }
    }
}

