package com.example.xargoosh.feature.playlists.presentation

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroButton as Button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xargoosh.core.components.surface.GlassSurface
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.design.themes.glassmorphism
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.presentation.components.TrackOptionsMenu
import com.example.xargoosh.feature.library.presentation.HomeViewModel
import com.example.xargoosh.feature.library.presentation.SmartPlaylistKind
import com.example.xargoosh.feature.library.presentation.selectTracks
import com.example.xargoosh.feature.library.presentation.TrackListItem
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Int,
    playlistName: String,
    onBackClick: () -> Unit,
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    playerViewModel: PlayerViewModel,
    onAddToPlaylist: (MusicTrack) -> Unit = {},
    onGoToAlbum: (String) -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onEditTags: (MusicTrack) -> Unit = {}
) {
    val tracks by homeViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
    CollectionDetailsBody(
        collectionName = playlistName,
        tracks = tracks,
        onBackClick = onBackClick,
        playerViewModel = playerViewModel,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        onEditTags = onEditTags
    )
}

@Composable
fun SmartPlaylistDetailsScreen(
    kind: SmartPlaylistKind,
    allTracks: List<MusicTrack>,
    onBackClick: () -> Unit,
    playerViewModel: PlayerViewModel,
    onAddToPlaylist: (MusicTrack) -> Unit = {},
    onGoToAlbum: (String) -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onEditTags: (MusicTrack) -> Unit = {}
) {
    val tracks = remember(kind, allTracks) { kind.selectTracks(allTracks) }
    val title = when (kind) {
        SmartPlaylistKind.RECENTLY_ADDED -> stringResource(R.string.recently_added)
        SmartPlaylistKind.MOST_PLAYED -> stringResource(R.string.most_played)
        SmartPlaylistKind.NEVER_PLAYED -> stringResource(R.string.never_played)
    }
    CollectionDetailsBody(
        collectionName = title,
        tracks = tracks,
        onBackClick = onBackClick,
        playerViewModel = playerViewModel,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        onEditTags = onEditTags
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionDetailsBody(
    collectionName: String,
    tracks: List<MusicTrack>,
    onBackClick: () -> Unit,
    playerViewModel: PlayerViewModel,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onEditTags: (MusicTrack) -> Unit
) {
    val listState = rememberLazyListState()
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    if (showOptionsMenu && selectedTrack != null) {
        TrackOptionsMenu(
            track = selectedTrack!!,
            onDismiss = { showOptionsMenu = false },
            onPlayNext = { playerViewModel.playNext(selectedTrack!!) },
            onAddToQueue = { playerViewModel.addToQueue(selectedTrack!!) },
            onAddToPlaylist = { onAddToPlaylist(selectedTrack!!) },
            onGoToAlbum = { onGoToAlbum(selectedTrack!!.album) },
            onGoToArtist = { onGoToArtist(selectedTrack!!.artist) },
            onEditTags = { onEditTags(selectedTrack!!) },
            onChangeCover = { onEditTags(selectedTrack!!) }
        )

    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(
                XargooshTheme.colors.surface,
                XargooshTheme.colors.background
            )
        )
    )) {
    Scaffold(
        topBar = {
            com.example.xargoosh.core.components.surface.GlassSurface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = XargooshTheme.colors.surface.copy(alpha = 0.4f),
                borderColor = Color.Transparent
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = XargooshTheme.colors.onBackground)
                    }
                }
            }

        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .glassmorphism(cornerRadius = 16.dp, blurRadius = 15.dp, lightMode = false),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = collectionName,
                            color = XargooshTheme.colors.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)

                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.song_count, tracks.size, tracks.size),
                            color = XargooshTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp)

                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playShuffled(tracks)
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, XargooshTheme.colors.onSurfaceVariant.copy(alpha=0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = XargooshTheme.colors.onBackground)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.shuffle), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playQueue(tracks, 0)
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XargooshTheme.colors.primary,
                                contentColor = XargooshTheme.colors.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.play), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (tracks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_tracks_playlist), color = XargooshTheme.colors.onSurfaceVariant)
                        }
                    }
                } else {
                    items(tracks.size) { index ->
                        val track = tracks[index]
                        TrackListItem(
                            track = track,
                            index = index,
                            onClick = { playerViewModel.playQueue(tracks, index) },
                            onEditClick = {
                                selectedTrack = track
                                showOptionsMenu = true
                            })

                    }
                }
            }

            if (tracks.isNotEmpty()) {

            }
        }
    }
}
}
