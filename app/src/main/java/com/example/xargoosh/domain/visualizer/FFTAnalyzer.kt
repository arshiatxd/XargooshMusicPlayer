package com.example.xargoosh.domain.visualizer

class FFTAnalyzer(private val binCount: Int = 256) {

    val smoothedBands = FloatArray(binCount)
    private val rawMagnitudes = FloatArray(binCount)
    private val smoothingFactor = 0.3f  

    var bassEnergy = 0f; private set
    var midEnergy = 0f; private set
    var trebleEnergy = 0f; private set

    fun processFFT(rawFft: ByteArray): Boolean {
        if (rawFft.size < 4) return false
        val half = rawFft.size / 2

        for (i in 0 until minOf(binCount, half)) {
            val re = rawFft[2 * i].toFloat()
            val im = if (2 * i + 1 < rawFft.size) rawFft[2 * i + 1].toFloat() else 0f
            val mag = kotlin.math.sqrt(re * re + im * im) / 128f  
            rawMagnitudes[i] = mag.coerceIn(0f, 1f)
        }

        for (i in 0 until binCount) {
            smoothedBands[i] = smoothedBands[i] * (1f - smoothingFactor) + rawMagnitudes[i] * smoothingFactor
        }

        val bassEnd = (binCount * 0.10f).toInt().coerceAtLeast(1)
        val midEnd  = (binCount * 0.60f).toInt()
        var bSum = 0f; var mSum = 0f; var tSum = 0f
        for (i in 0 until bassEnd) bSum += smoothedBands[i]
        for (i in bassEnd until midEnd) mSum += smoothedBands[i]
        for (i in midEnd until binCount) tSum += smoothedBands[i]
        bassEnergy   = (bSum / bassEnd).coerceIn(0f, 1f)
        midEnergy    = (mSum / (midEnd - bassEnd).coerceAtLeast(1)).coerceIn(0f, 1f)
        trebleEnergy = (tSum / (binCount - midEnd).coerceAtLeast(1)).coerceIn(0f, 1f)
        return true
    }

    fun reset() {
        smoothedBands.fill(0f)
        rawMagnitudes.fill(0f)
        bassEnergy = 0f; midEnergy = 0f; trebleEnergy = 0f
    }
}
