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

class NeuralNetworkRenderer : VisualizerRenderer {
    override val id = VisualizerStyle.NEURAL_NETWORK
    override val displayNameRes = com.example.xargoosh.R.string.style_neural_network
    override val supportsFFT = true
    override val supportsBlurredBackground = true
    override val supportsAlbumArtwork = false

    private val layers = intArrayOf(4, 8, 4)
    private val totalNodes = layers.sum()
    private val positions = FloatArray(totalNodes * 2)

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var initialized = false
    private var pulseTime = 0f

    override fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        if (!initialized || width == 0f || height == 0f) {
            initNodes(width, height)
            initialized = true
        }

        if (state.isOnBeat) {
            pulseTime = 1f
        }

        val delta = if (state.reduceMotion) 0f else 0.05f
        if (pulseTime > 0f) {
            pulseTime -= delta
        }

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            var nodeOffset1 = 0
            var connectionIdx = 0

            for (l in 0 until layers.size - 1) {
                val nodesInLayer1 = layers[l]
                val nodesInLayer2 = layers[l + 1]
                val nodeOffset2 = nodeOffset1 + nodesInLayer1

                for (n1 in 0 until nodesInLayer1) {
                    val idx1 = nodeOffset1 + n1
                    val x1 = positions[idx1 * 2]
                    val y1 = positions[idx1 * 2 + 1]

                    for (n2 in 0 until nodesInLayer2) {
                        val idx2 = nodeOffset2 + n2
                        val x2 = positions[idx2 * 2]
                        val y2 = positions[idx2 * 2 + 1]

                        val bandVal = state.smoothedBands[(connectionIdx * 2).coerceIn(0, 255)]
                        val alpha = (bandVal * 0.8f).coerceIn(0.1f, 1f)

                        linePaint.color = palette.secondary.copy(alpha = alpha).toArgb()
                        nativeCanvas.drawLine(x1, y1, x2, y2, linePaint)
                        connectionIdx++
                    }
                }
                nodeOffset1 += nodesInLayer1
            }

            var nodeIdx = 0
            for (l in layers.indices) {
                val nodesInLayer = layers[l]
                for (n in 0 until nodesInLayer) {
                    val x = positions[nodeIdx * 2]
                    val y = positions[nodeIdx * 2 + 1]

                    val bandVal = state.smoothedBands[(nodeIdx * 10).coerceIn(0, 255)]
                    val size = 5f + (bandVal * 15f)

                    val pColor = if (bandVal > 0.5f) palette.glow else palette.primary
                    nodePaint.color = pColor.toArgb()

                    nativeCanvas.drawCircle(x, y, size, nodePaint)

                    if (pulseTime > 0f) {
                        val layerProgress = l / (layers.size - 1).toFloat()
                        val pulsePos = 1f - pulseTime
                        if (kotlin.math.abs(layerProgress - pulsePos) < 0.2f) {
                            nodePaint.color = palette.accent.copy(alpha = pulseTime).toArgb()
                            nativeCanvas.drawCircle(x, y, size + 10f, nodePaint)
                        }
                    }
                    nodeIdx++
                }
            }
        }
    }

    private fun initNodes(width: Float, height: Float) {
        val spacingX = width / (layers.size + 1)
        var idx = 0

        for (l in layers.indices) {
            val nodes = layers[l]
            val spacingY = height / (nodes + 1)
            val x = spacingX * (l + 1)

            for (n in 0 until nodes) {
                val y = spacingY * (n + 1)
                positions[idx * 2] = x
                positions[idx * 2 + 1] = y
                idx++
            }
        }
    }
}

