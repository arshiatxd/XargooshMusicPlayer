package com.example.xargoosh.domain.models

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null,
    val dateAdded: Long,
    val genre: String? = null,
    val filePath: String? = null,
    val folderPath: String? = null,
    val playCount: Int = 0,
    val replayGainTrackDb: Float? = null,
    val replayGainTrackPeak: Float? = null,
    val replayGainAlbumDb: Float? = null,
    val replayGainAlbumPeak: Float? = null
)
