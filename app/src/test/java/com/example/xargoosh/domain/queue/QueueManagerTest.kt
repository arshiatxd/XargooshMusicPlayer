package com.example.xargoosh.domain.queue

import com.example.xargoosh.domain.models.MusicTrack
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QueueManagerTest {
    @Test
    fun playNextMovesExistingTrackAfterCurrent() = runTest {
        val tracks = listOf(track("a"), track("b"), track("c"))
        val (queue, _) = QueueManager.setQueue(tracks, 0, false)

        val result = QueueManager.putNext(tracks[2], queue[0].id, shuffleEnabled = false)

        assertEquals(listOf("a", "c", "b"), result.queue.map { it.track.id })
        assertFalse(result.wasAdded)
    }

    @Test
    fun playNextDoesNotDuplicateCurrentTrack() = runTest {
        val tracks = listOf(track("a"), track("b"))
        val (queue, _) = QueueManager.setQueue(tracks, 0, false)

        val result = QueueManager.putNext(tracks[0], queue[0].id, shuffleEnabled = false)

        assertFalse(result.changed)
        assertEquals(listOf("a", "b"), result.queue.map { it.track.id })
    }

    @Test
    fun disablingShuffleRestoresOriginalOrder() = runTest {
        val tracks = listOf(track("a"), track("b"), track("c"))
        val (queue, _) = QueueManager.setQueue(tracks, 0, false)
        QueueManager.applyPlaybackOrder(listOf(queue[2].id, queue[0].id, queue[1].id))

        val restored = QueueManager.restoreNormalOrder()

        assertEquals(listOf("a", "b", "c"), restored.map { it.track.id })
    }

    private fun track(id: String) = MusicTrack(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 1_000L,
        uri = "content://test/$id",
        dateAdded = 0L
    )
}
