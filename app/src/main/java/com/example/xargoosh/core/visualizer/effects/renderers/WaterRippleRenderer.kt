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

class WaterRippleRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.WATER_RIPPLE
    override val displayNameRes = com.example.xargoosh.R.string.style_water_ripple
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val maxRipples = 8
    private val ripples = FloatArray(maxRipples) 
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE 
    }

    private var lastTimeNanos = 0L

    init {
        for (i in 0 until maxRipples) {
            ripples[i] = -1f
        }
    }

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) * 0.6f

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos

        if (state.reduceMotion) {
            drawScope.drawIntoCanvas { canvas ->
                paint.color = palette.glow.copy(alpha = 0.5f + state.bassEnergy * 0.5f).toArgb()
                paint.strokeWidth = 3f + state.bassEnergy * 10f
                canvas.nativeCanvas.drawCircle(cx, cy, maxRadius * 0.5f, paint)
            }
            return
        }

        if (state.isOnBeat) {
            for (i in 0 until maxRipples) {
                if (ripples[i] < 0f) {
                    ripples[i] = 0f
                    break
                }
            }
        }

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            for (i in 0 until maxRipples) {
                var r = ripples[i]
                if (r >= 0f) {
                    r += deltaMs * 0.3f

                    if (r > maxRadius) {
                        ripples[i] = -1f
                    } else {
                        ripples[i] = r
                        val progress = r / maxRadius
                        val alpha = (0.8f * (1f - progress)).coerceIn(0f, 1f)
                        val thick = (6f * (1f - progress)).coerceAtLeast(1f)

                        paint.color = palette.glow.copy(alpha = alpha).toArgb()
                        paint.strokeWidth = thick
                        nativeCanvas.drawCircle(cx, cy, r, paint)
                    }
                }
            }
        }
    }
}

