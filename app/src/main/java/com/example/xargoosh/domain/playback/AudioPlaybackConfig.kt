package com.example.xargoosh.domain.playback

import com.example.xargoosh.domain.models.MusicTrack
import kotlin.math.pow

enum class TransitionMode { AUTOMATIC_GAPLESS, FADE_THROUGH }

enum class ReplayGainMode { OFF, TRACK, ALBUM }

data class AudioPlaybackConfig(
    val transitionMode: TransitionMode = TransitionMode.AUTOMATIC_GAPLESS,
    val fadeThroughDurationMs: Int = DEFAULT_FADE_THROUGH_MS,
    val fadeUserPauseResume: Boolean = true,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF
) {
    companion object {
        val SUPPORTED_FADE_DURATIONS_MS = setOf(500, 1000, 1500, 2000)
        const val DEFAULT_FADE_THROUGH_MS = 1500
        const val USER_PAUSE_FADE_MS = 200L
        const val USER_RESUME_FADE_MS = 300L

        fun sanitized(
            transitionMode: String?,
            fadeThroughDurationMs: Int,
            fadeUserPauseResume: Boolean,
            replayGainMode: String?
        ) = AudioPlaybackConfig(
            transitionMode = enumValueOrDefault(transitionMode, TransitionMode.AUTOMATIC_GAPLESS),
            fadeThroughDurationMs = fadeThroughDurationMs.takeIf(SUPPORTED_FADE_DURATIONS_MS::contains)
                ?: DEFAULT_FADE_THROUGH_MS,
            fadeUserPauseResume = fadeUserPauseResume,
            replayGainMode = enumValueOrDefault(replayGainMode, ReplayGainMode.OFF)
        )

        private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}

object ReplayGainNormalizer {
    fun factor(mode: ReplayGainMode, track: MusicTrack?): Float {
        if (mode == ReplayGainMode.OFF || track == null) return 1f
        val (gainDb, peak) = when (mode) {
            ReplayGainMode.OFF -> return 1f
            ReplayGainMode.TRACK -> track.replayGainTrackDb to track.replayGainTrackPeak
            ReplayGainMode.ALBUM ->
                (track.replayGainAlbumDb ?: track.replayGainTrackDb) to
                    (track.replayGainAlbumPeak ?: track.replayGainTrackPeak)
        }
        if (gainDb == null || !gainDb.isFinite()) return 1f
        var factor = 10.0.pow(gainDb / 20.0).toFloat()
        if (!factor.isFinite() || factor <= 0f) return 1f
        if (peak != null && peak.isFinite() && peak > 0f) factor = minOf(factor, 1f / peak)
        return factor.coerceIn(0f, 1f)
    }
}

object PlaybackEnvelopes {
    fun outgoing(remainingMs: Long, fadeDurationMs: Long): Float {
        if (fadeDurationMs <= 0L || remainingMs < 0L) return 1f
        return (remainingMs.toFloat() / fadeDurationMs).coerceIn(0f, 1f)
    }

    fun interpolate(start: Float, end: Float, elapsedMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return end
        val progress = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
        return start + (end - start) * progress
    }
}

class PlaybackVolumeCoordinator(private val applyVolume: (Float) -> Unit) {
    var replayGainFactor: Float = 1f
        set(value) { field = value.sanitized(); apply() }
    var transitionEnvelope: Float = 1f
        set(value) { field = value.sanitized(); apply() }
    var pauseEnvelope: Float = 1f
        set(value) { field = value.sanitized(); apply() }

    val composedFactor: Float
        get() = (replayGainFactor * transitionEnvelope * pauseEnvelope).coerceIn(0f, 1f)

    fun apply() = applyVolume(composedFactor)

    private fun Float.sanitized() = takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
}
