package com.example.xargoosh.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xargoosh.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracksAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title DESC")
    fun getAllTracksDesc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY artist ASC")
    fun getAllTracksByArtistAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun getAllTracksByDateAddedDesc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded ASC")
    fun getAllTracksByDateAddedAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC, title COLLATE NOCASE ASC, title ASC, uri ASC LIMIT 50")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLiked = 1 ORDER BY title ASC")
    fun getLikedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE folderPath = :folderKey ORDER BY title ASC")
    fun getTracksForFolder(folderKey: String): Flow<List<TrackEntity>>

    @Query("SELECT uri FROM tracks WHERE folderPath = :folderKey")
    suspend fun getTrackUrisForFolder(folderKey: String): List<String>

    @Query("SELECT dateAdded FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun getTrackDateAdded(uri: String): Long?

    @Query("DELETE FROM tracks WHERE uri = :trackUri")
    suspend fun deleteTrack(trackUri: String)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE uri = :trackUri")
    suspend fun incrementPlayCount(trackUri: String)

    @Query("UPDATE tracks SET isLiked = :isLiked WHERE uri = :trackUri")
    suspend fun updateLikeStatus(trackUri: String, isLiked: Boolean)

    @Query("UPDATE tracks SET album = :album WHERE uri = :trackUri")
    suspend fun updateAlbumTag(trackUri: String, album: String)

    @Query("UPDATE tracks SET artist = :artist WHERE uri = :trackUri")
    suspend fun updateArtistTag(trackUri: String, artist: String)

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album WHERE uri = :trackUri")
    suspend fun updateEditedMetadata(trackUri: String, title: String, artist: String, album: String)

    @Query("SELECT uri FROM tracks")
    suspend fun getAllTrackUris(): List<String>

    @Query("SELECT uri FROM tracks WHERE uri LIKE 'content://media/%'")
    suspend fun getMediaStoreTrackUris(): List<String>

    @Query("DELETE FROM tracks WHERE uri IN (:uris)")
    suspend fun deleteTracksByUris(uris: List<String>)

    @Query("DELETE FROM tracks WHERE folderPath = :folderKey")
    suspend fun deleteTracksForFolder(folderKey: String)

    @Query("""
        UPDATE tracks SET title = :title, artist = :artist, album = :album,
        durationMs = :durationMs, albumArtUri = :albumArtUri, dateAdded = :dateAdded,
        genre = :genre, folderPath = :folderPath, filePath = :filePath,
        replayGainTrackDb = :replayGainTrackDb, replayGainTrackPeak = :replayGainTrackPeak,
        replayGainAlbumDb = :replayGainAlbumDb, replayGainAlbumPeak = :replayGainAlbumPeak
        WHERE uri = :uri
    """)
    suspend fun updateScannedMetadata(
        uri: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        albumArtUri: String?,
        dateAdded: Long,
        genre: String?,
        folderPath: String?,
        filePath: String?,
        replayGainTrackDb: Float?,
        replayGainTrackPeak: Float?,
        replayGainAlbumDb: Float?,
        replayGainAlbumPeak: Float?
    )

    @androidx.room.Transaction
    suspend fun syncTracks(scannedTracks: List<TrackEntity>) {
        val existingUris = getMediaStoreTrackUris()
        val scannedUris = scannedTracks.map { it.uri }.toSet()
        val toDelete = existingUris.filter { it !in scannedUris }

        if (toDelete.isNotEmpty()) {
            deleteTracksByUris(toDelete)
        }
        scannedTracks.forEach { track ->
            updateScannedMetadata(
                track.uri, track.title, track.artist, track.album, track.durationMs,
                track.albumArtUri, track.dateAdded, track.genre, track.folderPath, track.filePath,
                track.replayGainTrackDb, track.replayGainTrackPeak,
                track.replayGainAlbumDb, track.replayGainAlbumPeak
            )
        }
        insertTracks(scannedTracks)
    }
}

