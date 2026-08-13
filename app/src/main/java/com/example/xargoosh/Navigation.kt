package com.example.xargoosh

import com.example.xargoosh.core.design.themes.XargooshTheme

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.serialization.Serializable
import com.example.xargoosh.feature.library.presentation.HomeScreen
import com.example.xargoosh.feature.player.presentation.NowPlayingScreen
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import com.example.xargoosh.presentation.equalizer.EqualizerScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.ui.res.stringResource

@Serializable data object HomeRoute : NavKey
@Serializable data object LibraryRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object NowPlayingRoute : NavKey
@Serializable data object QueueRoute : NavKey
@Serializable data object EqualizerRoute : NavKey
@Serializable data object MetadataEditorRoute : NavKey
@Serializable data class AlbumDetailsRoute(val albumName: String, val albumArtist: String? = null) : NavKey
@Serializable data class ArtistDetailsRoute(val artistName: String) : NavKey
@Serializable data class PlaylistDetailsRoute(val playlistId: Int, val playlistName: String) : NavKey
@Serializable data class SmartPlaylistRoute(val kind: com.example.xargoosh.feature.library.presentation.SmartPlaylistKind) : NavKey
@Serializable data class FolderDetailsRoute(val folderName: String, val folderKey: String) : NavKey
@Serializable data class GenreDetailsRoute(val genreName: String) : NavKey
@Serializable data object FullscreenVisualizerRoute : NavKey
@Serializable data object VisualizerSettingsRoute : NavKey
@Serializable data object MusicRecognitionRoute : NavKey

@Composable
fun MainNavigation(openNowPlayingRequest: Long = 0L) {
  val backStack = rememberNavBackStack(HomeRoute)
  androidx.compose.runtime.LaunchedEffect(openNowPlayingRequest) {
      if (openNowPlayingRequest > 0L && backStack.lastOrNull() != NowPlayingRoute) {
          backStack.add(NowPlayingRoute)
      }
  }
  val playerViewModel: PlayerViewModel = viewModel()
  val metadataEditorViewModel: com.example.xargoosh.presentation.editor.MetadataEditorViewModel = viewModel()
  val homeViewModel: com.example.xargoosh.feature.library.presentation.HomeViewModel = viewModel()
  val context = androidx.compose.ui.platform.LocalContext.current
  val openArtist: (String) -> Unit = { rawCredit ->
      val artist = com.example.xargoosh.utils.ArtistCredits.names(rawCredit).firstOrNull() ?: rawCredit
      backStack.add(ArtistDetailsRoute(artist))
  }
  var pendingDeleteUri by remember { mutableStateOf<String?>(null) }
  val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
          pendingDeleteUri?.let { uri ->
              homeViewModel.deleteTrack(uri)
              playerViewModel.removeTrackFromQueue(uri)
          }
      }
      pendingDeleteUri = null
  }
  val deleteTrackFromDevice: (com.example.xargoosh.domain.models.MusicTrack) -> Unit = { track ->
      com.example.xargoosh.utils.TrackUtils.deleteTrack(
          context = context,
          trackId = track.uri,
          onIntentSenderRequired = { request -> pendingDeleteUri = track.uri; deleteLauncher.launch(request) },
          onSuccess = {
              homeViewModel.deleteTrack(track.uri)
              playerViewModel.removeTrackFromQueue(track.uri)
          }
      )
  }
  val scope = rememberCoroutineScope()
  var trackForPlaylist by remember { mutableStateOf<com.example.xargoosh.domain.models.MusicTrack?>(null) }
  val playlists by homeViewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())

  Box(modifier = Modifier.fillMaxSize()) {

    if (trackForPlaylist != null) {
        var showCreateField by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { trackForPlaylist = null; showCreateField = false; newPlaylistName = "" },
            title = { androidx.compose.material3.Text(stringResource(R.string.add_to_playlist), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {

                    if (showCreateField) {
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                placeholder = { androidx.compose.material3.Text(stringResource(R.string.playlist_name)) },
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                singleLine = true
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                            androidx.compose.material3.IconButton(onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    scope.launch {
                                        homeViewModel.createPlaylistAndAddTrack(newPlaylistName.trim(), trackForPlaylist!!.uri)
                                        trackForPlaylist = null
                                        showCreateField = false
                                        newPlaylistName = ""
                                    }
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = stringResource(R.string.create),
                                    tint = XargooshTheme.colors.primary
                                )
                            }
                        }
                    } else {
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .clickable { showCreateField = true }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null, tint = XargooshTheme.colors.primary)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                            androidx.compose.material3.Text(stringResource(R.string.create_new_playlist), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = XargooshTheme.colors.primary)
                        }
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    if (playlists.isEmpty() && !showCreateField) {
                        androidx.compose.material3.Text(stringResource(R.string.no_playlists_create_hint))
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                            items(playlists.size) { index ->
                                val playlist = playlists[index]
                                androidx.compose.foundation.layout.Row(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                homeViewModel.addTrackToPlaylist(playlist.id, trackForPlaylist!!.uri)
                                                trackForPlaylist = null
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = XargooshTheme.colors.onSurfaceVariant
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                                    androidx.compose.material3.Text(
                                        if (playlist.name == "Favorites") androidx.compose.ui.res.stringResource(R.string.favorites) else playlist.name,
                                        fontSize = 16.sp,
                                        color = XargooshTheme.colors.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { trackForPlaylist = null; showCreateField = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      entryProvider =
        entryProvider {
          entry<HomeRoute> {
            HomeScreen(
                viewModel = homeViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = { tracks, startIndex ->
                    playerViewModel.playQueue(tracks, startIndex)
                },
                onEditMetadata = { track ->
                    metadataEditorViewModel.loadTrack(track)
                    backStack.add(MetadataEditorRoute)
                },
                onNavigateToSettings = {
                    backStack.add(SettingsRoute)
                },
                onNavigateToMusicRecognition = {
                    backStack.add(MusicRecognitionRoute)
                },
                onNavigateToEqualizer = {
                    backStack.add(EqualizerRoute)
                },
                onNavigateToNowPlaying = {
                    backStack.add(NowPlayingRoute)
                },
                onNavigateToAlbum = { albumName, albumArtist ->
                    backStack.add(AlbumDetailsRoute(albumName, albumArtist))
                },
                onNavigateToArtist = { artistName ->
                    openArtist(artistName)
                },
                 onNavigateToPlaylist = { playlistId, playlistName ->
                     backStack.add(PlaylistDetailsRoute(playlistId, playlistName))
                 },
                 onNavigateToSmartPlaylist = { kind -> backStack.add(SmartPlaylistRoute(kind)) },
                onNavigateToQueue = { backStack.add(QueueRoute) },
                onPlayNext = { track ->
                    playerViewModel.playNext(track)
                },
                onAddToQueue = { track ->
                    playerViewModel.addToQueue(track)
                },
                onNavigateToFolder = { folder ->
                    backStack.add(FolderDetailsRoute(folder.name, folder.uriString))
                },
                onNavigateToGenre = { genre -> backStack.add(GenreDetailsRoute(genre)) }
            )
          }
          entry<NowPlayingRoute> {
              NowPlayingScreen(
                  viewModel = playerViewModel,
                  onBack = { backStack.removeLastOrNull() },
                  onAddToPlaylist = { track -> trackForPlaylist = track },
                  onNavigateToQueue = { backStack.add(QueueRoute) },
                  onEditMetadata = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) },
                  onNavigateToAlbum = { backStack.add(AlbumDetailsRoute(it)) },
                  onNavigateToArtist = openArtist,
                  onTrackDeleted = { uri -> homeViewModel.deleteTrack(uri); playerViewModel.removeTrackFromQueue(uri) },
                  onNavigateFullscreen = { backStack.add(FullscreenVisualizerRoute) },
                  onNavigateEqualizer = { backStack.add(EqualizerRoute) }
              )
          }
          entry<QueueRoute> {
              com.example.xargoosh.presentation.queue.QueueScreen(
                  viewModel = playerViewModel,
                  onBack = { backStack.removeLastOrNull() }
              )
          }
          entry<MetadataEditorRoute> {
               com.example.xargoosh.presentation.editor.MetadataEditorScreen(
                   viewModel = metadataEditorViewModel,
                   onNavigateBack = { homeViewModel.forceScanLocalMusic(); backStack.removeLastOrNull() },
                   onTrackSaved = { playerViewModel.refreshTrackMetadata(listOf(it)) }
               )
          }
          entry<SettingsRoute> {
              val audioSessionId by playerViewModel.audioSessionId.collectAsStateWithLifecycle()
              com.example.xargoosh.feature.settings.presentation.SettingsScreen(
                  audioSessionId = audioSessionId,
                    playerViewModel = playerViewModel,
                  onNavigateBack = { backStack.removeLastOrNull() },
                  onNavigateToEqualizer = { backStack.add(EqualizerRoute) },
                  onNavigateToVisualizer = { backStack.add(VisualizerSettingsRoute) }
              )
          }
          entry<EqualizerRoute> {
              val audioSessionId by playerViewModel.audioSessionId.collectAsStateWithLifecycle()
              EqualizerScreen(
                  audioSessionId = audioSessionId,
                  onBack = { backStack.removeLastOrNull() }
              )
          }
          entry<AlbumDetailsRoute> { route ->
              val tracks by homeViewModel.allTracks.collectAsStateWithLifecycle()
              val albumTracks = tracks.filter { track ->
                  track.album == route.albumName && (route.albumArtist == null ||
                      com.example.xargoosh.utils.ArtistCredits.names(track.artist).firstOrNull()
                          ?.equals(route.albumArtist, ignoreCase = true) == true)
              }
              com.example.xargoosh.feature.playlists.presentation.AlbumDetailsScreen(
                  albumName = route.albumName,
                  tracks = albumTracks,
                  onBackClick = { backStack.removeLastOrNull() },
                  onTrackClick = { idx ->
                      playerViewModel.playQueue(albumTracks, idx)
                  },
                  onShuffleAll = {
                      if (albumTracks.isNotEmpty()) {
                           playerViewModel.playShuffled(albumTracks)
                      }
                  },
                   onPlayAll = {
                      if (albumTracks.isNotEmpty()) {
                          playerViewModel.playQueue(albumTracks, 0)
                      }
                   },
                   onPlayNext = playerViewModel::playNext,
                   onAddToQueue = playerViewModel::addToQueue,
                   onAddToPlaylist = { trackForPlaylist = it },
                    onGoToArtist = openArtist,
                   onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) },
                   onDeleteTrack = deleteTrackFromDevice
              )
          }

          entry<ArtistDetailsRoute> { route ->
              val tracks by homeViewModel.allTracks.collectAsStateWithLifecycle()
              val artistTracks = tracks.filter { com.example.xargoosh.utils.ArtistCredits.contains(it.artist, route.artistName) }
              com.example.xargoosh.feature.playlists.presentation.ArtistDetailsScreen(
                  artistName = route.artistName,
                  tracks = artistTracks,
                  onBackClick = { backStack.removeLastOrNull() },
                  onTrackClick = { idx ->
                      playerViewModel.playQueue(artistTracks, idx)
                  },
                  onAlbumClick = { albumName ->
                      backStack.add(AlbumDetailsRoute(albumName))
                  },
                  onShuffleAll = {
                      if (artistTracks.isNotEmpty()) {
                           playerViewModel.playShuffled(artistTracks)
                      }
                  },
                   onPlayAll = {
                      if (artistTracks.isNotEmpty()) {
                          playerViewModel.playQueue(artistTracks, 0)
                      }
                   },
                   onPlayNext = playerViewModel::playNext,
                   onAddToQueue = playerViewModel::addToQueue,
                   onAddToPlaylist = { trackForPlaylist = it },
                   onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) },
                   onDeleteTrack = deleteTrackFromDevice
              )
          }
          entry<FolderDetailsRoute> { route ->
              val folderTracks by homeViewModel.getTracksForFolder(route.folderKey)
                  .collectAsStateWithLifecycle(initialValue = emptyList())
              com.example.xargoosh.feature.playlists.presentation.FolderDetailsScreen(
                  folderPath = route.folderName,
                  tracks = folderTracks,
                  onBackClick = { backStack.removeLastOrNull() },
                  onTrackClick = { idx ->
                      playerViewModel.playQueue(folderTracks, idx)
                  },
                  onShuffleAll = {
                      if (folderTracks.isNotEmpty()) {
                           playerViewModel.playShuffled(folderTracks)
                      }
                  },
                  onPlayAll = {
                      if (folderTracks.isNotEmpty()) {
                          playerViewModel.playQueue(folderTracks, 0)
                      }
                  },
                   onPlayNext = { track -> playerViewModel.playNext(track) },
                   onAddToQueue = { track -> playerViewModel.addToQueue(track) },
                   onAddToPlaylist = { trackForPlaylist = it },
                   onGoToAlbum = { backStack.add(AlbumDetailsRoute(it)) },
                    onGoToArtist = openArtist,
                   onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) }
              )
          }
          entry<PlaylistDetailsRoute> { route ->
              com.example.xargoosh.feature.playlists.presentation.PlaylistDetailsScreen(
                  playlistId = route.playlistId,
                  playlistName = route.playlistName,
                   onBackClick = { backStack.removeLastOrNull() },
                   playerViewModel = playerViewModel,
                   onAddToPlaylist = { trackForPlaylist = it },
                   onGoToAlbum = { backStack.add(AlbumDetailsRoute(it)) },
                    onGoToArtist = openArtist,
                   onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) }
              )
          }
          entry<SmartPlaylistRoute> { route ->
              val tracks by homeViewModel.allTracks.collectAsStateWithLifecycle()
              com.example.xargoosh.feature.playlists.presentation.SmartPlaylistDetailsScreen(
                  kind = route.kind,
                  allTracks = tracks,
                  onBackClick = { backStack.removeLastOrNull() },
                  playerViewModel = playerViewModel,
                  onAddToPlaylist = { trackForPlaylist = it },
                  onGoToAlbum = { backStack.add(AlbumDetailsRoute(it)) },
                  onGoToArtist = openArtist,
                  onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) }
              )
          }
          entry<GenreDetailsRoute> { route ->
              val tracks by homeViewModel.allTracks.collectAsStateWithLifecycle()
              val genreTracks = remember(tracks, route.genreName) {
                  tracks.filter { com.example.xargoosh.feature.library.presentation.GenreNames.contains(it.genre, route.genreName) }
              }
              com.example.xargoosh.feature.playlists.presentation.GenreDetailsScreen(
                  genreName = route.genreName,
                  tracks = genreTracks,
                  onBackClick = { backStack.removeLastOrNull() },
                  onTrackClick = { index -> playerViewModel.playQueue(genreTracks, index) },
                  onAlbumClick = { album, artist -> backStack.add(AlbumDetailsRoute(album, artist)) },
                  onShuffleAll = { if (genreTracks.isNotEmpty()) playerViewModel.playShuffled(genreTracks) },
                  onPlayAll = { if (genreTracks.isNotEmpty()) playerViewModel.playQueue(genreTracks, 0) },
                  onPlayNext = playerViewModel::playNext,
                  onAddToQueue = playerViewModel::addToQueue,
                  onAddToPlaylist = { trackForPlaylist = it },
                  onGoToArtist = openArtist,
                  onEditTags = { track -> metadataEditorViewModel.loadTrack(track); backStack.add(MetadataEditorRoute) },
                  onDeleteTrack = deleteTrackFromDevice
              )
          }
          entry<FullscreenVisualizerRoute> {
              com.example.xargoosh.core.visualizer.effects.FullscreenVisualizerScreen(
                  viewModel = playerViewModel,
                  onBack = { backStack.removeLastOrNull() }
              )
          }
          entry<VisualizerSettingsRoute> {
              com.example.xargoosh.core.visualizer.effects.VisualizerSettingsScreen(
                  onNavigateBack = { backStack.removeLastOrNull() },
                  settings = playerViewModel.visualizerSettings,
                  viewModel = playerViewModel
              )
          }
          entry<MusicRecognitionRoute> {
              val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
              com.example.xargoosh.feature.recognition.MusicRecognitionScreen(
                  onBack = { backStack.removeLastOrNull() },
                  isPlaybackActive = isPlaying,
                  onPausePlayback = playerViewModel::playPause
              )
          }
        }
              )
  }
}
