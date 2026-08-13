package com.example.xargoosh.core.visualizer.effects.renderers

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.xargoosh.domain.visualizer.VisualizerPalette
import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class NierWaveRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.NIER_WAVE
    override val displayNameRes = com.example.xargoosh.R.string.style_nier_ambient
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val wavePath = Path()
    private val waveCorePaint = strokePaint()
    private val waveGlowPaint = strokePaint()
    private val ringPaint = strokePaint()
    private val arcPaint = strokePaint()
    private val barPaint = strokePaint().apply { strokeCap = Paint.Cap.BUTT }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arcRect = RectF()

    private val smoothedWaveform = FloatArray(WAVE_POINTS)
    private val pointX = FloatArray(WAVE_POINTS)
    private val pointY = FloatArray(WAVE_POINTS)

    private var lastTimestamp = 0L
    private var globalTime = 0f
    private var outerRotation = 0f
    private var innerRotation = 0f
    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedTreble = 0f
    private var beatGlow = 0f

    private var cachedDensity = -1f
    private var innerDash: DashPathEffect? = null
    private var outerDash: DashPathEffect? = null
    private var cachedGradient: RadialGradient? = null
    private var cachedSweep: SweepGradient? = null
    private var gradientRadius = -1f
    private var gradientCx = -1f
    private var gradientCy = -1f
    private var gradientPrimary = 0
    private var gradientSecondary = 0

    private class Particle {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var size = 0f
        var phase = 0f
    }

    private val particles = Array(MAX_PARTICLES) { Particle() }
    private var particleWidth = 0f
    private var particleHeight = 0f

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return

        val cx = width * 0.5f
        val cy = height * 0.5f
        val minDim = minOf(width, height)
        val density = drawScope.density
        ensureParticles(width, height)
        ensureDashEffects(density)

        if (lastTimestamp == 0L) lastTimestamp = state.animationTimeNanos
        val dt = ((state.animationTimeNanos - lastTimestamp) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastTimestamp = state.animationTimeNanos
        val motion = if (state.reduceMotion) 0f else 1f
        globalTime += dt * motion

        val response = state.intensity.coerceIn(0f, 1f) * 1.8f
        val smoothing = 1f - exp(-dt * 10f)
        smoothedBass = lerp(smoothedBass, state.bassEnergy * response, smoothing)
        smoothedMid = lerp(smoothedMid, state.midEnergy * response, smoothing)
        smoothedTreble = lerp(smoothedTreble, state.trebleEnergy * response, smoothing)
        val beatTarget = if (state.isOnBeat) state.glowIntensity * state.beatSensitivity else 0f
        beatGlow = lerp(beatGlow, beatTarget, 1f - exp(-dt * 7f))
        outerRotation = (outerRotation + dt * motion * (8f + smoothedBass * 18f)) % 360f
        innerRotation = (innerRotation - dt * motion * (15f + smoothedTreble * 24f)) % 360f

        val primary = blend(DEFAULT_CYAN, palette.primary.toArgb(), 0.42f)
        val secondary = blend(DEFAULT_VIOLET, palette.secondary.toArgb(), 0.42f)
        updateWaveform(state, dt)

        drawScope.drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val coreRadius = minDim * (0.036f + smoothedBass * 0.008f)
            val bloomRadius = coreRadius * (5.4f + smoothedBass * 1.8f)
            ensureGradients(cx, cy, bloomRadius, primary, secondary)
            fillPaint.shader = cachedGradient
            native.drawCircle(cx, cy, bloomRadius, fillPaint)

            drawRadialSpectrum(native, state, cx, cy, minDim, density, primary, secondary)
            drawCompoundRings(native, cx, cy, minDim, density, primary, secondary)
            drawWave(native, state, width, cx, cy, minDim, density, primary, secondary)
            drawParticles(native, state, width, height, density, primary, dt)
        }
    }

    private fun drawRadialSpectrum(
        canvas: android.graphics.Canvas,
        state: VisualizerState,
        cx: Float,
        cy: Float,
        minDim: Float,
        density: Float,
        primary: Int,
        secondary: Int
    ) {
        val bands = state.smoothedBands
        val radius = minDim * (0.115f + smoothedBass * 0.008f)
        val maxLength = minDim * (0.065f + state.intensity.coerceIn(0f, 1f) * 0.055f)
        val barCount = when (state.quality) {
            com.example.xargoosh.domain.visualizer.QualityLevel.LOW -> 48
            com.example.xargoosh.domain.visualizer.QualityLevel.MEDIUM -> 72
            else -> RADIAL_BARS
        }
        barPaint.shader = cachedSweep
        barPaint.alpha = (145 + beatGlow * 90f).toInt().coerceIn(0, 255)
        barPaint.strokeWidth = (1f + state.waveThickness.coerceIn(0f, 1f) * 1.8f) * density
        for (i in 0 until barCount) {
            val angle = i * TWO_PI / barCount - PI.toFloat() * 0.5f
            val band = if (bands.isNotEmpty()) bands[i * bands.size / barCount] else smoothedMid
            val length = maxLength * (0.12f + band.coerceIn(0f, 1.5f) * 0.88f)
            val cosA = cos(angle)
            val sinA = sin(angle)
            canvas.drawLine(
                cx + cosA * radius,
                cy + sinA * radius,
                cx + cosA * (radius + length),
                cy + sinA * (radius + length),
                barPaint
            )
        }
        barPaint.shader = null
        barPaint.color = ColorUtils.setAlphaComponent(primary, 105)
        barPaint.strokeWidth = 0.8f * density
        canvas.drawCircle(cx, cy, radius - 3f * density, barPaint)
        barPaint.color = ColorUtils.setAlphaComponent(secondary, 80)
        canvas.drawCircle(cx, cy, radius + maxLength + 3f * density, barPaint)
    }

    private fun drawCompoundRings(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        minDim: Float,
        density: Float,
        primary: Int,
        secondary: Int
    ) {
        val innerRadius = minDim * (0.205f + smoothedMid * 0.006f)
        val outerRadius = minDim * (0.275f + sin(globalTime * 0.65f) * 0.004f)

        canvas.save()
        canvas.rotate(innerRotation, cx, cy)
        ringPaint.pathEffect = innerDash
        ringPaint.color = ColorUtils.setAlphaComponent(primary, (125 + beatGlow * 80f).toInt().coerceIn(0, 255))
        ringPaint.strokeWidth = 1.25f * density
        canvas.drawCircle(cx, cy, innerRadius, ringPaint)
        arcPaint.pathEffect = null
        arcPaint.color = ColorUtils.setAlphaComponent(secondary, 165)
        arcPaint.strokeWidth = 2.1f * density
        setArc(cx, cy, innerRadius + 5f * density)
        canvas.drawArc(arcRect, -20f, 58f + smoothedBass * 25f, false, arcPaint)
        canvas.drawArc(arcRect, 160f, 42f + smoothedTreble * 30f, false, arcPaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(outerRotation, cx, cy)
        ringPaint.pathEffect = outerDash
        ringPaint.color = ColorUtils.setAlphaComponent(secondary, 115)
        ringPaint.strokeWidth = 1f * density
        canvas.drawCircle(cx, cy, outerRadius, ringPaint)
        arcPaint.pathEffect = null
        for (i in 0 until 3) {
            val radius = outerRadius + (i - 1) * 7f * density
            setArc(cx, cy, radius)
            arcPaint.color = ColorUtils.setAlphaComponent(if (i == 1) primary else secondary, 70 + i * 26)
            arcPaint.strokeWidth = (0.8f + i * 0.35f) * density
            canvas.drawArc(arcRect, 18f + i * 37f, 56f + i * 19f, false, arcPaint)
            canvas.drawArc(arcRect, 198f + i * 37f, 35f + i * 17f, false, arcPaint)
        }
        canvas.restore()
        ringPaint.pathEffect = null
    }

    private fun drawWave(
        canvas: android.graphics.Canvas,
        state: VisualizerState,
        width: Float,
        cx: Float,
        cy: Float,
        minDim: Float,
        density: Float,
        primary: Int,
        secondary: Int
    ) {
        val waveWidth = width * 1.08f
        val startX = cx - waveWidth * 0.5f
        val baseY = cy + sin(globalTime * 0.9f) * minDim * 0.012f
        val maxAmp = minDim * (0.035f + state.intensity.coerceIn(0f, 1f) * 0.04f + smoothedBass * 0.025f)
        for (i in 0 until WAVE_POINTS) {
            val progress = i.toFloat() / (WAVE_POINTS - 1)
            val envelope = sin(progress * PI).toFloat()
            val detail = sin(progress * 17f + globalTime * 1.7f) * smoothedMid * minDim * 0.012f +
                sin(progress * 39f - globalTime * 2.3f) * smoothedTreble * minDim * 0.008f
            pointX[i] = startX + progress * waveWidth
            pointY[i] = baseY + smoothedWaveform[i] * maxAmp * envelope + detail
        }
        buildSmoothPath()

        val thickness = (0.7f + state.waveThickness.coerceIn(0f, 1f) * 3.3f) * density
        waveGlowPaint.color = ColorUtils.setAlphaComponent(secondary, (40 + beatGlow * 45f).toInt().coerceIn(0, 255))
        waveGlowPaint.strokeWidth = thickness * (4.2f + state.glowIntensity * 2f)
        canvas.drawPath(wavePath, waveGlowPaint)
        waveGlowPaint.color = ColorUtils.setAlphaComponent(primary, 105)
        waveGlowPaint.strokeWidth = thickness * 2.2f
        canvas.drawPath(wavePath, waveGlowPaint)
        waveCorePaint.color = ColorUtils.blendARGB(primary, Color.White.toArgb(), 0.62f)
        waveCorePaint.strokeWidth = thickness
        canvas.drawPath(wavePath, waveCorePaint)
    }

    private fun drawParticles(
        canvas: android.graphics.Canvas,
        state: VisualizerState,
        width: Float,
        height: Float,
        density: Float,
        primary: Int,
        dt: Float
    ) {
        val count = (state.particleDensity.coerceIn(0f, 1f) * MAX_PARTICLES).toInt()
        if (count == 0) return
        val particleSpeed = if (state.reduceMotion) 0f else 1f
        for (i in 0 until count) {
            val particle = particles[i]
            particle.x += particle.vx * particleSpeed * dt
            particle.y += particle.vy * particleSpeed * dt
            if (particle.x < 0f) particle.x += width else if (particle.x > width) particle.x -= width
            if (particle.y < 0f) particle.y += height else if (particle.y > height) particle.y -= height
            val alpha = ((0.18f + sin(globalTime + particle.phase) * 0.08f) * 255f).toInt().coerceIn(0, 255)
            particlePaint.color = ColorUtils.setAlphaComponent(primary, alpha)
            canvas.drawCircle(particle.x, particle.y, particle.size * density * (1f + smoothedTreble * 0.4f), particlePaint)
        }
    }

    private fun updateWaveform(state: VisualizerState, dt: Float) {
        val waveform = state.waveform
        if (waveform.isEmpty()) return
        val amount = 1f - exp(-dt * 13f)
        for (i in 0 until WAVE_POINTS) {
            val raw = waveform[i * waveform.size / WAVE_POINTS] * (1.6f - state.rmsLoudness.coerceIn(0f, 1f) * 0.6f)
            smoothedWaveform[i] = lerp(smoothedWaveform[i], raw, amount)
        }
    }

    private fun buildSmoothPath() {
        wavePath.reset()
        wavePath.moveTo(pointX[0], pointY[0])
        for (i in 0 until WAVE_POINTS - 1) {
            val p0 = if (i == 0) 0 else i - 1
            val p2 = i + 1
            val p3 = minOf(i + 2, WAVE_POINTS - 1)
            wavePath.cubicTo(
                pointX[i] + (pointX[p2] - pointX[p0]) / 6f,
                pointY[i] + (pointY[p2] - pointY[p0]) / 6f,
                pointX[p2] - (pointX[p3] - pointX[i]) / 6f,
                pointY[p2] - (pointY[p3] - pointY[i]) / 6f,
                pointX[p2],
                pointY[p2]
            )
        }
    }

    private fun ensureParticles(width: Float, height: Float) {
        if (particleWidth == width && particleHeight == height) return
        for (particle in particles) {
            particle.x = Random.nextFloat() * width
            particle.y = Random.nextFloat() * height
            particle.vx = (Random.nextFloat() - 0.5f) * width * 0.022f
            particle.vy = (Random.nextFloat() - 0.5f) * height * 0.022f
            particle.size = Random.nextFloat() * 2.5f + 1f
            particle.phase = Random.nextFloat() * TWO_PI
        }
        particleWidth = width
        particleHeight = height
    }

    private fun ensureDashEffects(density: Float) {
        if (cachedDensity == density) return
        innerDash = DashPathEffect(floatArrayOf(12f * density, 7f * density, 2f * density, 7f * density), 0f)
        outerDash = DashPathEffect(floatArrayOf(3f * density, 10f * density), 0f)
        cachedDensity = density
    }

    private fun ensureGradients(cx: Float, cy: Float, radius: Float, primary: Int, secondary: Int) {
        if (abs(radius - gradientRadius) < 2f && cx == gradientCx && cy == gradientCy && primary == gradientPrimary && secondary == gradientSecondary) return
        cachedGradient = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(ColorUtils.setAlphaComponent(Color.White.toArgb(), 190), ColorUtils.setAlphaComponent(secondary, 95), ColorUtils.setAlphaComponent(primary, 35), Color.Transparent.toArgb()),
            floatArrayOf(0f, 0.16f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        cachedSweep = SweepGradient(cx, cy, intArrayOf(primary, secondary, primary), SWEEP_STOPS)
        gradientRadius = radius
        gradientCx = cx
        gradientCy = cy
        gradientPrimary = primary
        gradientSecondary = secondary
    }

    private fun setArc(cx: Float, cy: Float, radius: Float) {
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
    }

    private fun lerp(start: Float, end: Float, amount: Float) = start + (end - start) * amount

    private fun blend(base: Int, theme: Int, amount: Float) = ColorUtils.blendARGB(base, theme, amount)

    companion object {
        private const val WAVE_POINTS = 128
        private const val RADIAL_BARS = 96
        private const val MAX_PARTICLES = 16
        private const val TWO_PI = (PI * 2.0).toFloat()
        private val DEFAULT_CYAN = Color(0xFF88D8E0).toArgb()
        private val DEFAULT_VIOLET = Color(0xFFBCA9D1).toArgb()
        private val SWEEP_STOPS = floatArrayOf(0f, 0.5f, 1f)

        private fun strokePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
}
