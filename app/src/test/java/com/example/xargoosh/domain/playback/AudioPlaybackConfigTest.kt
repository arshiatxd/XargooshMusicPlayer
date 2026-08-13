package com.example.xargoosh.domain.playback

import com.example.xargoosh.data.local.ReplayGainMetadataReader
import com.example.xargoosh.domain.models.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AudioPlaybackConfigTest {
    @Test
    fun `invalid stored values are sanitized to defaults`() {
        val config = AudioPlaybackConfig.sanitized("CROSSFADE", 1234, true, "LOUD")

        assertEquals(TransitionMode.AUTOMATIC_GAPLESS, config.transitionMode)
        assertEquals(1500, config.fadeThroughDurationMs)
        assertEquals(ReplayGainMode.OFF, config.replayGainMode)
    }

    @Test
    fun `supported values survive sanitization`() {
        val config = AudioPlaybackConfig.sanitized("FADE_THROUGH", 2000, false, "ALBUM")

        assertEquals(TransitionMode.FADE_THROUGH, config.transitionMode)
        assertEquals(2000, config.fadeThroughDurationMs)
        assertEquals(false, config.fadeUserPauseResume)
        assertEquals(ReplayGainMode.ALBUM, config.replayGainMode)
    }

    @Test
    fun `replay gain selects modes fallback and clamps boosts`() {
        val track = track(trackDb = -6.0206f, trackPeak = 0.8f, albumDb = null, albumPeak = null)

        assertEquals(1f, ReplayGainNormalizer.factor(ReplayGainMode.OFF, track), 0.0001f)
        assertEquals(0.5f, ReplayGainNormalizer.factor(ReplayGainMode.TRACK, track), 0.001f)
        assertEquals(0.5f, ReplayGainNormalizer.factor(ReplayGainMode.ALBUM, track), 0.001f)
        assertEquals(1f, ReplayGainNormalizer.factor(ReplayGainMode.TRACK, track(trackDb = 5f)), 0.0001f)
        assertEquals(
            0.25f,
            ReplayGainNormalizer.factor(
                ReplayGainMode.ALBUM,
                track(trackDb = -6f, trackPeak = 4f, albumDb = 0f)
            ),
            0.0001f
        )
    }

    @Test
    fun `replay gain peak prevents clipping and malformed factors are ignored`() {
        assertEquals(
            0.5f,
            ReplayGainNormalizer.factor(ReplayGainMode.TRACK, track(trackDb = 0f, trackPeak = 2f)),
            0.0001f
        )
        assertEquals(
            1f,
            ReplayGainNormalizer.factor(ReplayGainMode.TRACK, track(trackDb = Float.NaN)),
            0.0001f
        )
    }

    @Test
    fun `volume factors compose and sanitize`() {
        var applied = -1f
        val coordinator = PlaybackVolumeCoordinator { applied = it }
        coordinator.replayGainFactor = 0.5f
        coordinator.transitionEnvelope = 0.4f
        coordinator.pauseEnvelope = 0.5f

        assertEquals(0.1f, coordinator.composedFactor, 0.0001f)
        assertEquals(0.1f, applied, 0.0001f)
        coordinator.pauseEnvelope = Float.NaN
        assertEquals(0.2f, applied, 0.0001f)
    }

    @Test
    fun `fade envelope calculations are bounded`() {
        assertEquals(1f, PlaybackEnvelopes.outgoing(2000, 1500), 0f)
        assertEquals(0.5f, PlaybackEnvelopes.outgoing(750, 1500), 0.0001f)
        assertEquals(0f, PlaybackEnvelopes.outgoing(0, 1500), 0f)
        assertEquals(0.5f, PlaybackEnvelopes.interpolate(0f, 1f, 150, 300), 0.0001f)
    }

    @Test
    fun `ReplayGain strings parse conservatively`() {
        assertEquals(-7.25f, ReplayGainMetadataReader.parseGainDb(" -7.25 dB ")!!, 0f)
        assertEquals(0.98f, ReplayGainMetadataReader.parsePeak("0.98")!!, 0f)
        assertNull(ReplayGainMetadataReader.parseGainDb("loud"))
        assertNull(ReplayGainMetadataReader.parseGainDb("NaN dB"))
        assertNull(ReplayGainMetadataReader.parsePeak("0"))
        assertNull(ReplayGainMetadataReader.parsePeak("Infinity"))
    }

    @Test
    fun `ReplayGain keys use locale independent normalization`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(
                "REPLAYGAIN_TRACK_GAIN",
                ReplayGainMetadataReader.normalizeTagKey("replaygain_track_gain")
            )
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun track(
        trackDb: Float? = null,
        trackPeak: Float? = null,
        albumDb: Float? = null,
        albumPeak: Float? = null
    ) = MusicTrack(
        id = "id",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 1000,
        uri = "file:///track.mp3",
        dateAdded = 0,
        replayGainTrackDb = trackDb,
        replayGainTrackPeak = trackPeak,
        replayGainAlbumDb = albumDb,
        replayGainAlbumPeak = albumPeak
    )
}
