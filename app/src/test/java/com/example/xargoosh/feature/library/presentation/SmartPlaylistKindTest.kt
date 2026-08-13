package com.example.xargoosh.feature.library.presentation

import com.example.xargoosh.domain.models.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlaylistKindTest {
    @Test
    fun recentlyAdded_ordersByDateThenTitleAndLimitsTo50() {
        val tracks = (0 until 55).map { index ->
            track(id = index.toString(), title = "Track ${index.toString().padStart(2, '0')}", dateAdded = index.toLong())
        } + listOf(
            track(id = "tie-b", title = "beta", dateAdded = 100),
            track(id = "tie-a", title = "Alpha", dateAdded = 100)
        )

        val result = SmartPlaylistKind.RECENTLY_ADDED.selectTracks(tracks)

        assertEquals(50, result.size)
        assertEquals(listOf("Alpha", "beta"), result.take(2).map { it.title })
        assertEquals((54 downTo 7).map(Int::toString), result.drop(2).map { it.id })
    }

    @Test
    fun mostPlayed_excludesUnplayedOrdersByCountThenTitleAndLimitsTo50() {
        val played = (0 until 55).map { index ->
            track(
                id = index.toString(),
                title = "Track ${index.toString().padStart(2, '0')}",
                playCount = 55 - index
            )
        }
        val ties = listOf(
            track(id = "tie-b", title = "beta", playCount = 100),
            track(id = "tie-a", title = "Alpha", playCount = 100)
        )
        val result = SmartPlaylistKind.MOST_PLAYED.selectTracks(
            played + ties + track(id = "never", title = "Never", playCount = 0)
        )

        assertEquals(50, result.size)
        assertEquals(listOf("Alpha", "beta"), result.take(2).map { it.title })
        assertTrue(result.none { it.playCount == 0 })
        assertEquals((0..47).map(Int::toString), result.drop(2).map { it.id })
    }

    @Test
    fun neverPlayed_includesEveryUnplayedTrackAndOrdersByDateThenTitle() {
        val unplayed = (0 until 60).map { index ->
            track(id = index.toString(), title = "Track $index", dateAdded = index.toLong())
        }
        val result = SmartPlaylistKind.NEVER_PLAYED.selectTracks(
            unplayed +
                track(id = "tie-b", title = "beta", dateAdded = 100) +
                track(id = "tie-a", title = "Alpha", dateAdded = 100) +
                track(id = "played", title = "Played", dateAdded = 200, playCount = 1)
        )

        assertEquals(62, result.size)
        assertEquals(listOf("Alpha", "beta"), result.take(2).map { it.title })
        assertTrue(result.none { it.id == "played" })
        assertEquals((59 downTo 0).map(Int::toString), result.drop(2).map { it.id })
    }

    @Test
    fun titleTiesUseStableNonLocaleSensitiveFallbacks() {
        val tracks = listOf(
            track(id = "z", title = "same", dateAdded = 1),
            track(id = "a", title = "same", dateAdded = 1),
            track(id = "upper", title = "Same", dateAdded = 1)
        )

        assertEquals(
            listOf("upper", "a", "z"),
            SmartPlaylistKind.RECENTLY_ADDED.selectTracks(tracks).map { it.id }
        )
    }

    private fun track(
        id: String,
        title: String,
        dateAdded: Long = 0,
        playCount: Int = 0
    ) = MusicTrack(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 1,
        uri = id,
        dateAdded = dateAdded,
        playCount = playCount
    )
}
