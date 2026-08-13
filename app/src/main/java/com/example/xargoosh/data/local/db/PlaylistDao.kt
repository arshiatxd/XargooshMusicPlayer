package com.example.xargoosh.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.xargoosh.data.local.entities.PlaylistEntity
import com.example.xargoosh.data.local.entities.PlaylistTrackEntity
import com.example.xargoosh.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri)")
    suspend fun containsTrack(playlistId: Int, trackUri: String): Boolean

    @Query("""
        SELECT tracks.* FROM tracks 
        INNER JOIN playlist_tracks ON tracks.uri = playlist_tracks.trackUri 
        WHERE playlist_tracks.playlistId = :playlistId 
        ORDER BY playlist_tracks.position ASC
    """)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>>

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackUri: String)

    @Query("DELETE FROM playlist_tracks WHERE trackUri = :trackUri")
    suspend fun removeTrackFromAllPlaylists(trackUri: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPosition(playlistId: Int): Int
}
