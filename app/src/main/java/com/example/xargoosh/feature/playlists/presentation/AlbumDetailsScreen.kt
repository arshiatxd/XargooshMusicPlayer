package com.example.xargoosh.feature.playlists.presentation

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroButton as Button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xargoosh.core.components.surface.GlassSurface
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.design.themes.glassmorphism
import androidx.compose.ui.unit.sp
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.presentation.components.AudioThumbnail
import com.example.xargoosh.presentation.components.TrackOptionsMenu
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsScreen(
    albumName: String,
    tracks: List<MusicTrack>,
    onBackClick: () -> Unit,
    onTrackClick: (Int) -> Unit,
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
    val artistName = tracks.firstOrNull()?.artist ?: stringResource(R.string.unknown_artist)
    val firstTrackUri = tracks.firstOrNull()?.uri ?: ""

    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    if (showOptionsMenu && selectedTrack != null) {
        TrackOptionsMenu(
            track = selectedTrack!!,
            onDismiss = { showOptionsMenu = false },
            onPlayNext = { onPlayNext(selectedTrack!!) },
            onAddToQueue = { onAddToQueue(selectedTrack!!) },
            onAddToPlaylist = { onAddToPlaylist(selectedTrack!!) },
            onGoToArtist = { onGoToArtist(selectedTrack!!.artist) },
            onEditTags = { onEditTags(selectedTrack!!) },
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
        containerColor = XargooshTheme.colors.background
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(XargooshTheme.colors.surfaceVariant)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        AudioThumbnail(
                            uri = firstTrackUri,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = albumName,
                        color = XargooshTheme.colors.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = artistName,
                        color = XargooshTheme.colors.onSurfaceVariant,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

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
                            contentColor = XargooshTheme.colors.onPrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.play), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(tracks.size) { index ->
                val track = tracks[index]
                TrackListItem(
                    track = track,
                    index = index + 1, 
                    onClick = { onTrackClick(index) },
                    onOptionsClick = {
                        selectedTrack = track
                        showOptionsMenu = true
                    })

            }
        }
    }
}
}

@Composable
fun TrackListItem(
    track: MusicTrack,
    index: Int,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = index.toString(),
            color = XargooshTheme.colors.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp))

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .glassmorphism(cornerRadius = 8.dp, blurRadius = 10.dp, lightMode = false)
        ) {
            AudioThumbnail(uri = track.uri, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = XargooshTheme.colors.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artist} - ${track.album}",
                    color = XargooshTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onOptionsClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options), tint = XargooshTheme.colors.onSurfaceVariant)
        }
    }
}
