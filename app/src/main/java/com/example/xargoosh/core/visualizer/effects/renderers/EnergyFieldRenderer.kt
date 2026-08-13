package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

class EnergyFieldRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.ENERGY_FIELD
    override val displayNameRes = com.example.xargoosh.R.string.style_energy_field
    override val supportsFFT = true
    override val supportsBlurredBackground = false
    override val supportsAlbumArtwork = false

    private val fieldPaints = Array(3) { 
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        } 
    }
    private val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = IntArray(2)

    private var currentAngle = 0f
    private var lastTimeNanos = 0L

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos

        if (!state.reduceMotion) {
            currentAngle += deltaMs * 0.0002f
        }

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val maxR = maxOf(width, height) * 0.8f

            val energies = floatArrayOf(state.bassEnergy, state.midEnergy, state.trebleEnergy)
            val pColors = arrayOf(palette.primary, palette.glow, palette.accent)

            for (i in 0..2) {
                val angle = currentAngle + (i * 2 * Math.PI / 3)
                val offset = maxR * 0.2f
                val px = cx + (cos(angle) * offset).toFloat()
                val py = cy + (sin(angle) * offset).toFloat()

                val alpha = (0.3f + energies[i] * 0.7f).coerceIn(0f, 1f)
                colors[0] = pColors[i].copy(alpha = alpha).toArgb()
                colors[1] = pColors[i].copy(alpha = 0f).toArgb()

                fieldPaints[i].shader = RadialGradient(
                    px, py, maxR, colors, null, Shader.TileMode.CLAMP
                )
                nativeCanvas.drawRect(0f, 0f, width, height, fieldPaints[i])
            }

            val centerR = maxR * (0.2f + state.bassEnergy * 0.3f)
            if (centerR > 0) {
                colors[0] = palette.glow.copy(alpha = state.bassEnergy.coerceIn(0f, 1f)).toArgb()
                colors[1] = palette.glow.copy(alpha = 0f).toArgb()
                centerGlowPaint.shader = RadialGradient(
                    cx, cy, centerR, colors, null, Shader.TileMode.CLAMP
                )
                nativeCanvas.drawCircle(cx, cy, centerR, centerGlowPaint)
            }
        }
    }
}

