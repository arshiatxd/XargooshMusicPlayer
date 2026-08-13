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

class ParticlesRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.PARTICLES
    override val displayNameRes = com.example.xargoosh.R.string.style_particles
    override val supportsFFT = false
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val maxParticles = 200

    private val particles = FloatArray(maxParticles * 5)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastTimeNanos = 0L

    init {
        for (i in 0 until maxParticles) {
            particles[i * 5 + 4] = 0f 
        }
    }

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val cx = width / 2f
        val cy = height / 2f

        if (state.reduceMotion) {

            drawScope.drawIntoCanvas { canvas ->
                paint.color = palette.primary.toArgb()
                paint.strokeWidth = 4f
                canvas.nativeCanvas.drawLine(0f, cy, width, cy, paint)
            }
            return
        }

        val deltaMs = (if (lastTimeNanos == 0L) 16f else (state.animationTimeNanos - lastTimeNanos) / 1_000_000f).coerceIn(0f, 50f)
        lastTimeNanos = state.animationTimeNanos

        val orbitRadius = 80f + (state.bassEnergy * 120f)
        val speedMult = 0.5f + (state.rmsLoudness * 2.5f)

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            for (i in 0 until maxParticles) {
                var life = particles[i * 5 + 4]

                if (life <= 0f) {

                    val angle = Random.nextFloat() * 2 * Math.PI
                    particles[i * 5] = cx + (cos(angle) * orbitRadius).toFloat()
                    particles[i * 5 + 1] = cy + (sin(angle) * orbitRadius).toFloat()

                    val speed = Random.nextFloat() * 2f + 1f
                    particles[i * 5 + 2] = (cos(angle) * speed).toFloat()
                    particles[i * 5 + 3] = (sin(angle) * speed).toFloat()

                    life = 1f
                }

                if (state.isOnBeat) {
                    particles[i * 5 + 2] *= 1.5f
                    particles[i * 5 + 3] *= 1.5f
                }

                val vx = particles[i * 5 + 2] * speedMult * (deltaMs / 16f)
                val vy = particles[i * 5 + 3] * speedMult * (deltaMs / 16f)

                particles[i * 5] += vx
                particles[i * 5 + 1] += vy
                life -= deltaMs * 0.001f 
                particles[i * 5 + 4] = life

                if (life > 0) {
                    val pColorInt = if (life > 0.5f) {
                        androidx.core.graphics.ColorUtils.blendARGB(palette.glow.toArgb(), palette.primary.toArgb(), (life - 0.5f) * 2f)
                    } else {
                        androidx.core.graphics.ColorUtils.setAlphaComponent(palette.glow.toArgb(), (255 * life * 2f).toInt().coerceIn(0, 255))
                    }

                    paint.color = pColorInt
                    val pSize = 4f * life
                    nativeCanvas.drawCircle(particles[i * 5], particles[i * 5 + 1], pSize, paint)
                }
            }
        }
    }

}

