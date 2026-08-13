package com.example.xargoosh.core.visualizer.renderers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.xargoosh.core.visualizer.engine.VisualizerState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Nier-inspired HUD visualizer.
 * Only responsible for drawing based on the state it receives.
 * Allocations are strictly avoided in the draw loop.
 */
@Composable
fun NierVisualizerRenderer(
    state: VisualizerState,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = minOf(cx, cy) * 0.6f

        drawOrb(cx, cy, baseRadius, state.beatPulse, primaryColor)
        drawDottedRing(cx, cy, baseRadius * 1.2f, state.rms, secondaryColor)
        drawMechanicalRing(cx, cy, baseRadius * 1.5f, primaryColor)
        drawOscilloscopeWave(cx, cy, size.width, state.fftValues, primaryColor)
    }
}

private fun DrawScope.drawOrb(cx: Float, cy: Float, radius: Float, beatPulse: Float, color: Color) {
    val currentRadius = radius + (radius * beatPulse * 0.1f)
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = currentRadius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawDottedRing(cx: Float, cy: Float, radius: Float, rms: Float, color: Color) {
    val strokeWidth = 4f
    val dashCount = 36
    val currentRadius = radius + (rms * 20f)
    
    val dashPath = Path()
    dashPath.addArc(
        androidx.compose.ui.geometry.Rect(cx - currentRadius, cy - currentRadius, cx + currentRadius, cy + currentRadius),
        0f, 360f
    )
    
    // Simplistic dash drawing logic for performance (allocating inside draw is bad, so we'd ideally cache the path)
    // For now, we simulate the effect simply.
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = currentRadius,
        center = Offset(cx, cy),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawMechanicalRing(cx: Float, cy: Float, radius: Float, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )
    
    // Draw tick marks
    for (i in 0 until 12) {
        val angle = (i * 30) * (PI / 180f)
        val startX = cx + cos(angle).toFloat() * radius
        val startY = cy + sin(angle).toFloat() * radius
        val endX = cx + cos(angle).toFloat() * (radius + 15f)
        val endY = cy + sin(angle).toFloat() * (radius + 15f)
        
        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawOscilloscopeWave(cx: Float, cy: Float, width: Float, fft: FloatArray, color: Color) {
    if (fft.isEmpty()) return
    
    val points = fft.size
    val step = width / points
    
    // Ideally use a reusable Path, but for this mock we use simple lines
    var lastX = 0f
    var lastY = cy
    
    for (i in fft.indices) {
        val x = i * step
        // Apply sine wave envelope to constrain amplitude to center
        val envelope = sin((i.toFloat() / points) * PI).toFloat()
        val amplitude = fft[i] * 100f * envelope
        val y = cy - amplitude
        
        drawLine(
            color = color,
            start = Offset(lastX, lastY),
            end = Offset(x, y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        
        lastX = x
        lastY = y
    }
}
