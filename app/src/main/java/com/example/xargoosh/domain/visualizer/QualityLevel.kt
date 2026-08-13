package com.example.xargoosh.domain.visualizer

enum class QualityLevel(
    val targetFps: Int,
    val maxParticles: Int,
    val fftResolution: Int, 
    val glowPasses: Int,
    val shadowEnabled: Boolean,
    val blurPasses: Int
) {
    ULTRA(120, 200, 512, 3, true, 3),
    HIGH(90,  150, 256, 2, true, 2),
    MEDIUM(60, 100, 256, 1, true, 1),
    LOW(60,    50, 128, 1, false, 1),
    SAVER(30,  25,  64, 0, false, 0),
    ULTRA_SAVER(24, 10, 64, 0, false, 0);

    val targetFrameMs: Long get() = 1000L / targetFps
}
