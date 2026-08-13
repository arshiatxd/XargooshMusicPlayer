package com.example.xargoosh.feature.playlists.presentation

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroButton as Button
import com.example.xargoosh.core.components.surface.AeroFloatingActionButton as FloatingActionButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xargoosh.core.components.surface.GlassSurface
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.design.themes.glassmorphism
import androidx.compose.ui.unit.sp
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.presentation.components.AudioThumbnail
import com.example.xargoosh.presentation.components.TrackOptionsMenu
import com.example.xargoosh.feature.library.presentation.TrackListItem
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailsScreen(
    genreName: String,
    tracks: List<MusicTrack>,
    onBackClick: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onAlbumClick: (String, String?) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit = {},
    onAddToQueue: (MusicTrack) -> Unit = {},
    onAddToPlaylist: (MusicTrack) -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onEditTags: (MusicTrack) -> Unit = {},
    onDeleteTrack: (MusicTrack) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val albums = remember(tracks) {
        tracks.groupBy { it.album to (com.example.xargoosh.utils.ArtistCredits.names(it.artist).firstOrNull() ?: it.artist) }.entries.toList()
    }

    if (showOptionsMenu && selectedTrack != null) {
        TrackOptionsMenu(
            track = selectedTrack!!,
            onDismiss = { showOptionsMenu = false },
            onPlayNext = { onPlayNext(selectedTrack!!) },
            onAddToQueue = { onAddToQueue(selectedTrack!!) },
            onAddToPlaylist = { onAddToPlaylist(selectedTrack!!) },
            onGoToAlbum = { onAlbumClick(selectedTrack!!.album, com.example.xargoosh.utils.ArtistCredits.names(selectedTrack!!.artist).firstOrNull()) },
            onGoToArtist = { onGoToArtist(selectedTrack!!.artist) },
            onEditTags = { onEditTags(selectedTrack!!) },
            onChangeCover = { onEditTags(selectedTrack!!) },
            onDeleteFromDevice = { onDeleteTrack(selectedTrack!!) }
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
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = XargooshTheme.colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

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
                            Text(
                                text = genreName.take(1).uppercase(),
                                color = XargooshTheme.colors.primary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold)

                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = genreName,
                            color = XargooshTheme.colors.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)

                    }
                }

                if (albums.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums.size) { index ->
                                val album = albums[index]
                                val firstTrack = album.value.first()
                                Column(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clickable { onAlbumClick(album.key.first, album.key.second) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(XargooshTheme.colors.surfaceVariant)
                                    ) {
                                        AudioThumbnail(
                                            uri = firstTrack.uri,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.key.first.ifEmpty { stringResource(R.string.unknown_album) },
                                        color = XargooshTheme.colors.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = pluralStringResource(R.plurals.song_count, album.value.size, album.value.size),
                                        color = XargooshTheme.colors.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )

                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                            onClick = onShuffleAll,
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
                            onClick = onPlayAll,
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

                items(tracks.size, key = { tracks[it].uri }) { index ->
                    val track = tracks[index]
                    TrackListItem(
                        track = track,
                        index = index,
                        onClick = { onTrackClick(index) },
                        onEditClick = {
                            selectedTrack = track
                            showOptionsMenu = true
                        }
                    )

                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = listState.firstVisibleItemIndex > 5,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.size(52.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = XargooshTheme.colors.primary,
                    contentColor = XargooshTheme.colors.onPrimary
                ) {
                    Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = stringResource(R.string.move_to_top))
                }
            }

        }
    }
}
}
