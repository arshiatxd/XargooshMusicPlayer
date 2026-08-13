package com.example.xargoosh.core.visualizer.effects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import com.example.xargoosh.feature.player.presentation.PlayerViewModel

@Stable
@Composable
fun VisualizerCanvas(
    state: VisualizerState,
    modifier: Modifier,
    style: VisualizerStyle,
    renderSize: Float = 1f
) {
    val registry = remember { RendererRegistry() }

    DisposableEffect(Unit) {
        onDispose {
            registry.clear()
        }
    }

    key(style) {
        Canvas(modifier = modifier.graphicsLayer {
            scaleX = renderSize.coerceIn(0.6f, 2f)
            scaleY = renderSize.coerceIn(0.6f, 2f)
        }) {
            if (!state.isPlaying && state.rmsLoudness < 0.01f) {

                drawLine(
                    color = state.palette.primary.copy(alpha = 0.5f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f
                )
            } else {
                registry.get(style).render(this, state, state.palette)
            }
        }
    }
}

@Composable
fun VisualizerCanvasWithLifecycle(
    viewModel: PlayerViewModel,
    modifier: Modifier
) {
    val state by viewModel.visualizerState.collectAsStateWithLifecycle()
    val style by viewModel.visualizerSettings.style.collectAsStateWithLifecycle(initialValue = VisualizerStyle.NIER_WAVE)
    val renderSize by viewModel.visualizerSettings.renderSize.collectAsStateWithLifecycle(initialValue = 1f)

    VisualizerCanvas(
        state = state,
        modifier = modifier,
        style = style,
        renderSize = renderSize
    )
}
