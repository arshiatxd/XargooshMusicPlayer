package com.example.xargoosh.domain.visualizer

import androidx.compose.ui.graphics.Color

data class VisualizerPalette(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val background: Color,
    val accent: Color
) {
    companion object {
        val DEFAULT = VisualizerPalette(
            primary = Color(0xFF00CFFF),
            secondary = Color(0xFF7B2FBE),
            glow = Color(0xFF00CFFF),
            background = Color.Black,
            accent = Color.White
        )
    }
}
