package com.example.xargoosh.domain.visualizer

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.annotation.StringRes

interface VisualizerRenderer {
    val id: VisualizerStyle
    @get:StringRes val displayNameRes: Int

    val supportsFFT: Boolean

    val supportsBlurredBackground: Boolean

    val supportsAlbumArtwork: Boolean

    fun render(drawScope: DrawScope, state: VisualizerState, palette: VisualizerPalette)

    fun onDeactivated() {}

    fun onActivated() {}
}
