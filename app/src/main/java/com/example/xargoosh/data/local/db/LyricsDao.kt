package com.example.xargoosh.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xargoosh.data.local.entities.LyricsEntity

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackUri = :uri LIMIT 1")
    suspend fun getLyrics(uri: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackUri = :uri")
    suspend fun deleteLyrics(uri: String)

    @Query("DELETE FROM lyrics")
    suspend fun clearAll()
}
