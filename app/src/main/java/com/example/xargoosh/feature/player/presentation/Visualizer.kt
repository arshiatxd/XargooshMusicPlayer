package com.example.xargoosh.feature.player.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    waveform: ByteArray,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        if (waveform.isEmpty()) {
            drawLine(
                color = color,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val numBars = 32
        val barWidth = width / (numBars * 1.5f)
        val spacing = (width - (numBars * barWidth)) / numBars

        val chunk = waveform.size / numBars

        for (i in 0 until numBars) {
            var sum = 0f
            for (j in 0 until chunk) {
                val index = (i * chunk) + j
                if (index < waveform.size) {
                    val value = waveform[index].toFloat() + 128f
                    val amplitude = kotlin.math.abs(value - 128f)
                    sum += amplitude
                }
            }
            val average = (sum / chunk) * 2f 

            val barHeight = (average / 128f) * (height / 2f)
            val x = i * (barWidth + spacing) + spacing / 2f

            drawLine(
                color = color.copy(alpha = 0.8f),
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
