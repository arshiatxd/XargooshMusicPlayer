package com.example.xargoosh.core.visualizer.effects

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import com.example.xargoosh.presentation.components.AudioThumbnail

@Composable
fun LyricsVisualizerBg(
    albumArtUri: String?,
    visualizerState: VisualizerState,
    style: VisualizerStyle,
    blurStrength: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLyricsMode: Boolean = true,
    showVisualizer: Boolean = true,
    renderSize: Float = 1f,
    useLightContrast: Boolean = false
) {
    if (!enabled) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        

        if (isLyricsMode) {
            AudioThumbnail(
                uri = albumArtUri ?: "",
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(radius = (blurStrength * 40f + 10f).dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                        } else {
                            Modifier.alpha(0.5f)
                        }
                    ),
                contentScale = ContentScale.Crop
            )
        }


        val vignette = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                if (useLightContrast) Color.White.copy(alpha = if (isLyricsMode) 0.82f else 0.55f)
                else Color.Black.copy(alpha = if (isLyricsMode) 0.85f else 0.5f)
            ),
            radius = 1200f
        )
        Box(modifier = Modifier.fillMaxSize().background(vignette))


        val atmosGradient = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4DD0E1).copy(alpha = 0.05f),
                Color(0xFFB39DDB).copy(alpha = 0.05f),
                Color.Transparent
            ),
            radius = 800f
        )
        Box(modifier = Modifier.fillMaxSize().background(atmosGradient))


        val vizAlpha = if (isLyricsMode) 0.4f else 1.0f
        if (showVisualizer) {
            VisualizerCanvas(
                state = visualizerState,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(vizAlpha),
                style = style,
                renderSize = renderSize
            )
        }


        if (isLyricsMode) {
            val textContrastOverlay = Brush.verticalGradient(
                colors = if (useLightContrast) listOf(
                    Color.White.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.82f)
                ) else listOf(
                    Color.Black.copy(alpha = 0.6f),
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.8f)
                )
            )
            Box(modifier = Modifier.fillMaxSize().background(textContrastOverlay))
        }
    }
}


