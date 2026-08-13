package com.example.xargoosh.data.local

import android.content.Context
import android.provider.MediaStore
import com.example.xargoosh.domain.models.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.xargoosh.R
import java.io.File

class MediaScanner(private val context: Context) {

    suspend fun scanLocalAudio(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MusicTrack>()
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            projection += MediaStore.Audio.Media.GENRE
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(),
            selection,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        ) ?: throw java.io.IOException(context.getString(R.string.media_scan_no_result))
        cursor.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val genreColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else {
                -1
            }

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: context.getString(R.string.unknown_title)
                val artist = it.getString(artistColumn) ?: context.getString(R.string.unknown_artist)
                val album = it.getString(albumColumn) ?: context.getString(R.string.unknown_album)
                val duration = it.getLong(durationColumn)
                val displayName = it.getString(displayNameColumn).orEmpty()
                val data = it.getString(dataColumn)
                val dateAdded = it.getLong(dateAddedColumn)
                val relativePath = it.getString(relativePathColumn) ?: ""

                val contentUri = "content://media/external/audio/media/$id"

                val lowerName = displayName.lowercase()
                if (lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") ||
                    lowerName.endsWith(".flac") || lowerName.endsWith(".wav")) {

                    var genre = if (genreColumn >= 0) it.getString(genreColumn) else null
                    if (genre.isNullOrBlank()) {
                        genre = runCatching {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, android.net.Uri.parse(contentUri))
                                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)
                            } finally {
                                retriever.release()
                            }
                        }.getOrNull()
                    }

                    val replayGain = data?.let(::File)
                        ?.takeIf { file -> file.isFile && file.canRead() }
                        ?.let { file -> runCatching { ReplayGainMetadataReader.read(file) }.getOrNull() }
                        ?: ReplayGainMetadata()
                    tracks.add(
                        MusicTrack(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri,
                            albumArtUri = "content://media/external/audio/albumart/${it.getLong(albumIdColumn)}",
                            dateAdded = dateAdded,
                            genre = genre,
                            folderPath = relativePath.trimEnd('/'),
                            filePath = data,
                            replayGainTrackDb = replayGain.trackDb,
                            replayGainTrackPeak = replayGain.trackPeak,
                            replayGainAlbumDb = replayGain.albumDb,
                            replayGainAlbumPeak = replayGain.albumPeak
                        )
                    )
                }
            }
        }
        tracks
    }
}
