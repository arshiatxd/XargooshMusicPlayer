package com.example.xargoosh.domain.visualizer

data class VisualizerState(
    val isPlaying: Boolean = false,
    val fftAvailable: Boolean = false,

    val smoothedBands: FloatArray = FloatArray(256),

    val waveform: FloatArray = FloatArray(1024),
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val trebleEnergy: Float = 0f,
    val rmsLoudness: Float = 0f,
    val isOnBeat: Boolean = false,
    val bpm: Float = 120f,
    val reduceMotion: Boolean = false,
    val quality: QualityLevel = QualityLevel.HIGH,

    val animationSpeed: Float = 1.0f,
    val intensity: Float = 0.7f,
    val glowIntensity: Float = 0.6f,
    val particleDensity: Float = 0.5f,
    val waveThickness: Float = 0.5f,
    val beatSensitivity: Float = 1.2f,

    val timestampNanos: Long = 0L,
    val animationTimeNanos: Long = 0L,
    val palette: VisualizerPalette = VisualizerPalette.DEFAULT
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerState) return false
        return isPlaying == other.isPlaying
            && fftAvailable == other.fftAvailable
            && bassEnergy == other.bassEnergy
            && midEnergy == other.midEnergy
            && trebleEnergy == other.trebleEnergy
            && rmsLoudness == other.rmsLoudness
            && isOnBeat == other.isOnBeat
            && bpm == other.bpm
            && reduceMotion == other.reduceMotion
            && quality == other.quality
            && animationSpeed == other.animationSpeed
            && intensity == other.intensity
            && glowIntensity == other.glowIntensity
            && particleDensity == other.particleDensity
            && waveThickness == other.waveThickness
            && beatSensitivity == other.beatSensitivity
            && timestampNanos == other.timestampNanos
            && animationTimeNanos == other.animationTimeNanos
            && palette == other.palette
            && smoothedBands.contentEquals(other.smoothedBands)
            && waveform.contentEquals(other.waveform)
    }

    override fun hashCode(): Int {
        var result = isPlaying.hashCode()
        result = 31 * result + fftAvailable.hashCode()
        result = 31 * result + timestampNanos.hashCode()
        result = 31 * result + animationTimeNanos.hashCode()
        result = 31 * result + smoothedBands.contentHashCode()
        result = 31 * result + waveform.contentHashCode()
        result = 31 * result + bassEnergy.hashCode()
        result = 31 * result + quality.hashCode()
        return result
    }

    companion object {
        val IDLE = VisualizerState()
    }
}
