package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.Paint
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
import kotlin.random.Random

class OrganicFlowRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.ORGANIC_FLOW
    override val displayNameRes = com.example.xargoosh.R.string.style_organic_flow
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val maxParticles = 300

    private val particles = FloatArray(maxParticles * 3)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastTimeNanos = 0L

    init {
        for (i in 0 until maxParticles) {
            particles[i * 3] = Random.nextFloat() * 1000f 
            particles[i * 3 + 1] = Random.nextFloat() * 1000f
            particles[i * 3 + 2] = Random.nextFloat() * 100f
        }
    }

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos
        val time = state.animationTimeNanos / 1_000_000_000.0

        val speedMult = 1.0f + state.rmsLoudness * 2.0f
        val activeCount = if (state.reduceMotion) 50 else maxParticles

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            for (i in 0 until activeCount) {
                var px = particles[i * 3]
                var py = particles[i * 3 + 1]
                var age = particles[i * 3 + 2]

                val noiseScale = 0.005f
                val nx = px * noiseScale
                val ny = py * noiseScale

                var angle = 0f
                if (!state.reduceMotion) {
                    val bandVal = state.smoothedBands[(i % 32) * 8]
                    angle = (sin(nx + time) + cos(ny + time) + bandVal * 2).toFloat() * Math.PI.toFloat()
                }

                val speed = (1f + (age % 2f)) * speedMult * (deltaMs / 16f)
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed

                px += vx
                py += vy
                age += deltaMs * 0.01f

                if (px < 0 || px > width || py < 0 || py > height) {
                    px = Random.nextFloat() * width
                    py = Random.nextFloat() * height
                    age = 0f
                }

                particles[i * 3] = px
                particles[i * 3 + 1] = py
                particles[i * 3 + 2] = age

                val speedRatio = (speed / 5f).coerceIn(0f, 1f)
                val pColor = lerpColor(palette.primary, palette.secondary, speedRatio)

                paint.color = pColor.copy(alpha = 0.4f).toArgb()
                val size = 2f + state.smoothedBands[i % 256] * 4f
                nativeCanvas.drawCircle(px, py, size, paint)
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

