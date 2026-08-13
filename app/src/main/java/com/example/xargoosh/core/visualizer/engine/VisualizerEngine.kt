package com.example.xargoosh.core.visualizer.engine

interface VisualizerEngine {
    fun start()
    fun stop()
    fun updateState(audioData: ByteArray)
    val state: VisualizerState
}

data class VisualizerState(
    val rms: Float = 0f,
    val fftValues: FloatArray = FloatArray(0),
    val beatPulse: Float = 0f,
    val isActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerState
        if (rms != other.rms) return false
        if (!fftValues.contentEquals(other.fftValues)) return false
        if (beatPulse != other.beatPulse) return false
        if (isActive != other.isActive) return false
        return true
    }

    override fun hashCode(): Int {
        var result = rms.hashCode()
        result = 31 * result + fftValues.contentHashCode()
        result = 31 * result + beatPulse.hashCode()
        result = 31 * result + isActive.hashCode()
        return result
    }
}
