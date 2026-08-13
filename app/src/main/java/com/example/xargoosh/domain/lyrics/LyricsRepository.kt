package com.example.xargoosh.domain.lyrics

import android.content.Context
import android.net.Uri
import com.example.xargoosh.data.local.db.AppDatabase
import com.example.xargoosh.data.local.entities.LyricsEntity
import com.example.xargoosh.domain.models.LyricLine
import com.example.xargoosh.domain.models.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray

object LyricsRepository {

    suspend fun getLyrics(context: Context, track: MusicTrack): List<LyricLine>? = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).lyricsDao()

        val cached = dao.getLyrics(track.uri)
        if (cached != null) {
            if (!cached.syncedLyrics.isNullOrBlank()) {
                parseLrc(cached.syncedLyrics).takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }
            if (!cached.plainLyrics.isNullOrBlank()) {
                return@withContext parsePlain(cached.plainLyrics)
            }
        }

        val localLyrics = getLocalLyrics(context, track)
        if (!localLyrics.isNullOrBlank()) {
            val localSynced = parseLrc(localLyrics)
            if (localSynced.isNotEmpty()) {
                dao.insertLyrics(LyricsEntity(trackUri = track.uri, syncedLyrics = localLyrics, plainLyrics = null))
                return@withContext localSynced
            }
        }

        val onlineEnabled = context.getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE)
            .getBoolean("online_lyrics_enabled", false)
        val apiLyricsPair = if (onlineEnabled) {
            getLyricsFromApi(track.title, track.artist, track.album, track.durationMs)
        } else null
        if (apiLyricsPair != null) {
            dao.insertLyrics(LyricsEntity(trackUri = track.uri, syncedLyrics = apiLyricsPair.first, plainLyrics = apiLyricsPair.second))
            if (!apiLyricsPair.first.isNullOrBlank()) return@withContext parseLrc(apiLyricsPair.first!!)
            if (!apiLyricsPair.second.isNullOrBlank()) return@withContext parsePlain(apiLyricsPair.second!!)
        }

        val plain = localLyrics ?: cached?.plainLyrics
        if (!plain.isNullOrBlank()) {
            dao.insertLyrics(LyricsEntity(trackUri = track.uri, syncedLyrics = null, plainLyrics = plain))
            return@withContext parsePlain(plain)
        }
        return@withContext null
    }

    private fun getLocalLyrics(context: Context, track: MusicTrack): String? {
        try {
            if (!track.filePath.isNullOrBlank()) {
                val file = File(track.filePath)
                if (file.exists()) {
                    val audioFile = AudioFileIO.read(file)
                    return audioFile.tag?.getFirst(FieldKey.LYRICS)
                }
            }
            val uri = Uri.parse(track.uri)
            val extension = track.filePath?.substringAfterLast('.', "mp3")?.takeIf { it.length <= 5 } ?: "mp3"
            val tempFile = File.createTempFile("lyrics_", ".$extension", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                } ?: return null
                return AudioFileIO.read(tempFile).tag?.getFirst(FieldKey.LYRICS)
            } finally {
                tempFile.delete()
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun getLyricsFromApi(title: String, artist: String, album: String, durationMs: Long): Pair<String?, String?>? {
        try {
            val t = URLEncoder.encode(title, "UTF-8")
            val a = URLEncoder.encode(artist, "UTF-8")
            val al = URLEncoder.encode(album, "UTF-8")
            val d = (durationMs / 1000).toInt()
            val urlString = "https://lrclib.net/api/get?track_name=$t&artist_name=$a&album_name=$al&duration=$d"
            var plainFallback: String? = null
            requestJson(urlString)?.let { json ->
                val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                plainFallback = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                if (synced != null) return Pair(synced, plainFallback)
            }
            val searchUrl = "https://lrclib.net/api/search?track_name=$t&artist_name=$a"
            val results = requestText(searchUrl)?.let { JSONArray(it) }
            if (results != null) {
                val candidates = (0 until results.length()).mapNotNull(results::optJSONObject)
                val syncedResult = candidates
                    .filter { it.optString("syncedLyrics", "").isNotBlank() }
                    .minByOrNull { kotlin.math.abs(it.optDouble("duration", 0.0) - durationMs / 1000.0) }
                    ?.takeIf { kotlin.math.abs(it.optDouble("duration", 0.0) - durationMs / 1000.0) <= 20.0 }
                if (syncedResult != null) return Pair(
                    syncedResult.optString("syncedLyrics", ""),
                    syncedResult.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                )
                val plainResult = candidates.firstOrNull { it.optString("plainLyrics", "").isNotBlank() }
                plainFallback = plainFallback ?: plainResult?.optString("plainLyrics", "")
            }
            if (!plainFallback.isNullOrBlank()) return Pair(null, plainFallback)
        } catch (_: Exception) {
        }
        return null
    }

    private fun requestJson(url: String): JSONObject? = requestText(url)?.let(::JSONObject)

    private fun requestText(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "XargooshMusicPlayer/1.1.0")
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input -> readBoundedText(input, 1024 * 1024) }
            } else null
        } finally {
            connection.disconnect()
        }
    }

    private fun readBoundedText(input: java.io.InputStream, maxBytes: Int): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw java.io.IOException("Lyrics response exceeds the size limit.")
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    internal fun parseLrc(lrcContent: String): List<LyricLine> {
        val offset = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)
            .find(lrcContent)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val timestamp = Regex("""\[(\d+):([0-5]?\d)(?:[.:](\d{1,3}))?]""")
        return lrcContent.lines().flatMap { line ->
            val matches = timestamp.findAll(line).toList()
            if (matches.isEmpty()) return@flatMap emptyList()
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@flatMap emptyList()
            matches.mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> fraction.toLongOrNull()?.times(100) ?: 0L
                    2 -> fraction.toLongOrNull()?.times(10) ?: 0L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                LyricLine((minutes * 60_000 + seconds * 1_000 + millis + offset).coerceAtLeast(0), text)
            }
        }.distinctBy { it.timestampMs to it.text }.sortedBy { it.timestampMs }
    }

    private fun parsePlain(plainContent: String): List<LyricLine> {
        return plainContent.lineSequence().map(String::trim).filter(String::isNotEmpty)
            .map { LyricLine(timestampMs = 0L, text = it, isSynced = false) }.toList()
    }
}
