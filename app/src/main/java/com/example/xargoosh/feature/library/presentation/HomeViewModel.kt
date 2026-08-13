package com.example.xargoosh.feature.library.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xargoosh.data.local.MediaScanner
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.domain.models.MusicFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import com.example.xargoosh.utils.ArtistCredits

import com.example.xargoosh.data.DataRepository
import com.example.xargoosh.data.local.db.AppDatabase
import kotlinx.coroutines.flow.catch
import com.example.xargoosh.R

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: com.example.xargoosh.data.DataRepository = com.example.xargoosh.data.DataRepositoryImpl(
        application,
        MediaScanner(application),
        AppDatabase.getDatabase(application)
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _allTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val allTracks: StateFlow<List<MusicTrack>> = _allTracks.asStateFlow()

    private val _mostPlayedTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val mostPlayedTracks: StateFlow<List<MusicTrack>> = _mostPlayedTracks.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val likedTracks: StateFlow<List<MusicTrack>> = _likedTracks.asStateFlow()

    suspend fun updateAlbumTag(trackUri: String, album: String) {
        AppDatabase.getDatabase(getApplication()).trackDao().updateAlbumTag(trackUri, album)
    }

    suspend fun updateArtistTag(trackUri: String, artist: String) {
        AppDatabase.getDatabase(getApplication()).trackDao().updateArtistTag(trackUri, artist)
    }

    suspend fun invalidateLyrics(trackUri: String) {
        AppDatabase.getDatabase(getApplication()).lyricsDao().deleteLyrics(trackUri)
    }

    enum class SortOption {
        TITLE_ASC, TITLE_DESC, ARTIST_ASC, ARTIST_DESC, ARTIST_TRACK_COUNT, DATE_ADDED_DESC, DATE_ADDED_ASC, ARTIST_DATE_ADDED_DESC, ARTIST_DATE_ADDED_ASC
    }
    
    fun createPlaylistAndAddTrack(name: String, trackUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createPlaylist(name)
            repository.addTrackToPlaylist(newId.toInt(), trackUri)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackToPlaylist(playlistId, trackUri)
        }
    }

    fun addTracksToPlaylist(playlistId: Int, trackUris: Collection<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTracksToPlaylist(playlistId, trackUris)
        }
    }

    fun createPlaylistAndAddTracks(name: String, trackUris: Collection<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createPlaylist(name)
            repository.addTracksToPlaylist(newId.toInt(), trackUris)
        }
    }

    fun getTracksForPlaylist(playlistId: Int) = repository.getTracksForPlaylist(playlistId)

    fun getTracksForFolder(folderKey: String) = repository.getTracksForFolder(folderKey)


    private val prefs = application.getSharedPreferences("xargoosh_prefs", android.content.Context.MODE_PRIVATE)
    private val _currentSort = MutableStateFlow(
        runCatching {
            SortOption.valueOf(prefs.getString("sort_order", SortOption.TITLE_ASC.name) ?: SortOption.TITLE_ASC.name)
        }.getOrDefault(SortOption.TITLE_ASC)
    )
    val currentSort: StateFlow<SortOption> = _currentSort.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _genreFilter = MutableStateFlow<String?>(null)
    val genreFilter: StateFlow<String?> = _genreFilter.asStateFlow()

    private val _availableGenres = MutableStateFlow<List<String>>(emptyList())
    val availableGenres: StateFlow<List<String>> = _availableGenres.asStateFlow()

    private fun <T : Enum<T>> storedEnum(key: String, default: T, values: Array<T>): MutableStateFlow<T> {
        val stored = prefs.getString(key, null)
        return MutableStateFlow(values.firstOrNull { it.name == stored } ?: default)
    }

    private val _albumSort = storedEnum("albums_sort_order", AlbumSort.NAME_ASC, AlbumSort.entries.toTypedArray())
    val albumSort = _albumSort.asStateFlow()
    private val _artistSort = storedEnum("artists_sort_order", ArtistSort.NAME_ASC, ArtistSort.entries.toTypedArray())
    val artistSort = _artistSort.asStateFlow()
    private val _genreSort = storedEnum("genres_sort_order", GenreSort.NAME_ASC, GenreSort.entries.toTypedArray())
    val genreSort = _genreSort.asStateFlow()
    private val _playlistSort = storedEnum("playlists_sort_order", PlaylistSort.DATE_DESC, PlaylistSort.entries.toTypedArray())
    val playlistSort = _playlistSort.asStateFlow()
    private val _folderSort = storedEnum("folders_sort_order", FolderSort.NAME_ASC, FolderSort.entries.toTypedArray())
    val folderSort = _folderSort.asStateFlow()
    private val _albumLayout = storedEnum("albums_layout", AlbumLayout.GRID, AlbumLayout.entries.toTypedArray())
    val albumLayout = _albumLayout.asStateFlow()

    init {
        observeTracks()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String?) {
        _genreFilter.value = genre
    }

    private fun observeTracks() {
        viewModelScope.launch {
            repository.getAllTracksAsc().catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: getApplication<Application>().getString(R.string.tracks_database_error))
            }.collect { tracks ->
                _allTracks.value = tracks
                _availableGenres.value = tracks.flatMap { GenreNames.from(it.genre) }
                    .distinctBy(String::lowercase).sortedBy(String::lowercase)
            }
        }
        viewModelScope.launch {
            combine(_allTracks, _searchQuery, _genreFilter, _currentSort) { source, query, genre, sort ->
                val filtered = source.filter { matchesSearch(it, query) && (genre == null || GenreNames.contains(it.genre, genre)) }
                sortTracks(filtered, sort)
            }.flowOn(Dispatchers.Default).collect { tracks ->
                val scanning = (_uiState.value as? HomeUiState.Success)?.isScanning == true
                _uiState.value = HomeUiState.Success(tracks, scanning)
            }
        }
        viewModelScope.launch { repository.getMostPlayedTracks().collect { _mostPlayedTracks.value = it } }
        viewModelScope.launch { repository.getLikedTracks().collect { _likedTracks.value = it } }
    }

    fun updateSort(option: SortOption) {
        _currentSort.value = option
        prefs.edit().putString("sort_order", option.name).apply()
    }

    fun updateAlbumSort(value: AlbumSort) { _albumSort.value = value; prefs.edit().putString("albums_sort_order", value.name).apply() }
    fun updateArtistSort(value: ArtistSort) { _artistSort.value = value; prefs.edit().putString("artists_sort_order", value.name).apply() }
    fun updateGenreSort(value: GenreSort) { _genreSort.value = value; prefs.edit().putString("genres_sort_order", value.name).apply() }
    fun updatePlaylistSort(value: PlaylistSort) { _playlistSort.value = value; prefs.edit().putString("playlists_sort_order", value.name).apply() }
    fun updateFolderSort(value: FolderSort) { _folderSort.value = value; prefs.edit().putString("folders_sort_order", value.name).apply() }
    fun updateAlbumLayout(value: AlbumLayout) { _albumLayout.value = value; prefs.edit().putString("albums_layout", value.name).apply() }

    val albumGroups: StateFlow<List<AlbumGroup>> = combine(_allTracks, _searchQuery, _albumSort) { source, query, sort ->
        val groups = source.filter { matchesSearch(it, query) }.groupBy { track ->
            track.album to (ArtistCredits.names(track.artist).firstOrNull() ?: track.artist)
        }.map { (identity, tracks) -> AlbumGroup(identity.first, identity.second, tracks) }
        when (sort) {
            AlbumSort.NAME_ASC -> groups.sortedBy { it.name.lowercase() }
            AlbumSort.NAME_DESC -> groups.sortedByDescending { it.name.lowercase() }
            AlbumSort.ARTIST_ASC -> groups.sortedBy { it.artist.lowercase() }
            AlbumSort.ARTIST_DESC -> groups.sortedByDescending { it.artist.lowercase() }
            AlbumSort.SONG_COUNT_ASC -> groups.sortedBy { it.tracks.size }
            AlbumSort.SONG_COUNT_DESC -> groups.sortedByDescending { it.tracks.size }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val artistGroups: StateFlow<List<ArtistGroup>> = combine(_allTracks, _searchQuery, _artistSort) { source, query, sort ->
        val groups = ArtistCredits.groupTracks(source).map { ArtistGroup(it.key, it.value) }
            .filter { query.isBlank() || ArtistCredits.matchScore(it.name, query) != Int.MAX_VALUE || it.tracks.any { track -> matchesSearch(track, query) } }
        when (sort) {
            ArtistSort.NAME_ASC -> groups.sortedBy { it.name.lowercase() }
            ArtistSort.NAME_DESC -> groups.sortedByDescending { it.name.lowercase() }
            ArtistSort.SONG_COUNT_ASC -> groups.sortedBy { it.tracks.size }
            ArtistSort.SONG_COUNT_DESC -> groups.sortedByDescending { it.tracks.size }
            ArtistSort.ALBUM_COUNT_ASC -> groups.sortedBy { it.albumCount }
            ArtistSort.ALBUM_COUNT_DESC -> groups.sortedByDescending { it.albumCount }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val genreGroups: StateFlow<List<GenreGroup>> = combine(_allTracks, _searchQuery, _genreSort) { source, query, sort ->
        val grouped = linkedMapOf<String, MutableList<MusicTrack>>()
        source.filter { matchesSearch(it, query) }.forEach { track ->
            GenreNames.from(track.genre).forEach { genre ->
                val displayName = grouped.keys.firstOrNull { it.equals(genre, ignoreCase = true) } ?: genre
                grouped.getOrPut(displayName) { mutableListOf() }.add(track)
            }
        }
        val groups = grouped.map { GenreGroup(it.key, it.value.distinctBy(MusicTrack::uri)) }
        when (sort) {
            GenreSort.NAME_ASC -> groups.sortedBy { it.name.lowercase() }
            GenreSort.NAME_DESC -> groups.sortedByDescending { it.name.lowercase() }
            GenreSort.SONG_COUNT_ASC -> groups.sortedBy { it.tracks.size }
            GenreSort.SONG_COUNT_DESC -> groups.sortedByDescending { it.tracks.size }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val explicitFolders: StateFlow<List<MusicFolder>> = repository.getFolders()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteFolder(folderId: Int) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    val playlists: StateFlow<List<com.example.xargoosh.domain.models.Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanSafFolder(uri: android.net.Uri) {
        viewModelScope.launch {
            repository.scanSafFolder(uri)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    private var hasScannedLocalMusic = false

    fun forceScanLocalMusic() {
        hasScannedLocalMusic = false
        loadLocalMusic()
    }

    fun deleteTrack(trackUri: String) {
        viewModelScope.launch { repository.deleteTrack(trackUri) }
    }

    fun loadLocalMusic() {
        if (hasScannedLocalMusic) return
        hasScannedLocalMusic = true

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                _uiState.value = currentState.copy(isScanning = true)
            } else {
                _uiState.value = HomeUiState.Loading
            }
            try {
                repository.syncLocalMedia()
                val newState = _uiState.value
                if (newState is HomeUiState.Success) {
                    _uiState.value = newState.copy(isScanning = false)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: getApplication<Application>().getString(R.string.music_scan_failed))
                hasScannedLocalMusic = false
            }
        }
    }
}

private fun matchesSearch(track: MusicTrack, rawQuery: String): Boolean {
    val query = rawQuery.trim().lowercase()
    return query.isEmpty() || track.title.lowercase().contains(query) ||
        ArtistCredits.names(track.artist).any { ArtistCredits.matchScore(it, query) != Int.MAX_VALUE } ||
        track.album.lowercase().contains(query) || track.genre.orEmpty().lowercase().contains(query)
}

private fun sortTracks(tracks: List<MusicTrack>, sort: HomeViewModel.SortOption): List<MusicTrack> = when (sort) {
    HomeViewModel.SortOption.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
    HomeViewModel.SortOption.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
    HomeViewModel.SortOption.ARTIST_ASC -> tracks.sortedBy { it.artist.lowercase() }
    HomeViewModel.SortOption.ARTIST_DESC -> tracks.sortedByDescending { it.artist.lowercase() }
    HomeViewModel.SortOption.ARTIST_TRACK_COUNT -> {
        val counts = tracks.groupingBy { it.artist.lowercase() }.eachCount()
        tracks.sortedByDescending { counts[it.artist.lowercase()] ?: 0 }
    }
    HomeViewModel.SortOption.DATE_ADDED_DESC, HomeViewModel.SortOption.ARTIST_DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAdded }
    HomeViewModel.SortOption.DATE_ADDED_ASC, HomeViewModel.SortOption.ARTIST_DATE_ADDED_ASC -> tracks.sortedBy { it.dateAdded }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val tracks: List<MusicTrack>, val isScanning: Boolean = false) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}


