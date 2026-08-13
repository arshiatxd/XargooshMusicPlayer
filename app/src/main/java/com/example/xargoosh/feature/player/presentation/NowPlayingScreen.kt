package com.example.xargoosh.feature.player.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.example.xargoosh.core.design.themes.AeroBgDeep
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.presentation.components.AudioThumbnail
import com.example.xargoosh.domain.models.LyricLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.xargoosh.domain.lyrics.LyricsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onNavigateToQueue: () -> Unit = {},
    onAddToPlaylist: (com.example.xargoosh.domain.models.MusicTrack) -> Unit = {},
    onEditMetadata: (com.example.xargoosh.domain.models.MusicTrack) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onTrackDeleted: (String) -> Unit = {},
    onNavigateFullscreen: () -> Unit = {},
    onNavigateEqualizer: () -> Unit = {},
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentTrackFavorite.collectAsStateWithLifecycle()

    var showLyrics by remember { mutableStateOf(false) }
    var selectedBottomSheetTab by rememberSaveable { mutableIntStateOf(1) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var syncedLyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var lyricsLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val visualizerState by viewModel.visualizerState.collectAsStateWithLifecycle()
    val visualizerStyle by viewModel.currentVisualizerStyle.collectAsStateWithLifecycle()
    val visualizerSettings = viewModel.visualizerSettings
    val vizEnabled by visualizerSettings.enabled.collectAsStateWithLifecycle(initialValue = false)
    val blurStrength by visualizerSettings.blurStrength.collectAsStateWithLifecycle(initialValue = 0.5f)
    val useTabbedPlayerLayout by visualizerSettings.useTabbedPlayerLayout.collectAsStateWithLifecycle(initialValue = false)
    val renderSize by visualizerSettings.renderSize.collectAsStateWithLifecycle(initialValue = 1f)
    val placementBehindLyrics by visualizerSettings.placementBehindLyrics.collectAsStateWithLifecycle(initialValue = true)

    val context = LocalContext.current
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.retryVisualizerCapture()
        } else scope.launch { visualizerSettings.setEnabled(false) }
    }
    LaunchedEffect(vizEnabled) {
        if (vizEnabled && androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            visualizerPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    var pendingDeleteUri by remember { mutableStateOf<String?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) pendingDeleteUri?.let(onTrackDeleted)
        pendingDeleteUri = null
    }
    LaunchedEffect(currentTrack) {
        val track = currentTrack ?: return@LaunchedEffect
        lyricsLoading = true
        syncedLyrics = emptyList()
        try {
            syncedLyrics = LyricsRepository.getLyrics(context, track).orEmpty()
        } finally {
            lyricsLoading = false
        }
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            XargooshTheme.colors.background,
            XargooshTheme.colors.secondary.copy(alpha = 0.3f),
            XargooshTheme.colors.background
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Content
        if (vizEnabled) {
            com.example.xargoosh.core.visualizer.effects.LyricsVisualizerBg(
                albumArtUri = currentTrack?.uri,
                visualizerState = visualizerState,
                style = visualizerStyle,
                blurStrength = blurStrength,
                enabled = true,
                showVisualizer = (if (useTabbedPlayerLayout) selectedBottomSheetTab == 1 else showLyrics) && placementBehindLyrics,
                renderSize = renderSize,
                useLightContrast = XargooshTheme.colors.background.luminance() > 0.5f
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(bgBrush))
        }

        // 2. Main Layout Structure
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            )
        )
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val sheetPeekHeight = screenHeightDp * if (screenHeightDp < 600.dp) 0.2f else 0.34f
        val density = LocalDensity.current
        val partialSheetOffsetPx = with(density) { (screenHeightDp - sheetPeekHeight).toPx() }
        var sheetExpansion by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(scaffoldState.bottomSheetState, partialSheetOffsetPx) {
            snapshotFlow {
                runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrNull() to scaffoldState.bottomSheetState.currentValue
            }.collect { (offset, value) ->
                if (offset != null && partialSheetOffsetPx > 0f) {
                    sheetExpansion = if (value == SheetValue.Expanded) 1f
                    else (1f - offset / partialSheetOffsetPx).coerceIn(0f, 1f)
                }
            }
        }

        if (useTabbedPlayerLayout) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = sheetPeekHeight,
                containerColor = Color.Transparent,
                sheetContainerColor = Color.Transparent,
                sheetTonalElevation = 0.dp,
                sheetShadowElevation = 0.dp,
                sheetContent = {
                    PlayerBottomSheetContent(
                        viewModel = viewModel,
                        currentTrack = currentTrack,
                        lyrics = syncedLyrics,
                        lyricsLoading = lyricsLoading,
                        currentPosition = currentPosition,
                        onNavigateToQueue = onNavigateToQueue,
                        expansionProgress = sheetExpansion,
                        selectedTab = selectedBottomSheetTab,
                        onSelectedTabChange = { selectedBottomSheetTab = it }
                    )
                }
            ) { paddingValues ->
                PlayerMainContent(
                    paddingValues = paddingValues,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    visualizerState = visualizerState,
                    visualizerStyle = visualizerStyle,
                    visualizerEnabled = vizEnabled,
                    visualizerSize = renderSize,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = isFavorite,
                    showLyrics = false,
                    useTabbedPlayerLayout = true,
                    lyricsLoading = lyricsLoading,
                    syncedLyrics = syncedLyrics,
                    viewModel = viewModel,
                    onBack = onBack,
                    onToggleLyrics = { showLyrics = !showLyrics },
                    onShowOptions = { showOptionsMenu = true },
                    onNavigateFullscreen = onNavigateFullscreen,
                    onNavigateEqualizer = onNavigateEqualizer,
                    onAddToPlaylist = onAddToPlaylist,
                    onNavigateToQueue = onNavigateToQueue,
                    onNavigateToAlbum = { currentTrack?.album?.let(onNavigateToAlbum) }
                )
            }
        } else {
            PlayerMainContent(
                paddingValues = PaddingValues(0.dp),
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                visualizerState = visualizerState,
                visualizerStyle = visualizerStyle,
                visualizerEnabled = vizEnabled,
                visualizerSize = renderSize,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                isFavorite = isFavorite,
                showLyrics = showLyrics,
                useTabbedPlayerLayout = false,
                lyricsLoading = lyricsLoading,
                syncedLyrics = syncedLyrics,
                viewModel = viewModel,
                onBack = onBack,
                onToggleLyrics = { showLyrics = !showLyrics },
                onShowOptions = { showOptionsMenu = true },
                onNavigateFullscreen = onNavigateFullscreen,
                onNavigateEqualizer = onNavigateEqualizer,
                onAddToPlaylist = onAddToPlaylist,
                onNavigateToQueue = onNavigateToQueue,
                onNavigateToAlbum = { currentTrack?.album?.let(onNavigateToAlbum) }
            )
        }

        // 3. Options Menu Overlay
        if (showOptionsMenu && currentTrack != null) {
            com.example.xargoosh.presentation.components.TrackOptionsMenu(
                track = currentTrack!!,
                onDismiss = { showOptionsMenu = false },
                onPlayNext = { showOptionsMenu = false; viewModel.playNext(currentTrack!!) },
                onAddToQueue = { showOptionsMenu = false; viewModel.addToQueue(currentTrack!!) },
                onAddToPlaylist = { showOptionsMenu = false; onAddToPlaylist(currentTrack!!) },
                onEditTags = { showOptionsMenu = false; onEditMetadata(currentTrack!!) },
                onChangeCover = { showOptionsMenu = false; onEditMetadata(currentTrack!!) },
                onGoToAlbum = { showOptionsMenu = false; onNavigateToAlbum(currentTrack!!.album) },
                onGoToArtist = { showOptionsMenu = false; onNavigateToArtist(currentTrack!!.artist) },
                onSetAsRingtone = { showOptionsMenu = false; com.example.xargoosh.utils.TrackUtils.setAsRingtone(context, currentTrack!!.uri) },
                onDeleteFromDevice = {
                    val track = currentTrack!!
                    showOptionsMenu = false
                    com.example.xargoosh.utils.TrackUtils.deleteTrack(
                        context,
                        track.uri,
                        onIntentSenderRequired = { request -> pendingDeleteUri = track.uri; deleteLauncher.launch(request) },
                        onSuccess = { onTrackDeleted(track.uri) }
                    )
                }
            )
        }
    }
}

@Composable
private fun PlayerMainContent(
    paddingValues: PaddingValues,
    currentTrack: com.example.xargoosh.domain.models.MusicTrack?,
    isPlaying: Boolean,
    currentPosition: Long,
    visualizerState: com.example.xargoosh.domain.visualizer.VisualizerState,
    visualizerStyle: com.example.xargoosh.domain.visualizer.VisualizerStyle,
    visualizerEnabled: Boolean,
    visualizerSize: Float,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    showLyrics: Boolean,
    useTabbedPlayerLayout: Boolean,
    lyricsLoading: Boolean,
    syncedLyrics: List<LyricLine>,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onToggleLyrics: () -> Unit,
    onShowOptions: () -> Unit,
    onNavigateFullscreen: () -> Unit,
    onNavigateEqualizer: () -> Unit,
    onAddToPlaylist: (com.example.xargoosh.domain.models.MusicTrack) -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToAlbum: () -> Unit
) {
    val compactHeight = LocalConfiguration.current.screenHeightDp < 500
    val contentScrollState = rememberScrollState()
    var artworkExpanded by remember(currentTrack?.uri) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 20.dp)
            .padding(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp + paddingValues.calculateBottomPadding()
            )
            .then(if (compactHeight && !showLyrics) Modifier.verticalScroll(contentScrollState) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.xargoosh.core.components.surface.GlassSurface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.subtle,
                color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.surface.copy(alpha = 0.2f),
                borderColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.glassBorder,
                onClick = { if (showLyrics) onToggleLyrics() else onBack() }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.back), tint = XargooshTheme.colors.onBackground)
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showLyrics) stringResource(R.string.lyrics).uppercase() else stringResource(R.string.now_playing).uppercase(),
                    color = XargooshTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
                if (!showLyrics) {
                    Text(
                        text = currentTrack?.album?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: stringResource(R.string.brand_xargoosh),
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface,
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onNavigateToAlbum)
                    )
                }
            }
            
            IconButton(onClick = onShowOptions, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more), tint = XargooshTheme.colors.onBackground)
            }
        }

        if (showLyrics) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    lyricsLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = XargooshTheme.colors.primary) }
                    syncedLyrics.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_lyrics_available), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 16.sp) }
                    else -> {
                        LyricsOverlay(lyrics = syncedLyrics, currentPositionMs = currentPosition, onSeekTo = { viewModel.seekTo(it) })
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            
            BoxWithConstraints(
                modifier = if (compactHeight) {
                    Modifier.fillMaxWidth().height(180.dp)
                } else {
                    Modifier.fillMaxWidth().weight(1f)
                },
                contentAlignment = Alignment.Center
            ) {
                val size = minOf(maxWidth * 0.96f, maxHeight)
                Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                    if (visualizerEnabled) {
                        com.example.xargoosh.core.visualizer.effects.VisualizerCanvas(
                            state = visualizerState,
                            style = visualizerStyle,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            renderSize = visualizerSize
                        )
                    }
                    val artworkFraction by animateFloatAsState(
                        targetValue = when {
                            useTabbedPlayerLayout && artworkExpanded -> 0.98f
                            useTabbedPlayerLayout -> 0.8f
                            else -> 0.72f
                        },
                        animationSpec = tween(240),
                        label = "playerArtworkSize"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize(artworkFraction)
                            .shadow(28.dp, RoundedCornerShape(22.dp), ambientColor = XargooshTheme.colors.glow)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, XargooshTheme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                            .clickable(enabled = useTabbedPlayerLayout && currentTrack != null) { artworkExpanded = !artworkExpanded }
                    ) {
                        if (currentTrack != null) {
                            AudioThumbnail(uri = currentTrack.uri, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp), tint = XargooshTheme.colors.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
            modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: stringResource(R.string.no_track_selected),
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleLarge,
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTrack?.artist ?: stringResource(R.string.unknown_artist),
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                com.example.xargoosh.core.components.surface.GlassSurface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.subtle,
                    color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.surface.copy(alpha = 0.2f),
                    borderColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.glassBorder,
                    onClick = viewModel::toggleFavorite
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                            tint = if (isFavorite) com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary else com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        PlayerTimeSlider(currentPosition = currentPosition, durationMs = currentTrack?.durationMs ?: 1L, onSeek = { viewModel.seekTo(it) })
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val showOptionalActions = maxWidth >= 312.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            if (showOptionalActions) {
                IconButton(onClick = onNavigateToAlbum, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Album, contentDescription = stringResource(R.string.open_album), tint = XargooshTheme.colors.onSurface, modifier = Modifier.size(22.dp))
                }
            }
            val shuffleColor by animateColorAsState(targetValue = if (shuffleEnabled) com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary else com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, animationSpec = tween(300), label = "shuffleColor")
            IconButton(onClick = { viewModel.toggleShuffle() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Shuffle, contentDescription = stringResource(if (shuffleEnabled) R.string.disable_shuffle else R.string.enable_shuffle), tint = shuffleColor, modifier = Modifier.size(24.dp))
            }

            IconButton(onClick = { viewModel.skipToPrevious() }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous), tint = XargooshTheme.colors.onBackground, modifier = Modifier.size(32.dp))
            }
            
            com.example.xargoosh.core.components.surface.GlassSurface(
                modifier = Modifier
                    .size(64.dp),
                shape = CircleShape,
                color = XargooshTheme.colors.primary,
                borderColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary,
                blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.high,
                elevation = com.example.xargoosh.core.design.themes.XargooshTheme.elevation.level5,
                onClick = viewModel::playPause
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play), tint = XargooshTheme.colors.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            
            IconButton(onClick = { viewModel.skipToNext() }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground, modifier = Modifier.size(32.dp))
            }
            
            val loopColor by animateColorAsState(targetValue = if (repeatMode != 0) com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary else com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, animationSpec = tween(300), label = "loopColor")
            IconButton(onClick = { viewModel.toggleLoop() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = when (repeatMode) {
                        1 -> stringResource(R.string.switch_repeat_one)
                        2 -> stringResource(R.string.disable_repeat)
                        else -> stringResource(R.string.enable_repeat_all)
                    },
                    tint = loopColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (showOptionalActions) {
                IconButton(onClick = onNavigateToQueue, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.open_queue), tint = XargooshTheme.colors.onSurface, modifier = Modifier.size(22.dp))
                }
            }
        }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (!useTabbedPlayerLayout) {
                TextButton(onClick = onToggleLyrics) { Icon(Icons.Default.Lyrics, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.lyrics)) }
            }
            TextButton(onClick = onNavigateEqualizer) { Icon(Icons.Default.GraphicEq, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.equalizer)) }
            TextButton(onClick = onNavigateFullscreen) { Icon(Icons.Default.Fullscreen, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.visualizer)) }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBottomSheetContent(
    viewModel: PlayerViewModel,
    currentTrack: com.example.xargoosh.domain.models.MusicTrack?,
    lyrics: List<LyricLine>,
    lyricsLoading: Boolean,
    currentPosition: Long,
    onNavigateToQueue: () -> Unit,
    expansionProgress: Float,
    selectedTab: Int,
    onSelectedTabChange: (Int) -> Unit
) {
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val progress = expansionProgress.coerceIn(0f, 1f)
    val surfaceAlpha = androidx.compose.ui.util.lerp(0.55f, 1f, progress)
    val backgroundAlpha = androidx.compose.ui.util.lerp(0.7f, 1f, progress)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(1f)
            .clip(sheetShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        XargooshTheme.colors.surface.copy(alpha = surfaceAlpha),
                        XargooshTheme.colors.background.copy(alpha = backgroundAlpha)
                    )
                )
            )
    ) {
        Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface)

        val tabs = listOf(
            stringResource(R.string.up_next),
            stringResource(R.string.lyrics),
            stringResource(R.string.related)
        )
        Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelectedTabChange(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (isSelected) XargooshTheme.colors.primary else XargooshTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(Modifier.width(if (isSelected) 54.dp else 0.dp).height(3.dp).background(XargooshTheme.colors.primary))
                }
            }
        }

        val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle(initialValue = emptyList())
        val currentIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
        val libraryTracks by viewModel.libraryTracks.collectAsStateWithLifecycle()
        val upNext = remember(currentQueue, currentIndex) { currentQueue.drop((currentIndex + 1).coerceAtLeast(0)) }
        val related = remember(libraryTracks, currentTrack) {
            val genres = com.example.xargoosh.feature.library.presentation.GenreNames.from(currentTrack?.genre).map(String::lowercase).toSet()
            if (genres.isEmpty()) emptyList() else libraryTracks.filter { candidate ->
                candidate.uri != currentTrack?.uri && com.example.xargoosh.feature.library.presentation.GenreNames.from(candidate.genre)
                    .any { it.lowercase() in genres }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> if (upNext.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.nothing_else_in_queue), color = XargooshTheme.colors.onSurfaceVariant) }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        item {
                            TextButton(onClick = onNavigateToQueue, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.open_full_queue))
                            }
                        }
                        itemsIndexed(upNext, key = { _, item -> item.id }) { _, item ->
                            PlayerListRow(item.track, onClick = { viewModel.playQueueItem(item.id) })
                        }
                    }
                }
                1 -> when {
                    lyricsLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = XargooshTheme.colors.primary) }
                    lyrics.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_lyrics_available), color = XargooshTheme.colors.onSurfaceVariant) }
                    else -> LyricsOverlay(
                        lyrics = lyrics,
                        currentPositionMs = currentPosition,
                        onSeekTo = viewModel::seekTo,
                        inactiveColor = XargooshTheme.colors.onSurface.copy(alpha = 0.86f)
                    )
                }
                else -> if (related.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_matching_genre_tracks), color = XargooshTheme.colors.onSurfaceVariant) }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(related, key = { _, track -> track.uri }) { index, track ->
                            PlayerListRow(track, onClick = { viewModel.playQueue(related, index) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerListRow(track: com.example.xargoosh.domain.models.MusicTrack, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AudioThumbnail(track.uri, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = XargooshTheme.colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = XargooshTheme.colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MiniPlayer(
    track: com.example.xargoosh.domain.models.MusicTrack, isPlaying: Boolean, onPlayPause: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    com.example.xargoosh.core.components.surface.GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.medium, vertical = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.small)
            .clickable { onClick() },
        shape = com.example.xargoosh.core.design.themes.XargooshTheme.shapes.extraLarge,
        blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.high,
        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.surface.copy(alpha = 0.4f),
        borderColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.glassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(com.example.xargoosh.core.design.themes.XargooshTheme.colors.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(track.title.take(1).uppercase(), color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = track.title, color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium, maxLines = 1)
                Text(text = track.artist, color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary.copy(alpha = 0.2f))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary)
            }
        }
    }
}


@Composable
fun PlayerTimeSlider(currentPosition: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentPosition, isDragging) { if (!isDragging) sliderPos = currentPosition.toFloat() }
    val durationSafe = durationMs.coerceAtLeast(1L)
    Slider(
        value = sliderPos.coerceIn(0f, durationSafe.toFloat()), 
        onValueChange = { isDragging = true; sliderPos = it }, 
        onValueChangeFinished = { isDragging = false; onSeek(sliderPos.toLong()) }, 
        valueRange = 0f..durationSafe.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary, 
            activeTrackColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary, 
            inactiveTrackColor = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface.copy(alpha=0.3f)
        ), 
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.medium), horizontalArrangement = Arrangement.SpaceBetween) {
        val pos = if (isDragging) sliderPos.toLong() else currentPosition
        Text(stringResource(R.string.playback_time, pos / 60000, (pos % 60000) / 1000), color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.labelMedium)
        Text(stringResource(R.string.playback_time, durationMs / 60000, (durationMs % 60000) / 1000), color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.labelMedium)
    }
}
