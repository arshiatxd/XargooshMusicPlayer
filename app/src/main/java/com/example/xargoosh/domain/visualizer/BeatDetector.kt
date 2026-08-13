package com.example.xargoosh.domain.visualizer

data class BeatState(
    val isOnBeat: Boolean,
    val bpm: Float,
    val rmsLoudness: Float
)

class BeatDetector(private val sensitivity: Float = 1.2f) {
    private val energyHistory = FloatArray(48)
    private var historyIndex = 0
    private var historyCount = 0
    private var previousEnergy = 0f
    private var lastBeatNanos = 0L
    private val bpmHistory = ArrayDeque<Float>(8)

    var currentBpm = 120f
        private set

    fun analyze(
        waveform: FloatArray,
        timestampNanos: Long,
        sensitivityOverride: Float = sensitivity,
        spectralEnergy: Float? = null
    ): BeatState {
        var sumSq = 0f
        for (sample in waveform) sumSq += sample * sample
        val rms = kotlin.math.sqrt(sumSq / waveform.size.coerceAtLeast(1)).coerceIn(0f, 1f)
        val energy = (spectralEnergy ?: rms).coerceIn(0f, 1f)

        var average = 0f
        for (i in 0 until historyCount) average += energyHistory[i]
        if (historyCount > 0) average /= historyCount

        var variance = 0f
        for (i in 0 until historyCount) {
            val difference = energyHistory[i] - average
            variance += difference * difference
        }
        val deviation = if (historyCount > 1) kotlin.math.sqrt(variance / historyCount) else 0f
        val sensitivity = sensitivityOverride.coerceIn(0.7f, 2f)
        val strictness = 2.2f - sensitivity
        val threshold = average + maxOf(0.018f, deviation * (0.8f + strictness * 0.35f))
        val onset = energy - previousEnergy > maxOf(0.012f, deviation * 0.3f)
        val minimumInterval = 280_000_000L
        val warmedUp = historyCount >= 12
        val isOnBeat = warmedUp && energy > 0.045f && energy > threshold && onset &&
            (lastBeatNanos == 0L || timestampNanos - lastBeatNanos >= minimumInterval)

        if (isOnBeat) {
            if (lastBeatNanos != 0L) {
                val intervalMs = (timestampNanos - lastBeatNanos) / 1_000_000f
                var bpmSample = 60_000f / intervalMs
                while (bpmSample < 70f) bpmSample *= 2f
                while (bpmSample > 180f) bpmSample /= 2f
                if (bpmSample in 70f..180f) {
                    if (bpmHistory.size == 8) bpmHistory.removeFirst()
                    bpmHistory.addLast(bpmSample)
                    currentBpm = bpmHistory.sorted()[bpmHistory.size / 2]
                }
            }
            lastBeatNanos = timestampNanos
        }

        energyHistory[historyIndex] = energy
        historyIndex = (historyIndex + 1) % energyHistory.size
        if (historyCount < energyHistory.size) historyCount++
        previousEnergy = energy

        return BeatState(isOnBeat = isOnBeat, bpm = currentBpm, rmsLoudness = rms)
    }

    fun reset() {
        energyHistory.fill(0f)
        historyIndex = 0
        historyCount = 0
        previousEnergy = 0f
        lastBeatNanos = 0L
        bpmHistory.clear()
        currentBpm = 120f
    }
}
