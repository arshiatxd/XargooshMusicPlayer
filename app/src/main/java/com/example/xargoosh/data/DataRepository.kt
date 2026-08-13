package com.example.xargoosh.data

import android.content.Context
import com.example.xargoosh.data.local.MediaScanner
import com.example.xargoosh.data.local.db.AppDatabase
import com.example.xargoosh.data.local.entities.TrackEntity
import com.example.xargoosh.data.local.entities.FolderEntity
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.domain.models.MusicFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.xargoosh.R

interface DataRepository {
    suspend fun syncLocalMedia()
    suspend fun deleteTrack(trackUri: String)
    fun getAllTracksAsc(): Flow<List<MusicTrack>>
    fun getAllTracksDesc(): Flow<List<MusicTrack>>
    fun getAllTracksByArtistAsc(): Flow<List<MusicTrack>>
    fun getAllTracksByDateAddedDesc(): Flow<List<MusicTrack>>
    fun getAllTracksByDateAddedAsc(): Flow<List<MusicTrack>>
    fun getMostPlayedTracks(): Flow<List<MusicTrack>>
    fun getLikedTracks(): Flow<List<MusicTrack>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Int)

    fun getAllPlaylists(): Flow<List<com.example.xargoosh.domain.models.Playlist>>
    suspend fun getPlaylistsOnce(): List<com.example.xargoosh.domain.models.Playlist>
    fun getTracksForPlaylist(playlistId: Int): Flow<List<MusicTrack>>
    suspend fun addTrackToPlaylist(playlistId: Int, trackUri: String)
    suspend fun addTracksToPlaylist(playlistId: Int, trackUris: Collection<String>)
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackUri: String)
    suspend fun scanSafFolder(treeUri: android.net.Uri)
    fun getTracksForFolder(folderKey: String): Flow<List<MusicTrack>>

    fun getFolders(): Flow<List<MusicFolder>>
    suspend fun deleteFolder(folderId: Int)


    suspend fun getOrCreateFavoritesPlaylist(): Int
    fun isFavorite(trackUri: String): Flow<Boolean>
    suspend fun toggleFavorite(trackUri: String)
    suspend fun incrementPlayCount(trackUri: String)
}

class DataRepositoryImpl(
    private val context: Context,
    private val mediaScanner: MediaScanner,
    private val db: AppDatabase
) : DataRepository {

    private val trackDao = db.trackDao()

    private fun TrackEntity.toMusicTrack() = MusicTrack(
        id = uri,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        dateAdded = dateAdded,
        albumArtUri = albumArtUri,
        genre = genre,
        folderPath = folderPath,
        filePath = filePath,
        playCount = playCount,
        replayGainTrackDb = replayGainTrackDb,
        replayGainTrackPeak = replayGainTrackPeak,
        replayGainAlbumDb = replayGainAlbumDb,
        replayGainAlbumPeak = replayGainAlbumPeak
    )

    override suspend fun incrementPlayCount(trackUri: String) = trackDao.incrementPlayCount(trackUri)

    private fun scanDirectoryTree(
        root: DocumentFile,
        sourceKey: String,
        newTracks: MutableList<MusicTrack>
    ) {
        val pending = java.util.ArrayDeque<Pair<DocumentFile, Int>>()
        val visited = mutableSetOf<String>()
        pending.add(root to 0)
        var visitedItems = 0
        while (pending.isNotEmpty()) {
            val (directory, depth) = pending.removeLast()
            val directoryKey = directory.uri.toString()
            if (!visited.add(directoryKey)) continue
            if (depth > 64) throw java.io.IOException(context.getString(R.string.folder_nested_too_deep))
            for (file in directory.listFiles()) {
                visitedItems++
                if (visitedItems > 50_000) throw java.io.IOException(context.getString(R.string.folder_too_many_items))
                if (file.isDirectory) {
                    pending.add(file to depth + 1)
                    continue
                }
                val mime = file.type ?: ""
                val name = file.name?.lowercase() ?: ""
                if (!(mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".flac") || name.endsWith(".wav"))) continue
                var title = file.name ?: context.getString(R.string.unknown_title)
                var artist = context.getString(R.string.unknown_artist)
                var album = directory.name ?: context.getString(R.string.unknown_folder)
                var duration = 0L
                var genre: String? = null

                runCatching {
                    context.contentResolver.openFileDescriptor(file.uri, "r")?.use { pfd ->
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(pfd.fileDescriptor)
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { title = it }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { artist = it }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { album = it }
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { duration = it }
                            genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)
                        } finally {
                            retriever.release()
                        }
                    }
                }

                newTracks.add(
                    MusicTrack(
                        id = file.uri.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = file.uri.toString(),
                        albumArtUri = null,
                        dateAdded = file.lastModified().takeIf { it > 0L }?.div(1000)
                            ?: System.currentTimeMillis() / 1000,
                        genre = genre,
                        folderPath = sourceKey,
                        filePath = null
                    )
                )
            }
        }
    }

    override suspend fun scanSafFolder(treeUri: android.net.Uri) {
        withContext(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
            val newTracks = mutableListOf<MusicTrack>()
            val sourceKey = treeUri.toString()
            scanDirectoryTree(docFile, sourceKey, newTracks)

            val existingUris = trackDao.getTrackUrisForFolder(sourceKey)
            val scannedUris = newTracks.mapTo(mutableSetOf()) { it.uri }
            val staleUris = existingUris
                .filterNot(scannedUris::contains)
                .filter(::isDefinitelyMissing)
            val entities = newTracks.map { track ->
                TrackEntity(
                    uri = track.uri,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    albumArtUri = track.albumArtUri,
                    dateAdded = trackDao.getTrackDateAdded(track.uri) ?: track.dateAdded,
                    genre = track.genre,
                    playCount = 0,
                    isLiked = false,
                    folderPath = track.folderPath,
                    filePath = null,
                    replayGainTrackDb = track.replayGainTrackDb,
                    replayGainTrackPeak = track.replayGainTrackPeak,
                    replayGainAlbumDb = track.replayGainAlbumDb,
                    replayGainAlbumPeak = track.replayGainAlbumPeak
                )
            }
            db.withTransaction {
                if (staleUris.isNotEmpty()) trackDao.deleteTracksByUris(staleUris)
                if (entities.isNotEmpty()) {
                    entities.forEach { track ->
                        trackDao.updateScannedMetadata(
                            track.uri, track.title, track.artist, track.album, track.durationMs,
                            track.albumArtUri, track.dateAdded, track.genre, track.folderPath, track.filePath,
                            track.replayGainTrackDb, track.replayGainTrackPeak,
                            track.replayGainAlbumDb, track.replayGainAlbumPeak
                        )
                    }
                    trackDao.insertTracks(entities)
                }
                val folderDao = db.folderDao()
                val existing = folderDao.getFolderByUri(sourceKey)
                val folder = FolderEntity(
                    id = existing?.id ?: 0,
                    name = docFile.name ?: context.getString(com.example.xargoosh.R.string.added_folder),
                    uriString = sourceKey,
                    dateAdded = existing?.dateAdded ?: System.currentTimeMillis()
                )
                if (existing == null) folderDao.insertFolder(folder) else folderDao.updateFolder(folder)
            }
        }
    }

    private fun isDefinitelyMissing(uri: String): Boolean {
        return try {
            context.contentResolver.openAssetFileDescriptor(android.net.Uri.parse(uri), "r")?.use { }
            false
        } catch (_: java.io.FileNotFoundException) {
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteTrack(trackUri: String) {
        db.withTransaction {
            db.playlistDao().removeTrackFromAllPlaylists(trackUri)
            db.lyricsDao().deleteLyrics(trackUri)
            trackDao.deleteTrack(trackUri)
        }
    }

    override suspend fun syncLocalMedia() {
        val tracks = mediaScanner.scanLocalAudio()
        val entities = tracks.map { track ->
            TrackEntity(
                uri = track.uri,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                dateAdded = track.dateAdded,
                albumArtUri = track.albumArtUri, 
                genre = track.genre ?: "",
                filePath = track.filePath,
                folderPath = track.folderPath,
                replayGainTrackDb = track.replayGainTrackDb,
                replayGainTrackPeak = track.replayGainTrackPeak,
                replayGainAlbumDb = track.replayGainAlbumDb,
                replayGainAlbumPeak = track.replayGainAlbumPeak
            )
        }
        trackDao.syncTracks(entities)
    }

    override fun getAllTracksAsc(): Flow<List<MusicTrack>> {
        return trackDao.getAllTracksAsc().map { entities -> entities.map { it.toMusicTrack() } }
    }

    override fun getAllTracksDesc(): Flow<List<MusicTrack>> {
        return trackDao.getAllTracksDesc().map { entities -> entities.map { it.toMusicTrack() } }
    }

    override fun getAllTracksByArtistAsc(): Flow<List<MusicTrack>> {
        return trackDao.getAllTracksByArtistAsc().map { entities -> entities.map { it.toMusicTrack() } }
    }

    override fun getAllTracksByDateAddedDesc(): Flow<List<MusicTrack>> {
        return trackDao.getAllTracksByDateAddedDesc().map { entities -> entities.map { it.toMusicTrack() } }
    }

    override fun getAllTracksByDateAddedAsc(): Flow<List<MusicTrack>> {
        return trackDao.getAllTracksByDateAddedAsc().map { entities -> entities.map { it.toMusicTrack() } }
    }

    override fun getMostPlayedTracks(): Flow<List<MusicTrack>> = trackDao.getMostPlayedTracks().map { entities -> entities.map { it.toMusicTrack() } }

    override fun getLikedTracks(): Flow<List<MusicTrack>> = trackDao.getLikedTracks().map { entities -> entities.map { it.toMusicTrack() } }

    override suspend fun createPlaylist(name: String): Long {
        return db.playlistDao().insertPlaylist(com.example.xargoosh.data.local.entities.PlaylistEntity(name = name, dateCreated = System.currentTimeMillis()))
    }

    override fun getAllPlaylists(): Flow<List<com.example.xargoosh.domain.models.Playlist>> {
        return db.playlistDao().getAllPlaylists().map { list -> list.map { com.example.xargoosh.domain.models.Playlist(it.id, it.name, it.dateCreated) } }
    }

    override suspend fun getPlaylistsOnce(): List<com.example.xargoosh.domain.models.Playlist> {
        return db.playlistDao().getAllPlaylists().first().map { com.example.xargoosh.domain.models.Playlist(it.id, it.name, it.dateCreated) }
    }

    override suspend fun deletePlaylist(playlistId: Int) {
        val lists = getPlaylistsOnce()
        val p = lists.find { it.id == playlistId }
        if (p?.name == "Favorites") return
        db.playlistDao().deletePlaylist(playlistId)
    }

    override suspend fun getOrCreateFavoritesPlaylist(): Int {
        return db.withTransaction { getOrCreateFavoritesInTransaction() }
    }

    private suspend fun getOrCreateFavoritesInTransaction(): Int {
        val playlistDao = db.playlistDao()
        return playlistDao.getPlaylistByName("Favorites")?.id
            ?: playlistDao.insertPlaylist(
                com.example.xargoosh.data.local.entities.PlaylistEntity(
                    name = "Favorites",
                    dateCreated = System.currentTimeMillis()
                )
            ).toInt()
    }

    override fun isFavorite(trackUri: String): Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            val favId = getOrCreateFavoritesPlaylist()
            db.playlistDao().getTracksForPlaylist(favId).collect { tracks ->
                emit(tracks.any { it.uri == trackUri })
            }
        }
    }

    override suspend fun toggleFavorite(trackUri: String) {
        db.withTransaction {
            val playlistDao = db.playlistDao()
            val favId = getOrCreateFavoritesInTransaction()
            if (playlistDao.containsTrack(favId, trackUri)) {
                playlistDao.removeTrackFromPlaylist(favId, trackUri)
                trackDao.updateLikeStatus(trackUri, false)
            } else {
                val position = playlistDao.getNextPosition(favId)
                playlistDao.insertPlaylistTrack(
                    com.example.xargoosh.data.local.entities.PlaylistTrackEntity(favId, trackUri, position)
                )
                trackDao.updateLikeStatus(trackUri, true)
            }
        }
    }

    override fun getTracksForPlaylist(playlistId: Int): Flow<List<MusicTrack>> {
        return db.playlistDao().getTracksForPlaylist(playlistId).map { entities -> entities.map { it.toMusicTrack() } }
    }

    override suspend fun addTrackToPlaylist(playlistId: Int, trackUri: String) {
        db.withTransaction {
            val playlistDao = db.playlistDao()
            val position = playlistDao.getNextPosition(playlistId)
            playlistDao.insertPlaylistTrack(com.example.xargoosh.data.local.entities.PlaylistTrackEntity(playlistId, trackUri, position))
        }
    }

    override suspend fun addTracksToPlaylist(playlistId: Int, trackUris: Collection<String>) {
        db.withTransaction {
            val playlistDao = db.playlistDao()
            var position = playlistDao.getNextPosition(playlistId)
            trackUris.distinct().forEach { uri ->
                if (!playlistDao.containsTrack(playlistId, uri)) {
                    playlistDao.insertPlaylistTrack(
                        com.example.xargoosh.data.local.entities.PlaylistTrackEntity(playlistId, uri, position++)
                    )
                }
            }
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Int, trackUri: String) {
        db.playlistDao().removeTrackFromPlaylist(playlistId, trackUri)
    }

    override fun getFolders(): Flow<List<MusicFolder>> {
        return db.folderDao().getAllFolders().map { list -> list.map { MusicFolder(it.id, it.name, it.uriString, it.dateAdded) } }
    }

    override fun getTracksForFolder(folderKey: String): Flow<List<MusicTrack>> =
        trackDao.getTracksForFolder(folderKey).map { tracks ->
            tracks.map { it.toMusicTrack() }
        }

    override suspend fun deleteFolder(folderId: Int) {
        val folder = db.folderDao().getFolder(folderId) ?: return
        trackDao.deleteTracksForFolder(folder.uriString)
        db.folderDao().deleteFolder(folderId)
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                android.net.Uri.parse(folder.uriString),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
}


