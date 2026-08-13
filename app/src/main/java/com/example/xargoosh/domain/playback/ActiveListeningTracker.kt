package com.example.xargoosh.domain.playback

/** Tracks active listening time for one media-item playback session. */
internal class ActiveListeningTracker {
    private var itemId: String? = null
    private var trackUri: String? = null
    private var thresholdMs = DEFAULT_THRESHOLD_MS
    private var activeTimeMs = 0L
    private var activeSinceMs: Long? = null
    private var emitted = false

    fun startSession(
        itemId: String?,
        trackUri: String?,
        durationMs: Long,
        isPlaying: Boolean,
        elapsedRealtimeMs: Long
    ) {
        this.itemId = itemId
        this.trackUri = trackUri
        thresholdMs = thresholdFor(durationMs)
        activeTimeMs = 0L
        activeSinceMs = elapsedRealtimeMs.takeIf { isPlaying && itemId != null }
        emitted = false
    }

    fun update(
        itemId: String?,
        trackUri: String?,
        durationMs: Long,
        isPlaying: Boolean,
        elapsedRealtimeMs: Long
    ): String? {
        if (itemId != this.itemId) {
            startSession(itemId, trackUri, durationMs, isPlaying, elapsedRealtimeMs)
            return null
        }
        this.trackUri = trackUri
        if (itemId == null || emitted) return null

        thresholdMs = thresholdFor(durationMs)
        activeSinceMs?.let { activeSince ->
            activeTimeMs += (elapsedRealtimeMs - activeSince).coerceAtLeast(0L)
        }
        activeSinceMs = elapsedRealtimeMs.takeIf { isPlaying }

        return this.trackUri
            ?.takeIf { activeTimeMs >= thresholdMs }
            ?.also { emitted = true }
    }

    private fun thresholdFor(durationMs: Long): Long {
        if (durationMs <= 0L) return DEFAULT_THRESHOLD_MS
        val halfDurationMs = durationMs / 2 + durationMs % 2
        return minOf(DEFAULT_THRESHOLD_MS, halfDurationMs)
    }

    private companion object {
        const val DEFAULT_THRESHOLD_MS = 30_000L
    }
}
