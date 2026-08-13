package com.example.xargoosh.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackUri: String,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val timestamp: Long = System.currentTimeMillis()
)
