package com.example.xargoosh.feature.library.presentation

import com.example.xargoosh.domain.models.MusicTrack
import java.util.Locale
import kotlinx.serialization.Serializable

enum class AlbumLayout { LIST, GRID }
enum class AlbumSort { NAME_ASC, NAME_DESC, ARTIST_ASC, ARTIST_DESC, SONG_COUNT_ASC, SONG_COUNT_DESC }
enum class ArtistSort { NAME_ASC, NAME_DESC, SONG_COUNT_ASC, SONG_COUNT_DESC, ALBUM_COUNT_ASC, ALBUM_COUNT_DESC }
enum class GenreSort { NAME_ASC, NAME_DESC, SONG_COUNT_ASC, SONG_COUNT_DESC }
enum class PlaylistSort { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC }
enum class FolderSort { NAME_ASC, NAME_DESC, SONG_COUNT_ASC, SONG_COUNT_DESC, DATE_ASC, DATE_DESC }

@Serializable
enum class SmartPlaylistKind {
    RECENTLY_ADDED,
    MOST_PLAYED,
    NEVER_PLAYED
}

fun SmartPlaylistKind.selectTracks(tracks: List<MusicTrack>): List<MusicTrack> {
    val titleOrder = compareBy<MusicTrack>(
        { it.title.lowercase(Locale.ROOT) },
        { it.title },
        { it.uri }
    )
    return when (this) {
        SmartPlaylistKind.RECENTLY_ADDED -> tracks
            .sortedWith(compareByDescending<MusicTrack> { it.dateAdded }.then(titleOrder))
            .take(50)
        SmartPlaylistKind.MOST_PLAYED -> tracks
            .filter { it.playCount > 0 }
            .sortedWith(compareByDescending<MusicTrack> { it.playCount }.then(titleOrder))
            .take(50)
        SmartPlaylistKind.NEVER_PLAYED -> tracks
            .filter { it.playCount == 0 }
            .sortedWith(compareByDescending<MusicTrack> { it.dateAdded }.then(titleOrder))
    }
}

data class AlbumGroup(
    val name: String,
    val artist: String,
    val tracks: List<MusicTrack>
) {
    val key: String = "$name\u0000$artist"
}

data class ArtistGroup(
    val name: String,
    val tracks: List<MusicTrack>
) {
    val albumCount: Int = tracks.map { it.album.lowercase() }.distinct().size
}

data class GenreGroup(
    val name: String,
    val tracks: List<MusicTrack>
)

object GenreNames {
    private val separators = Regex("[,;/]")

    fun from(raw: String?): List<String> = raw.orEmpty()
        .split(separators)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)

    fun contains(raw: String?, genre: String): Boolean =
        from(raw).any { it.equals(genre, ignoreCase = true) }
}
