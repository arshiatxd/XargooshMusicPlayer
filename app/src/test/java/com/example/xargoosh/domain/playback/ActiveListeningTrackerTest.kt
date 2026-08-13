package com.example.xargoosh.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveListeningTrackerTest {
    private val tracker = ActiveListeningTracker()

    @Test
    fun restoreOrPreparingWhilePausedDoesNotCount() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, false, 0L)

        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, false, 90_000L))
    }

    @Test
    fun pausedTimeDoesNotAccumulate() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 0L)

        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, false, 10_000L))
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, false, 100_000L))
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, true, 100_000L))
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, true, 119_999L))
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 60_000L, true, 120_000L)
        )
    }

    @Test
    fun returnsUriPayloadExactlyOnce() {
        tracker.startSession("queue-1", "content://track/1", 40_000L, true, 1_000L)

        assertNull(tracker.update("queue-1", "content://track/1", 40_000L, true, 20_999L))
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 40_000L, true, 21_000L)
        )
        assertNull(tracker.update("queue-1", "content://track/1", 40_000L, true, 60_000L))
    }

    @Test
    fun qualifiedSessionWaitsForUriBeforeEmission() {
        tracker.startSession("queue-1", null, 40_000L, true, 0L)

        assertNull(tracker.update("queue-1", null, 40_000L, true, 20_000L))
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 40_000L, true, 20_001L)
        )
        assertNull(tracker.update("queue-1", "content://track/1", 40_000L, true, 40_000L))
    }

    @Test
    fun unknownOrNonpositiveDurationUsesThirtySeconds() {
        listOf(-1L, 0L).forEachIndexed { index, duration ->
            val id = "queue-$index"
            val uri = "content://track/$index"
            tracker.startSession(id, uri, duration, true, 0L)
            assertNull(tracker.update(id, uri, duration, true, 29_999L))
            assertEquals(uri, tracker.update(id, uri, duration, true, 30_000L))
        }
    }

    @Test
    fun transitionResetsEligibility() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 0L)
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, true, 29_000L))

        tracker.startSession("queue-2", "content://track/2", 60_000L, true, 29_000L)
        assertNull(tracker.update("queue-2", "content://track/2", 60_000L, true, 30_000L))
        assertEquals(
            "content://track/2",
            tracker.update("queue-2", "content://track/2", 60_000L, true, 59_000L)
        )
    }

    @Test
    fun trueSameIdRepeatResetsEligibility() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 0L)
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 60_000L, true, 30_000L)
        )

        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 30_000L)
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, true, 59_999L))
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 60_000L, true, 60_000L)
        )
    }

    @Test
    fun sameIdPlaylistUpdatePreservesEmittedState() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 0L)
        assertEquals(
            "content://track/1",
            tracker.update("queue-1", "content://track/1", 60_000L, true, 30_000L)
        )

        assertNull(tracker.update("queue-1", "content://track/updated", 60_000L, true, 30_001L))
        assertNull(tracker.update("queue-1", "content://track/updated", 60_000L, true, 90_000L))
    }

    @Test
    fun sameIdPlaylistUpdatePreservesAccumulatedTimeAndUsesLatestPayload() {
        tracker.startSession("queue-1", "content://track/1", 60_000L, true, 0L)
        assertNull(tracker.update("queue-1", "content://track/1", 60_000L, true, 20_000L))

        assertNull(tracker.update("queue-1", "content://track/updated", 60_000L, true, 20_001L))
        assertEquals(
            "content://track/updated",
            tracker.update("queue-1", "content://track/updated", 60_000L, true, 30_000L)
        )
    }
}
