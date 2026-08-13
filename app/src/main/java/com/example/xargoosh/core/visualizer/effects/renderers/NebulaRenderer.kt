package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
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

class NebulaRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.NEBULA
    override val displayNameRes = com.example.xargoosh.R.string.style_nebula
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val blobPaints = Array(4) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = IntArray(2)
    private val positions = FloatArray(4 * 2) 

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val time = state.animationTimeNanos / 1_000_000_000.0
        val t = if (state.reduceMotion) 0.0 else time

        val baseOpacity = (0.3f + state.rmsLoudness * 0.5f).coerceIn(0.3f, 0.8f)

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            val centerRadius = (width.coerceAtMost(height) / 3f) + (state.bassEnergy * 100f)
            if (centerRadius > 0) {
                colors[0] = palette.glow.copy(alpha = state.bassEnergy * baseOpacity).toArgb()
                colors[1] = palette.glow.copy(alpha = 0f).toArgb()
                centerGlowPaint.shader = RadialGradient(
                    cx, cy, centerRadius, colors, null, Shader.TileMode.CLAMP
                )
                nativeCanvas.drawCircle(cx, cy, centerRadius, centerGlowPaint)
            }

            val paletteColors = arrayOf(palette.primary, palette.secondary, palette.glow, palette.primary)

            for (i in 0 until 4) {
                val angle = t * (0.2 + i * 0.1) + i * (Math.PI / 2)
                val dist = (width.coerceAtMost(height) / 4f) * (0.8 + 0.2 * sin(t * 0.5 + i))

                positions[i * 2] = cx + (cos(angle) * dist).toFloat()
                positions[i * 2 + 1] = cy + (sin(angle) * dist).toFloat()

                val bandIdx = (i * 64).coerceIn(0, 255)
                val bandValue = state.smoothedBands[bandIdx]
                val radius = (width.coerceAtMost(height) / 3f) + (bandValue * 150f)

                if (radius > 0) {
                    val color = paletteColors[i % paletteColors.size]
                    colors[0] = color.copy(alpha = baseOpacity).toArgb()
                    colors[1] = color.copy(alpha = 0f).toArgb()

                    val px = positions[i * 2]
                    val py = positions[i * 2 + 1]

                    blobPaints[i].shader = RadialGradient(
                        px, py, radius, colors, null, Shader.TileMode.CLAMP
                    )
                    nativeCanvas.drawCircle(px, py, radius, blobPaints[i])
                }
            }
        }
    }
}

