package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
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

class HexGridRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.HEX_GRID
    override val displayNameRes = com.example.xargoosh.R.string.style_hex_grid
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val hexSize = 30f
    private val hexPath = Path()
    private val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var positions: FloatArray? = null
    private var hexCount = 0
    private var beatFlashTime = 0f
    private var lastTimeNanos = 0L

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        if (positions == null && width > 0 && height > 0) {
            initGrid(width, height)
        }

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos

        if (state.isOnBeat && !state.reduceMotion) {
            beatFlashTime = 100f
        }
        if (beatFlashTime > 0) {
            beatFlashTime -= deltaMs
        }
        val flashBoost = if (beatFlashTime > 0) (beatFlashTime / 100f) * 0.3f else 0f

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val pos = positions ?: return@drawIntoCanvas

            for (i in 0 until hexCount) {
                val cx = pos[i * 2]
                val cy = pos[i * 2 + 1]

                val bandVal = if (state.reduceMotion) 0.5f else state.smoothedBands[i % 256]
                val baseAlpha = (bandVal * 0.9f).coerceIn(0f, 1f)
                val alpha = (baseAlpha + flashBoost).coerceIn(0f, 1f)

                val color = lerpColor(palette.primary, palette.secondary, bandVal)
                hexPaint.color = color.copy(alpha = alpha).toArgb()

                buildHexPath(cx, cy, hexSize * 0.9f)
                nativeCanvas.drawPath(hexPath, hexPaint)
            }
        }
    }

    private fun buildHexPath(cx: Float, cy: Float, radius: Float) {
        hexPath.reset()
        for (i in 0..5) {
            val angle_deg = 60 * i - 30
            val angle_rad = Math.PI / 180 * angle_deg
            val px = cx + radius * cos(angle_rad).toFloat()
            val py = cy + radius * sin(angle_rad).toFloat()
            if (i == 0) hexPath.moveTo(px, py)
            else hexPath.lineTo(px, py)
        }
        hexPath.close()
    }

    private fun initGrid(width: Float, height: Float) {
        val w = Math.sqrt(3.0).toFloat() * hexSize
        val h = 2f * hexSize
        val cols = (width / w).toInt() + 2
        val rows = (height / (h * 0.75f)).toInt() + 2

        hexCount = cols * rows
        positions = FloatArray(hexCount * 2)

        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val offset = if (r % 2 == 1) w / 2f else 0f
                positions!![idx * 2] = c * w + offset
                positions!![idx * 2 + 1] = r * h * 0.75f
                idx++
            }
        }
    }

    private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
        return Color(
            red = c1.red + (c2.red - c1.red) * fraction,
            green = c1.green + (c2.green - c1.green) * fraction,
            blue = c1.blue + (c2.blue - c1.blue) * fraction,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * fraction
        )
    }
}

