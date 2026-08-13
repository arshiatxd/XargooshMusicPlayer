package com.example.xargoosh.domain.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatDetectorTest {
    @Test
    fun estimatesTempoFromRealtimeBeatIntervals() {
        val detector = BeatDetector()
        val loud = FloatArray(128) { 1f }
        val quiet = FloatArray(128)

        repeat(12) { detector.analyze(quiet, 1_000_000_000L + it * 20_000_000L) }
        assertTrue(detector.analyze(loud, 2_000_000_000L).isOnBeat)
        detector.analyze(quiet, 2_200_000_000L)
        val secondBeat = detector.analyze(loud, 2_500_000_000L)

        assertTrue(secondBeat.isOnBeat)
        assertEquals(120f, secondBeat.bpm, 0.1f)
    }

    @Test
    fun doesNotEmitStartupOrSustainedEnergyBumps() {
        val detector = BeatDetector()
        val loud = FloatArray(128) { 0.8f }

        repeat(12) { index ->
            assertTrue(!detector.analyze(loud, 1_000_000_000L + index * 20_000_000L).isOnBeat)
        }
        assertTrue(!detector.analyze(loud, 1_500_000_000L).isOnBeat)
    }
}
