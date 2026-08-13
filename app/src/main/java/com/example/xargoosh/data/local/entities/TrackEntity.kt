package com.example.xargoosh.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val dateAdded: Long,
    val genre: String?,
    val playCount: Int = 0,
    val isLiked: Boolean = false,
    val folderPath: String?,
    val filePath: String? = null,
    val replayGainTrackDb: Float? = null,
    val replayGainTrackPeak: Float? = null,
    val replayGainAlbumDb: Float? = null,
    val replayGainAlbumPeak: Float? = null
)
