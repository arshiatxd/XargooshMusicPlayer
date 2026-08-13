package com.example.xargoosh.feature.library.presentation

import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroButton as Button
import com.example.xargoosh.core.components.surface.AeroFloatingActionButton as FloatingActionButton

import com.example.xargoosh.core.design.themes.AppTheme
import com.example.xargoosh.core.design.themes.ThemeManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import java.util.Locale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.presentation.components.AudioThumbnail
import kotlinx.coroutines.launch
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.feature.player.presentation.PlayerViewModel

private val avatarColors = listOf(
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5),
    Color(0xFF00897B), Color(0xFF43A047), Color(0xFFFF6F00),
    Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF00ACC1),
    Color(0xFF6D4C41), Color(0xFF546E7A), Color(0xFF7B1FA2)
)

@Composable
private fun localizedPlaylistName(name: String): String =
    if (name == "Favorites") stringResource(R.string.favorites) else name

private enum class RenameGroupType { ALBUM, ARTIST }
private data class GroupRenameRequest(
    val type: RenameGroupType,
    val oldName: String,
    val tracks: List<MusicTrack>,
    val newName: String = "",
    val startIndex: Int = 0,
    val completed: Int = 0
)

fun avatarColorFor(name: String): Color {
    val idx = (name.firstOrNull()?.code ?: 0) % avatarColors.size
    return avatarColors[idx]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToPlayer: (List<MusicTrack>, Int) -> Unit = { _, _ -> },
    onEditMetadata: (MusicTrack) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusicRecognition: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToFolder: (com.example.xargoosh.domain.models.MusicFolder) -> Unit = {},
    onNavigateToAlbum: (String, String?) -> Unit = { _, _ -> },
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (Int, String) -> Unit = { _, _ -> },
    onNavigateToSmartPlaylist: (SmartPlaylistKind) -> Unit = {},
    onPlayNext: (MusicTrack) -> Unit = {},
    onAddToQueue: (MusicTrack) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSort by viewModel.currentSort.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val genreFilter by viewModel.genreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val albumGroups by viewModel.albumGroups.collectAsStateWithLifecycle()
    val artistGroups by viewModel.artistGroups.collectAsStateWithLifecycle()
    val genreGroups by viewModel.genreGroups.collectAsStateWithLifecycle()
    val albumSort by viewModel.albumSort.collectAsStateWithLifecycle()
    val artistSort by viewModel.artistSort.collectAsStateWithLifecycle()
    val genreSort by viewModel.genreSort.collectAsStateWithLifecycle()
    val playlistSort by viewModel.playlistSort.collectAsStateWithLifecycle()
    val folderSort by viewModel.folderSort.collectAsStateWithLifecycle()
    val albumLayout by viewModel.albumLayout.collectAsStateWithLifecycle()
    val libraryFolders by viewModel.explicitFolders.collectAsStateWithLifecycle()

    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    

    val tabs = listOf(
        stringResource(R.string.tab_songs),
        stringResource(R.string.tab_playlists),
        stringResource(R.string.tab_albums),
        stringResource(R.string.tab_artists),
        stringResource(R.string.folders),
        stringResource(R.string.genres)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val tabListState = rememberLazyListState()
    val songListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val albumGridState = rememberLazyGridState()
    val artistListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    val genreListState = rememberLazyListState()
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var selectedTrackUris by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedAlbumKeys by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedArtistNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedGenreNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedFolderIds by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var showSelectionPlaylistDialog by remember { mutableStateOf(false) }
    var showSelectionDeleteDialog by remember { mutableStateOf(false) }
    var showSelectionCreatePlaylist by remember { mutableStateOf(false) }
    var selectionPlaylistName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val renameCancelledMessage = stringResource(R.string.rename_cancelled)
    val folderAccessFailedMessage = stringResource(R.string.folder_access_failed)
    val tooManyFilesRenameMessage = stringResource(R.string.too_many_files_rename)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectionCount = when {
        selectedTrackUris.isNotEmpty() -> selectedTrackUris.size
        selectedAlbumKeys.isNotEmpty() -> selectedAlbumKeys.size
        selectedArtistNames.isNotEmpty() -> selectedArtistNames.size
        selectedGenreNames.isNotEmpty() -> selectedGenreNames.size
        else -> selectedFolderIds.size
    }
    val hasSelection = selectionCount > 0
    val selectedActionUris = remember(
        selectedTrackUris, selectedAlbumKeys, selectedArtistNames, selectedGenreNames,
        selectedFolderIds, albumGroups, artistGroups, genreGroups, libraryFolders, allTracks
    ) {
        when {
            selectedTrackUris.isNotEmpty() -> selectedTrackUris
            selectedAlbumKeys.isNotEmpty() -> albumGroups.filter { it.key in selectedAlbumKeys }.flatMap { it.tracks }.map(MusicTrack::uri).toSet()
            selectedArtistNames.isNotEmpty() -> artistGroups.filter { it.name in selectedArtistNames }.flatMap { it.tracks }.map(MusicTrack::uri).toSet()
            selectedGenreNames.isNotEmpty() -> genreGroups.filter { it.name in selectedGenreNames }.flatMap { it.tracks }.map(MusicTrack::uri).toSet()
            selectedFolderIds.isNotEmpty() -> {
                val folderKeys = libraryFolders.filter { it.id in selectedFolderIds }.map { it.uriString }.toSet()
                allTracks.filter { it.folderPath in folderKeys }.map(MusicTrack::uri).toSet()
            }
            else -> emptySet()
        }
    }
    val clearSelection = {
        selectedTrackUris = emptySet()
        selectedAlbumKeys = emptySet()
        selectedArtistNames = emptySet()
        selectedGenreNames = emptySet()
        selectedFolderIds = emptySet()
    }
    val restoreFailedTracks: (Collection<String>) -> Unit = { uris ->
        selectedTrackUris = selectedTrackUris + uris
        if (pagerState.currentPage != 0) scope.launch { pagerState.scrollToPage(0) }
    }
    var renameRequest by remember { mutableStateOf<GroupRenameRequest?>(null) }
    var approvedRename by remember { mutableStateOf<GroupRenameRequest?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var isRenaming by remember { mutableStateOf(false) }
    var pendingWriteRequest by remember { mutableStateOf<GroupRenameRequest?>(null) }
    var pendingBatchDelete by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var batchDeleteQueue by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var deleteQueue by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var deletingUri by rememberSaveable { mutableStateOf<String?>(null) }
    val batchDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingBatchDelete.forEach { uri ->
                viewModel.deleteTrack(uri)
                playerViewModel.removeTrackFromQueue(uri)
            }
        } else {
            restoreFailedTracks(pendingBatchDelete)
        }
        pendingBatchDelete = arrayListOf()
    }
    val sequentialDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val uri = deletingUri
        if (uri != null && result.resultCode == android.app.Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val deleted = runCatching { context.contentResolver.delete(android.net.Uri.parse(uri), null, null) }.getOrDefault(0)
                if (deleted > 0) {
                        viewModel.deleteTrack(uri)
                        playerViewModel.removeTrackFromQueue(uri)
                        deletingUri = null
                } else {
                    restoreFailedTracks(listOf(uri))
                    deletingUri = null
                }
            } else {
                viewModel.deleteTrack(uri)
                playerViewModel.removeTrackFromQueue(uri)
                deletingUri = null
            }
        } else {
            if (uri != null) restoreFailedTracks(listOf(uri))
            deletingUri = null
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        tabListState.animateScrollToItem(pagerState.settledPage)
    }
    LaunchedEffect(deleteQueue, deletingUri) {
        if (deletingUri != null || deleteQueue.isEmpty()) return@LaunchedEffect
        val uri = deleteQueue.first()
        deleteQueue = ArrayList(deleteQueue.drop(1))
        deletingUri = uri
        com.example.xargoosh.utils.TrackUtils.deleteTrack(
            context = context,
            trackId = uri,
            onIntentSenderRequired = sequentialDeleteLauncher::launch,
            onSuccess = {
                viewModel.deleteTrack(uri)
                playerViewModel.removeTrackFromQueue(uri)
                deletingUri = null
            },
            onFailure = { restoreFailedTracks(listOf(uri)); deletingUri = null }
        )
    }
    LaunchedEffect(batchDeleteQueue, pendingBatchDelete) {
        if (pendingBatchDelete.isNotEmpty() || batchDeleteQueue.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@LaunchedEffect
        val chunk = batchDeleteQueue.take(2_000)
        batchDeleteQueue = ArrayList(batchDeleteQueue.drop(chunk.size))
        runCatching {
            android.provider.MediaStore.createDeleteRequest(context.contentResolver, chunk.map(android.net.Uri::parse)).intentSender
        }.onSuccess { sender ->
            pendingBatchDelete = ArrayList(chunk)
            batchDeleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
        }.onFailure {
            deleteQueue = ArrayList(deleteQueue + chunk)
        }
    }
    val writeRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) approvedRename = pendingWriteRequest
        else android.widget.Toast.makeText(context, renameCancelledMessage, android.widget.Toast.LENGTH_SHORT).show()
        pendingWriteRequest = null
    }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val readFlag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            val writeFlag = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val granted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, readFlag or writeFlag)
            }.recoverCatching {
                context.contentResolver.takePersistableUriPermission(uri, readFlag)
            }.isSuccess
            if (granted) viewModel.scanSafFolder(uri)
            else android.widget.Toast.makeText(context, folderAccessFailedMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (permissions[mediaPermission] == true) viewModel.loadLocalMusic()
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        else
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionLauncher.launch(perms)
    }

    LaunchedEffect(approvedRename) {
        val request = approvedRename ?: return@LaunchedEffect
        approvedRename = null
        isRenaming = true
        var updated = 0
        val tracksToProcess = request.tracks.distinctBy { it.uri }
        tracksToProcess.drop(request.startIndex).forEachIndexed { offset, track ->
            val updatedArtist = if (request.type == RenameGroupType.ARTIST) {
                com.example.xargoosh.utils.ArtistCredits.replace(track.artist, request.oldName, request.newName)
            } else null
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                pendingWriteRequest = request.copy(
                    startIndex = request.startIndex + offset,
                    completed = request.completed + updated
                )
            }
            val result = com.example.xargoosh.domain.editor.MetadataEditor.editMetadata(
                context = context,
                uri = android.net.Uri.parse(track.uri),
                title = null,
                artist = updatedArtist,
                album = request.newName.takeIf { request.type == RenameGroupType.ALBUM },
                coverArtUri = null,
                intentSenderLauncher = writeRequestLauncher.takeIf { Build.VERSION.SDK_INT == Build.VERSION_CODES.Q }
            )
            if (result is com.example.xargoosh.domain.editor.MetadataEditor.EditResult.PermissionRequired) {
                isRenaming = false
                return@LaunchedEffect
            }
            if (result == com.example.xargoosh.domain.editor.MetadataEditor.EditResult.Success) {
                if (request.type == RenameGroupType.ALBUM) viewModel.updateAlbumTag(track.uri, request.newName)
                else viewModel.updateArtistTag(track.uri, updatedArtist ?: track.artist)
                viewModel.invalidateLyrics(track.uri)
                val updatedTrack = if (request.type == RenameGroupType.ALBUM) {
                    track.copy(album = request.newName)
                } else {
                    track.copy(artist = updatedArtist ?: track.artist)
                }
                playerViewModel.refreshTrackMetadata(listOf(updatedTrack))
                updated++
            }
        }
        isRenaming = false
        pendingWriteRequest = null
        renameRequest = null
        val updatedTotal = request.completed + updated
        android.widget.Toast.makeText(context, resources.getString(R.string.updated_tracks, updatedTotal, tracksToProcess.size), android.widget.Toast.LENGTH_LONG).show()
    }

    val tracks = (uiState as? HomeUiState.Success)?.tracks ?: emptyList()
    val isScanning = (uiState as? HomeUiState.Success)?.isScanning == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (XargooshTheme.appTheme.usesGlass) {
                    Brush.verticalGradient(
                        listOf(
                            XargooshTheme.colors.primary.copy(alpha = 0.2f),
                            XargooshTheme.colors.background,
                            XargooshTheme.colors.secondary.copy(alpha = 0.1f),
                            XargooshTheme.colors.background
                        )
                    )
                } else androidx.compose.ui.graphics.SolidColor(XargooshTheme.colors.background)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            com.example.xargoosh.core.components.surface.GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .padding(horizontal = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.medium, vertical = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.small),
                shape = RoundedCornerShape(0.dp),
                blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.medium,
                color = Color.Transparent,
                borderColor = Color.Transparent
            ) {
                if (hasSelection) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.selected_count, selectionCount),
                            color = XargooshTheme.colors.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showSelectionPlaylistDialog = true }, enabled = selectedActionUris.isNotEmpty()) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = stringResource(R.string.add_selected_playlist), tint = XargooshTheme.colors.primary)
                        }
                        IconButton(onClick = { com.example.xargoosh.utils.TrackUtils.shareTracks(context, selectedActionUris) }, enabled = selectedActionUris.isNotEmpty()) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_selected_songs), tint = XargooshTheme.colors.onSurface)
                        }
                        IconButton(onClick = { showSelectionDeleteDialog = true }, enabled = selectedActionUris.isNotEmpty()) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected_songs_description), tint = XargooshTheme.colors.error)
                        }
                        IconButton(onClick = clearSelection) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_selection), tint = XargooshTheme.colors.onSurface)
                        }
                    }
                } else if (isSearchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyLarge.copy(color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(stringResource(R.string.search_hint), color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface.copy(alpha = 0.5f), style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyLarge)
                                    }
                                    inner()
                                }
                            )
                            IconButton(onClick = { 
                                if (searchQuery.isNotEmpty()) viewModel.updateSearchQuery("") 
                                else isSearchActive = false 
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_clear), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(stringResource(R.string.brand_xargoosh_upper), color = XargooshTheme.colors.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                                    Text(stringResource(R.string.brand_music_player_upper), color = XargooshTheme.colors.primary, fontSize = 10.sp, letterSpacing = 2.sp)
                                }
                            }
                            Row {
                                IconButton(onClick = onNavigateToMusicRecognition) {
                                     Icon(Icons.Default.GraphicEq, contentDescription = stringResource(R.string.music_recognition_title), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface)
                                }
                                IconButton(onClick = { isSearchActive = true }) {
                                     Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface)
                                }
                                IconButton(onClick = { onNavigateToSettings() }) {
                                     Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.settings), tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface)
                                }
                            }
                        }
                    }
            }

            androidx.compose.foundation.lazy.LazyRow(
                state = tabListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs.size) { index ->
                    val title = tabs[index]
                    val isSelected = pagerState.currentPage == index
                    Column(
                        modifier = Modifier
                            .height(52.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = { if (!hasSelection) scope.launch { pagerState.animateScrollToPage(index, animationSpec = tween(280)) } }
                            ),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title.uppercase(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
                            color = if (isSelected) XargooshTheme.colors.primary else XargooshTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            letterSpacing = 0.7.sp
                        )
                        Box(
                            Modifier
                                .width(if (isSelected) 52.dp else 0.dp)
                                .height(2.dp)
                                .background(XargooshTheme.colors.primary)
                        )
                }
            }
            }
            HorizontalDivider(color = XargooshTheme.colors.outline.copy(alpha = 0.5f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !hasSelection
            ) { page ->
                when (page) {
                    0 -> TracksTab(
                        homeViewModel = viewModel,
                        tracks = tracks,
                        currentTrack = currentTrack,
                        isScanning = isScanning,
                        isPlaying = isPlaying,
                        uiState = uiState,
                        onClick = { idx -> onNavigateToPlayer(tracks, idx) },
                        onEditMetadata = onEditMetadata,
                        showSortMenu = showSortMenu,
                        onSortClick = { showSortMenu = true },
                        onSortDismiss = { showSortMenu = false },
                        currentSort = currentSort,
                        onSortChange = { viewModel.updateSort(it) },
                        onQueueClick = onNavigateToQueue,
                        availableGenres = availableGenres,
                        selectedGenre = genreFilter,
                        onGenreSelect = { viewModel.setGenreFilter(it) },
                        onShuffleAll = {
                            if (tracks.isNotEmpty()) {
                                playerViewModel.playShuffled(tracks)
                            }
                        },
                        onPlayAll = { if (tracks.isNotEmpty()) onNavigateToPlayer(tracks, 0) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onTrackDeleted = playerViewModel::removeTrackFromQueue,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onNavigateToArtist = onNavigateToArtist,
                        listState = songListState,
                        selectedTrackUris = selectedTrackUris,
                        onSelectionChange = { selectedTrackUris = it }
                    )
                    1 -> PlaylistsTab(
                        viewModel = viewModel,
                        tracks = allTracks,
                        onPlaylistClick = onNavigateToPlaylist,
                        onSmartPlaylistClick = onNavigateToSmartPlaylist,
                        listState = playlistListState,
                        sort = playlistSort,
                        onSortChange = viewModel::updatePlaylistSort
                    )
                    2 -> AlbumsTab(
                        albums = albumGroups,
                        sort = albumSort,
                        layout = albumLayout,
                        listState = albumListState,
                        gridState = albumGridState,
                        onSortChange = viewModel::updateAlbumSort,
                        onLayoutChange = viewModel::updateAlbumLayout,
                        onAlbumClick = onNavigateToAlbum,
                        onPlay = { playerViewModel.playQueue(it, 0) },
                        onShuffle = playerViewModel::playShuffled,
                        onAddToQueue = playerViewModel::addAllToQueue,
                        selectedAlbumKeys = selectedAlbumKeys,
                        onSelectionChange = { selectedAlbumKeys = it },
                        onRename = { album, groupTracks ->
                            renameValue = album
                            renameRequest = GroupRenameRequest(
                                RenameGroupType.ALBUM,
                                album,
                                groupTracks.distinctBy { it.uri }
                            )
                        }
                    )
                    3 -> ArtistsTab(
                        artists = artistGroups,
                        sort = artistSort,
                        listState = artistListState,
                        onSortChange = viewModel::updateArtistSort,
                        onArtistClick = onNavigateToArtist,
                        onPlay = { playerViewModel.playQueue(it, 0) },
                        onShuffle = playerViewModel::playShuffled,
                        onAddToQueue = playerViewModel::addAllToQueue,
                        selectedArtistNames = selectedArtistNames,
                        onSelectionChange = { selectedArtistNames = it },
                        onRename = { artist ->
                            renameValue = artist
                            renameRequest = GroupRenameRequest(
                                RenameGroupType.ARTIST,
                                artist,
                                allTracks.filter { com.example.xargoosh.utils.ArtistCredits.contains(it.artist, artist) }
                            )
                        }
                    )
                    4 -> FoldersTab(
                        viewModel = viewModel,
                        onFolderClick = onNavigateToFolder,
                        onAddFolderClick = { folderLauncher.launch(null) },
                        listState = folderListState,
                        sort = folderSort,
                        onSortChange = viewModel::updateFolderSort,
                        selectedFolderIds = selectedFolderIds,
                        onSelectionChange = { selectedFolderIds = it }
                    )
                    5 -> GenresTab(
                        genres = genreGroups,
                        sort = genreSort,
                        listState = genreListState,
                        onSortChange = viewModel::updateGenreSort,
                        onGenreClick = onNavigateToGenre,
                        onPlay = { playerViewModel.playQueue(it, 0) },
                        onShuffle = playerViewModel::playShuffled,
                        onAddToQueue = playerViewModel::addAllToQueue,
                        selectedGenreNames = selectedGenreNames,
                        onSelectionChange = { selectedGenreNames = it }
                    )
                }
            }
        }

        if (currentTrack != null) {
            MiniPlayerHost(
                playerViewModel = playerViewModel,
                track = currentTrack!!,
                isPlaying = isPlaying,
                onPlayPause = { playerViewModel.playPause() },
                onPrevious = { playerViewModel.skipToPrevious() },
                onNext = { playerViewModel.skipToNext() },
                onEqualizer = onNavigateToEqualizer,
                onClick = onNavigateToNowPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        if (showSelectionPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showSelectionPlaylistDialog = false; showSelectionCreatePlaylist = false },
                title = { Text(stringResource(R.string.add_songs_to_playlist, selectedActionUris.size)) },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        if (showSelectionCreatePlaylist) {
                            OutlinedTextField(
                                value = selectionPlaylistName,
                                onValueChange = { selectionPlaylistName = it },
                                label = { Text(stringResource(R.string.playlist_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (selectionPlaylistName.isNotBlank()) {
                                        viewModel.createPlaylistAndAddTracks(selectionPlaylistName.trim(), selectedActionUris)
                                        clearSelection()
                                        selectionPlaylistName = ""
                                        showSelectionCreatePlaylist = false
                                        showSelectionPlaylistDialog = false
                                    }
                                },
                                enabled = selectionPlaylistName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text(stringResource(R.string.create_and_add)) }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showSelectionCreatePlaylist = true }.padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = XargooshTheme.colors.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.create_new_playlist), color = XargooshTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                            }
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                                items(playlists.size, key = { playlists[it].id }) { index ->
                                    val playlist = playlists[index]
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.addTracksToPlaylist(playlist.id, selectedActionUris)
                                            clearSelection()
                                            showSelectionPlaylistDialog = false
                                        }.padding(vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = XargooshTheme.colors.onSurfaceVariant)
                                        Spacer(Modifier.width(12.dp))
                                        Text(localizedPlaylistName(playlist.name), color = XargooshTheme.colors.onBackground)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showSelectionPlaylistDialog = false; showSelectionCreatePlaylist = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }

        if (showSelectionDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showSelectionDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_selected_songs)) },
                text = { Text(stringResource(R.string.delete_selection_summary, selectedActionUris.size, selectionCount)) },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = selectedActionUris.toList()
                        clearSelection()
                        showSelectionDeleteDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val (mediaUris, otherUris) = selected.partition {
                                android.net.Uri.parse(it).authority == android.provider.MediaStore.AUTHORITY
                            }
                            deleteQueue = ArrayList(deleteQueue + otherUris)
                            batchDeleteQueue = ArrayList(batchDeleteQueue + mediaUris)
                        } else {
                            deleteQueue = ArrayList(deleteQueue + selected)
                        }
                    }) { Text(stringResource(R.string.delete), color = XargooshTheme.colors.error) }
                },
                dismissButton = { TextButton(onClick = { showSelectionDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }

        renameRequest?.let { request ->
            AlertDialog(
                onDismissRequest = { if (!isRenaming) renameRequest = null },
                title = { Text(stringResource(R.string.rename_group, request.type.name.lowercase())) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.rename_files_description, request.tracks.size), color = XargooshTheme.colors.onSurfaceVariant)
                        OutlinedTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it },
                            label = { Text(stringResource(if (request.type == RenameGroupType.ALBUM) R.string.album_name else R.string.artist_name)) },
                            singleLine = true,
                            enabled = !isRenaming
                        )
                        if (isRenaming) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = renameValue.isNotBlank() && renameValue.trim() != request.oldName && !isRenaming,
                        onClick = {
                            val confirmed = request.copy(newName = renameValue.trim())
                            val mediaUris = confirmed.tracks.map { android.net.Uri.parse(it.uri) }
                                .filter { it.authority == android.provider.MediaStore.AUTHORITY }
                                .distinct()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mediaUris.isNotEmpty()) {
                                if (mediaUris.size > 2_000) {
                                    android.widget.Toast.makeText(context, tooManyFilesRenameMessage, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    pendingWriteRequest = confirmed
                                    val sender = android.provider.MediaStore.createWriteRequest(context.contentResolver, mediaUris).intentSender
                                    writeRequestLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
                                }
                            } else approvedRename = confirmed
                        }
                    ) { Text(stringResource(R.string.rename)) }
                },
                dismissButton = { TextButton(enabled = !isRenaming, onClick = { renameRequest = null }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }
}

@Composable
private fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    track: MusicTrack,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEqualizer: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPosition by playerViewModel.currentPosition.collectAsStateWithLifecycle()
    BottomMiniPlayer(
        track = track,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onEqualizer = onEqualizer,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun BottomMiniPlayer(
    track: MusicTrack,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEqualizer: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val durationSafe = track.durationMs.coerceAtLeast(1L)
    val progress = (currentPosition.toFloat() / durationSafe.toFloat()).coerceIn(0f, 1f)
    val isAero = XargooshTheme.appTheme.isAero

    com.example.xargoosh.core.components.surface.GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = com.example.xargoosh.core.design.themes.XargooshTheme.shapes.extraLarge,
        elevation = com.example.xargoosh.core.design.themes.XargooshTheme.elevation.level4,
        blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.high,
        color = XargooshTheme.colors.surfaceVariant.copy(alpha = if (isAero) 0.96f else 1f),
        borderColor = XargooshTheme.colors.primary.copy(alpha = 0.2f),
        opaque = !isAero,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isAero) 68.dp else 64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                    modifier = Modifier
                        .size(if (isAero) 52.dp else 44.dp)
                        .clip(com.example.xargoosh.core.design.themes.XargooshTheme.shapes.medium)
                        .background(avatarColorFor(track.title)),
                    contentAlignment = Alignment.Center
                ) {
                    AudioThumbnail(uri = track.uri, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title, 
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground, 
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist, 
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, 
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    MiniWaveform(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = if (isAero) Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(XargooshTheme.colors.surface.copy(alpha = 0.34f))
                        .padding(horizontal = 2.dp) else Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(onClick = onEqualizer, modifier = Modifier.size(if (isAero) 30.dp else 32.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = stringResource(R.string.equalizer), tint = XargooshTheme.colors.secondary, modifier = Modifier.size(18.dp))
                    }
                    androidx.compose.material3.IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous), tint = XargooshTheme.colors.onBackground, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isAero) 40.dp else 44.dp)
                            .clip(CircleShape)
                            .background(com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary.copy(alpha = 0.2f))
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next), tint = XargooshTheme.colors.onBackground, modifier = Modifier.size(20.dp))
                    }
                }
        }
    }
}

@Composable
private fun MiniWaveform(progress: Float, modifier: Modifier = Modifier) {
    val activeColor = XargooshTheme.colors.primary
    val inactiveColor = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.28f)
    Canvas(modifier) {
        val bars = 34
        val gap = size.width / bars
        repeat(bars) { index ->
            val ratio = index.toFloat() / (bars - 1)
            val amplitude = (0.25f + kotlin.math.abs(kotlin.math.sin(index * 1.37f)) * 0.75f) * size.height
            drawLine(
                color = if (ratio <= progress) activeColor else inactiveColor,
                start = androidx.compose.ui.geometry.Offset(index * gap, (size.height - amplitude) / 2f),
                end = androidx.compose.ui.geometry.Offset(index * gap, (size.height + amplitude) / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksTab(
    homeViewModel: HomeViewModel,
    tracks: List<MusicTrack>,
    currentTrack: MusicTrack?,
    isScanning: Boolean,
    isPlaying: Boolean,
    uiState: HomeUiState,
    onClick: (Int) -> Unit,
    onEditMetadata: (MusicTrack) -> Unit,
    showSortMenu: Boolean,
    onSortClick: () -> Unit,
    onSortDismiss: () -> Unit,
    currentSort: HomeViewModel.SortOption,
    onSortChange: (HomeViewModel.SortOption) -> Unit,
    onQueueClick: () -> Unit,
    availableGenres: List<String>,
    selectedGenre: String?,
    onGenreSelect: (String?) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAddToQueue: (MusicTrack) -> Unit,
    onTrackDeleted: (String) -> Unit,
    onNavigateToAlbum: (String, String?) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    listState: LazyListState,
    selectedTrackUris: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    var selectedTrackForMenu by remember { mutableStateOf<MusicTrack?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showPlaylistSelector by remember { mutableStateOf(false) }
      var trackToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val deletedFromDeviceMessage = stringResource(R.string.deleted_from_device)
    val playlists by homeViewModel.playlists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            android.widget.Toast.makeText(context, deletedFromDeviceMessage, android.widget.Toast.LENGTH_SHORT).show()
            trackToDelete?.let { uri ->
                homeViewModel.deleteTrack(uri)
                onTrackDeleted(uri)
            }
            trackToDelete = null
            showOptionsMenu = false
        }
    }

        if (showOptionsMenu && selectedTrackForMenu != null) {
        com.example.xargoosh.presentation.components.TrackOptionsMenu(
            track = selectedTrackForMenu!!,
            onDismiss = { showOptionsMenu = false },
            onPlayNext = { onPlayNext(selectedTrackForMenu!!) },
            onAddToQueue = { onAddToQueue(selectedTrackForMenu!!) },
            onAddToPlaylist = { showPlaylistSelector = true },
            onGoToAlbum = {
                onNavigateToAlbum(
                    selectedTrackForMenu!!.album,
                    com.example.xargoosh.utils.ArtistCredits.names(selectedTrackForMenu!!.artist).firstOrNull()
                )
            },
            onGoToArtist = { onNavigateToArtist(selectedTrackForMenu!!.artist) },
            onEditTags = { onEditMetadata(selectedTrackForMenu!!) },
            onChangeCover = { onEditMetadata(selectedTrackForMenu!!) },
            onSetAsRingtone = { com.example.xargoosh.utils.TrackUtils.setAsRingtone(context, selectedTrackForMenu!!.uri) },
            onDeleteFromDevice = {
                trackToDelete = selectedTrackForMenu?.uri
                com.example.xargoosh.utils.TrackUtils.deleteTrack(
                    context = context,
                    trackId = selectedTrackForMenu!!.id,
                    onIntentSenderRequired = { deleteLauncher.launch(it) },
                    onSuccess = { 
                        homeViewModel.deleteTrack(selectedTrackForMenu!!.uri)
                        onTrackDeleted(selectedTrackForMenu!!.uri)
                        showOptionsMenu = false
                    }
                )
            }
        )
    }

    if (showPlaylistSelector && selectedTrackForMenu != null) {

        var showCreateField by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPlaylistSelector = false; showCreateField = false; newPlaylistName = "" },
            title = { Text(stringResource(R.string.add_to_playlist), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showCreateField) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                placeholder = { Text(stringResource(R.string.playlist_name)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    scope.launch {
                                        homeViewModel.createPlaylistAndAddTrack(newPlaylistName.trim(), selectedTrackForMenu!!.uri)
                                        showPlaylistSelector = false
                                        showCreateField = false
                                        newPlaylistName = ""
                                    }
                                }
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = stringResource(R.string.save))
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().clickable { showCreateField = true }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null, tint = XargooshTheme.colors.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(R.string.create_new_playlist), fontSize = 16.sp, color = XargooshTheme.colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (playlists.isEmpty()) {
                        Text(stringResource(R.string.no_playlists_available), modifier = Modifier.padding(8.dp))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(playlists.size) { index ->
                                val playlist = playlists[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                homeViewModel.addTrackToPlaylist(playlist.id, selectedTrackForMenu!!.uri)
                                                showPlaylistSelector = false
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = XargooshTheme.colors.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(localizedPlaylistName(playlist.name), fontSize = 16.sp, color = XargooshTheme.colors.onBackground)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistSelector = false; showCreateField = false; newPlaylistName = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isScanning) stringResource(R.string.scanning_song_count, tracks.size) else pluralStringResource(R.plurals.song_count, tracks.size, tracks.size),
                    color = XargooshTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onQueueClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue), tint = XargooshTheme.colors.onSurfaceVariant)
                    }
                    Box {

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(XargooshTheme.colors.surfaceVariant)
                            .clickable { onSortClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.sort), tint = XargooshTheme.colors.primary, modifier = Modifier.size(14.dp))
                        Text(
                            sortLabel(currentSort),
                            color = XargooshTheme.colors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            sortArrow(currentSort),
                            color = XargooshTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showSortMenu) {
                        ModalBottomSheet(
                            onDismissRequest = onSortDismiss,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            containerColor = XargooshTheme.colors.surfaceVariant,
                            dragHandle = { BottomSheetDefaults.DragHandle(color = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.4f)) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                Text(
                                    stringResource(R.string.sort_by), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 18.sp, 
                                    color = XargooshTheme.colors.onBackground,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                                HorizontalDivider(color = XargooshTheme.colors.outline.copy(alpha=0.1f))
                                SortOptionItem(stringResource(R.string.sort_title_az), HomeViewModel.SortOption.TITLE_ASC, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_title_za), HomeViewModel.SortOption.TITLE_DESC, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_artist_az), HomeViewModel.SortOption.ARTIST_ASC, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_artist_za), HomeViewModel.SortOption.ARTIST_DESC, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_artist_count), HomeViewModel.SortOption.ARTIST_TRACK_COUNT, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_recent_new_old), HomeViewModel.SortOption.DATE_ADDED_DESC, currentSort) { onSortChange(it); onSortDismiss() }
                                SortOptionItem(stringResource(R.string.sort_recent_old_new), HomeViewModel.SortOption.DATE_ADDED_ASC, currentSort) { onSortChange(it); onSortDismiss() }
                            }
                        }
                    }
                }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onShuffleAll,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, XargooshTheme.colors.onSurfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = XargooshTheme.colors.onBackground)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shuffle), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XargooshTheme.colors.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.play), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        when (uiState) {
            is HomeUiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XargooshTheme.colors.primary)
                }
            }
            is HomeUiState.Error -> item {
                Text(uiState.message, color = XargooshTheme.colors.error, modifier = Modifier.padding(16.dp))
            }
            else -> {}
        }

        items(tracks.size, key = { tracks[it].uri }) { index ->
            val track = tracks[index]
            TrackListItem(
                track = track,
                isPlayingTrack = currentTrack?.id == track.id,
                isPlaying = isPlaying && currentTrack?.id == track.id,
                index = index,
                selected = track.uri in selectedTrackUris,
                selectionMode = selectedTrackUris.isNotEmpty(),
                onClick = {
                    if (selectedTrackUris.isNotEmpty()) {
                        onSelectionChange(if (track.uri in selectedTrackUris) selectedTrackUris - track.uri else selectedTrackUris + track.uri)
                    } else onClick(index)
                },
                onLongClick = {
                    onSelectionChange(if (track.uri in selectedTrackUris) selectedTrackUris - track.uri else selectedTrackUris + track.uri)
                },
                onEditClick = {
                    selectedTrackForMenu = track
                    showOptionsMenu = true
                }
            )
        }
    }
        ScrollToTopButton(
            visible = listState.firstVisibleItemIndex > 5,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
        )
    }
}

@Composable
internal fun ScrollToTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.82f),
        exit = fadeOut() + scaleOut(targetScale = 0.82f)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            containerColor = XargooshTheme.colors.primary,
            contentColor = XargooshTheme.colors.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = stringResource(R.string.move_to_top), modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
internal fun SectionToolbar(
    countLabel: String,
    sortLabel: String,
    onSortClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(countLabel, color = XargooshTheme.colors.onSurfaceVariant, style = XargooshTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        trailing()
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(XargooshTheme.colors.surfaceVariant)
                .clickable(onClick = onSortClick).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Sort, contentDescription = null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(17.dp))
            Text(sortLabel, color = XargooshTheme.colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SectionSortSheet(
    visible: Boolean,
    title: String,
    selected: T,
    choices: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = XargooshTheme.colors.surfaceVariant,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Text(title, color = XargooshTheme.colors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        choices.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(value); onDismiss() }.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = if (selected == value) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground, modifier = Modifier.weight(1f), fontSize = 16.sp)
                RadioButton(selected = selected == value, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = XargooshTheme.colors.primary))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SortOptionItem(
    label: String,
    option: HomeViewModel.SortOption,
    currentSort: HomeViewModel.SortOption,
    onSelect: (HomeViewModel.SortOption) -> Unit
) {
    val selected = option == currentSort
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(option) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SmallGenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) XargooshTheme.colors.primary else XargooshTheme.colors.surfaceVariant,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) XargooshTheme.colors.onPrimary else XargooshTheme.colors.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chipText"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun sortLabel(sort: HomeViewModel.SortOption) = when (sort) {
    HomeViewModel.SortOption.TITLE_ASC, HomeViewModel.SortOption.TITLE_DESC -> stringResource(R.string.title)
    HomeViewModel.SortOption.ARTIST_ASC, HomeViewModel.SortOption.ARTIST_DESC, HomeViewModel.SortOption.ARTIST_TRACK_COUNT, HomeViewModel.SortOption.ARTIST_DATE_ADDED_DESC, HomeViewModel.SortOption.ARTIST_DATE_ADDED_ASC -> stringResource(R.string.artist)
    HomeViewModel.SortOption.DATE_ADDED_DESC, HomeViewModel.SortOption.DATE_ADDED_ASC -> stringResource(R.string.date)
}

private fun sortArrow(sort: HomeViewModel.SortOption) = when (sort) {
    HomeViewModel.SortOption.TITLE_DESC, HomeViewModel.SortOption.DATE_ADDED_DESC -> "↓"
    else -> "↑"
}

@Composable
fun TrackListItem(
    track: MusicTrack,
    isPlayingTrack: Boolean = false,
    isPlaying: Boolean = false,
    index: Int,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    val durationMin = track.durationMs / 60000
    val durationSec = (track.durationMs % 60000) / 1000
    val durationStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", durationMin, durationSec)
    val initials = track.title.take(2).uppercase()
    val avatarBg = avatarColorFor(track.title)

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.medium, vertical = com.example.xargoosh.core.design.themes.XargooshTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPlayingTrack) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .background(XargooshTheme.colors.primary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(com.example.xargoosh.core.design.themes.XargooshTheme.shapes.small)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = if (avatarBg.luminance() > 0.45f) Color.Black else Color.White, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium)
                AudioThumbnail(uri = track.uri, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isPlayingTrack) com.example.xargoosh.core.design.themes.XargooshTheme.colors.primary else com.example.xargoosh.core.design.themes.XargooshTheme.colors.onBackground,
                    style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = track.artist,
                        color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface,
                        style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.album.isNotBlank() && track.album != "<unknown>") {
                        Text("·", color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium)
                        Text(
                            text = track.album,
                            color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurface,
                            style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isPlayingTrack) {
                Icon(Icons.Default.GraphicEq, contentDescription = stringResource(if (isPlaying) R.string.playing else R.string.selected), tint = XargooshTheme.colors.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = XargooshTheme.colors.primary)
                )
            } else {
                Text(durationStr, color = com.example.xargoosh.core.design.themes.XargooshTheme.colors.onSurfaceVariant, style = com.example.xargoosh.core.design.themes.XargooshTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(8.dp))
                CompactTrackOptionsButton(onClick = onEditClick)
            }
        }
    }

    if (isPlayingTrack || selected) {
        com.example.xargoosh.core.components.surface.GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = com.example.xargoosh.core.design.themes.XargooshTheme.shapes.medium,
            color = XargooshTheme.colors.primary.copy(alpha = if (selected) 0.18f else 0.08f),
            borderColor = XargooshTheme.colors.primary.copy(alpha = if (selected) 0.62f else 0.32f),
            blurRadius = com.example.xargoosh.core.design.themes.XargooshTheme.blur.subtle
        ) {
            content()
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
            content()
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(
    viewModel: HomeViewModel,
    tracks: List<MusicTrack>,
    onPlaylistClick: (Int, String) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistKind) -> Unit,
    listState: LazyListState,
    sort: PlaylistSort,
    onSortChange: (PlaylistSort) -> Unit
) {
    val sourcePlaylists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlists = remember(sourcePlaylists, sort) {
        when (sort) {
            PlaylistSort.NAME_ASC -> sourcePlaylists.sortedBy { it.name.lowercase() }
            PlaylistSort.NAME_DESC -> sourcePlaylists.sortedByDescending { it.name.lowercase() }
            PlaylistSort.DATE_ASC -> sourcePlaylists.sortedBy { it.dateCreated }
            PlaylistSort.DATE_DESC -> sourcePlaylists.sortedByDescending { it.dateCreated }
        }
    }
    val smartPlaylists = SmartPlaylistKind.entries
    val smartTracks = remember(tracks) {
        smartPlaylists.associateWith { it.selectTracks(tracks) }
    }
    var showDialog by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()

    SectionSortSheet(
        visible = showSort,
        title = stringResource(R.string.sort_playlists),
        selected = sort,
        choices = listOf(
            PlaylistSort.NAME_ASC to stringResource(R.string.sort_playlist_az),
            PlaylistSort.NAME_DESC to stringResource(R.string.sort_playlist_za),
            PlaylistSort.DATE_DESC to stringResource(R.string.sort_newest),
            PlaylistSort.DATE_ASC to stringResource(R.string.sort_oldest)
        ),
        onSelect = onSortChange,
        onDismiss = { showSort = false }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.new_playlist), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XargooshTheme.colors.primary,
                        focusedLabelColor = XargooshTheme.colors.primary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { if (newName.isNotBlank()) { viewModel.createPlaylist(newName); showDialog = false; newName = "" } },
                    colors = ButtonDefaults.buttonColors(containerColor = XargooshTheme.colors.primary)
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (smartPlaylists.isEmpty() && playlists.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = XargooshTheme.colors.onSurfaceVariant, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.no_playlists_yet), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.tap_create_playlist), color = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
        Column(modifier = Modifier.fillMaxSize()) {
                if (selectedPlaylistIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(XargooshTheme.colors.errorContainer)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.selected_count, selectedPlaylistIds.size), color = XargooshTheme.colors.onErrorContainer, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = {
                                selectedPlaylistIds.forEach { viewModel.deletePlaylist(it) }
                                selectedPlaylistIds = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = XargooshTheme.colors.onErrorContainer)
                            }
                            IconButton(onClick = { selectedPlaylistIds = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = XargooshTheme.colors.onErrorContainer)
                            }
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                item {
                    Text(
                        text = stringResource(R.string.smart_playlists),
                        color = XargooshTheme.colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
                items(smartPlaylists.size, key = { "smart-${smartPlaylists[it].name}" }) { i ->
                    val kind = smartPlaylists[i]
                    SmartPlaylistRow(
                        kind = kind,
                        songCount = smartTracks.getValue(kind).size,
                        onClick = { onSmartPlaylistClick(kind) }
                    )
                }
                item { SectionToolbar(pluralStringResource(R.plurals.playlist_count, playlists.size, playlists.size), playlistSortLabel(sort), onSortClick = { showSort = true }) }
                 items(playlists.size, key = { playlists[it].id }) { i ->
                     val p = playlists[i]
                     val displayName = localizedPlaylistName(p.name)
                     val isSelected = selectedPlaylistIds.contains(p.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) XargooshTheme.colors.primaryContainer else XargooshTheme.colors.surfaceVariant)
                            .combinedClickable(
                                onClick = {
                                    if (selectedPlaylistIds.isNotEmpty()) {
                                        if (isSelected) selectedPlaylistIds = selectedPlaylistIds - p.id
                                        else selectedPlaylistIds = selectedPlaylistIds + p.id
                                    } else {
                                        onPlaylistClick(p.id, displayName)
                                    }
                                },
                                onLongClick = {
                                    selectedPlaylistIds = if (isSelected) selectedPlaylistIds - p.id else selectedPlaylistIds + p.id
                                }
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(if (isSelected) XargooshTheme.colors.primary else XargooshTheme.colors.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = XargooshTheme.colors.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(displayName, color = if (isSelected) XargooshTheme.colors.onPrimaryContainer else XargooshTheme.colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 90.dp).navigationBarsPadding(),
            containerColor = XargooshTheme.colors.primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_playlist))
        }
        ScrollToTopButton(
            visible = listState.firstVisibleItemIndex > 4,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
        )
    }
}

@Composable
private fun SmartPlaylistRow(
    kind: SmartPlaylistKind,
    songCount: Int,
    onClick: () -> Unit
) {
    val title = when (kind) {
        SmartPlaylistKind.RECENTLY_ADDED -> stringResource(R.string.recently_added)
        SmartPlaylistKind.MOST_PLAYED -> stringResource(R.string.most_played)
        SmartPlaylistKind.NEVER_PLAYED -> stringResource(R.string.never_played)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(XargooshTheme.colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(XargooshTheme.colors.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = XargooshTheme.colors.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = XargooshTheme.colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                pluralStringResource(R.plurals.song_count, songCount, songCount),
                color = XargooshTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CompactTrackOptionsButton(onClick: () -> Unit) {
    val isAero = XargooshTheme.appTheme.isAero
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).then(
                if (isAero) Modifier
                    .background(XargooshTheme.colors.surfaceVariant.copy(alpha = 0.88f), CircleShape)
                    .border(0.75.dp, XargooshTheme.colors.glassBorder.copy(alpha = 0.72f), CircleShape)
                else Modifier
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options), tint = XargooshTheme.colors.onSurface, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun playlistSortLabel(sort: PlaylistSort) = when (sort) {
    PlaylistSort.NAME_ASC, PlaylistSort.NAME_DESC -> stringResource(R.string.name)
    PlaylistSort.DATE_ASC, PlaylistSort.DATE_DESC -> stringResource(R.string.date)
}

@Composable
fun AlbumsTab(
    albums: List<AlbumGroup>,
    sort: AlbumSort,
    layout: AlbumLayout,
    listState: LazyListState,
    gridState: LazyGridState,
    onSortChange: (AlbumSort) -> Unit,
    onLayoutChange: (AlbumLayout) -> Unit,
    onAlbumClick: (String, String?) -> Unit,
    onPlay: (List<MusicTrack>) -> Unit,
    onShuffle: (List<MusicTrack>) -> Unit,
    onAddToQueue: (List<MusicTrack>) -> Unit,
    selectedAlbumKeys: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onRename: (String, List<MusicTrack>) -> Unit
) {
    var showSort by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SectionSortSheet(
        visible = showSort,
        title = stringResource(R.string.sort_albums),
        selected = sort,
        choices = listOf(
            AlbumSort.NAME_ASC to stringResource(R.string.sort_album_az), AlbumSort.NAME_DESC to stringResource(R.string.sort_album_za),
            AlbumSort.ARTIST_ASC to stringResource(R.string.sort_artist_name_az), AlbumSort.ARTIST_DESC to stringResource(R.string.sort_artist_name_za),
            AlbumSort.SONG_COUNT_ASC to stringResource(R.string.sort_count_ascending), AlbumSort.SONG_COUNT_DESC to stringResource(R.string.sort_count_descending)
        ),
        onSelect = onSortChange,
        onDismiss = { showSort = false }
    )
    if (albums.isEmpty()) { EmptyState(stringResource(R.string.no_albums_found)) ; return }

    Box(Modifier.fillMaxSize()) {
        val toolbar: @Composable () -> Unit = {
            SectionToolbar(pluralStringResource(R.plurals.album_count, albums.size, albums.size), albumSortLabel(sort), onSortClick = { showSort = true }) {
                IconButton(onClick = { onLayoutChange(if (layout == AlbumLayout.GRID) AlbumLayout.LIST else AlbumLayout.GRID) }, modifier = Modifier.size(40.dp)) {
                    Icon(if (layout == AlbumLayout.GRID) Icons.Default.ViewList else Icons.Default.GridView, contentDescription = stringResource(if (layout == AlbumLayout.GRID) R.string.show_albums_list else R.string.show_albums_grid), tint = XargooshTheme.colors.primary)
                }
            }
        }
        if (layout == AlbumLayout.LIST) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { toolbar() }
                items(albums.size, key = { albums[it].key }) { i ->
                    val album = albums[i]
                    val selected = album.key in selectedAlbumKeys
                    val toggleSelection = { onSelectionChange(if (selected) selectedAlbumKeys - album.key else selectedAlbumKeys + album.key) }
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SimpleListRow(
                            letter = album.name.take(1).uppercase(), primary = album.name.ifBlank { stringResource(R.string.unknown_album) }, secondary = "${album.artist} · ${pluralStringResource(R.plurals.track_count, album.tracks.size, album.tracks.size)}",
                            color = avatarColorFor(album.name), artworkUri = album.tracks.firstOrNull()?.uri,
                            selected = selected, selectionMode = selectedAlbumKeys.isNotEmpty(),
                            onClick = { if (selectedAlbumKeys.isNotEmpty()) toggleSelection() else onAlbumClick(album.name, album.artist) },
                            onLongClick = toggleSelection, onPlay = { onPlay(album.tracks) },
                            onShuffle = { onShuffle(album.tracks) }, onAddToQueue = { onAddToQueue(album.tracks) }, onRename = { onRename(album.name, album.tracks) }
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp), state = gridState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { toolbar() }
                gridItems(albums, key = { it.key }) { album ->
                    val selected = album.key in selectedAlbumKeys
                    val toggleSelection = { onSelectionChange(if (selected) selectedAlbumKeys - album.key else selectedAlbumKeys + album.key) }
                    AlbumGridCard(
                        album = album, selected = selected, selectionMode = selectedAlbumKeys.isNotEmpty(),
                        onClick = { if (selectedAlbumKeys.isNotEmpty()) toggleSelection() else onAlbumClick(album.name, album.artist) },
                        onLongClick = toggleSelection, onPlay = { onPlay(album.tracks) },
                        onShuffle = { onShuffle(album.tracks) }, onAddToQueue = { onAddToQueue(album.tracks) }, onRename = { onRename(album.name, album.tracks) }
                    )
                }
            }
        }
        ScrollToTopButton(
            visible = if (layout == AlbumLayout.GRID) gridState.firstVisibleItemIndex > 4 else listState.firstVisibleItemIndex > 4,
            onClick = { scope.launch { if (layout == AlbumLayout.GRID) gridState.animateScrollToItem(0) else listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
        )
    }
}

@Composable
private fun albumSortLabel(sort: AlbumSort) = when (sort) {
    AlbumSort.NAME_ASC, AlbumSort.NAME_DESC -> stringResource(R.string.album)
    AlbumSort.ARTIST_ASC, AlbumSort.ARTIST_DESC -> stringResource(R.string.artist)
    AlbumSort.SONG_COUNT_ASC, AlbumSort.SONG_COUNT_DESC -> stringResource(R.string.songs)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridCard(
    album: AlbumGroup,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (selected) XargooshTheme.colors.primaryContainer else Color.Transparent).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(4.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(avatarColorFor(album.name))) {
            album.tracks.firstOrNull()?.let { AudioThumbnail(it.uri, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.58f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(pluralStringResource(R.plurals.song_count, album.tracks.size, album.tracks.size), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.selected), tint = XargooshTheme.colors.primary, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(album.name.ifBlank { stringResource(R.string.unknown_album) }, color = XargooshTheme.colors.onBackground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = XargooshTheme.colors.primary))
            } else Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, stringResource(R.string.album_options), tint = XargooshTheme.colors.onSurfaceVariant) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.play)) }, onClick = { showMenu = false; onPlay() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shuffle)) }, onClick = { showMenu = false; onShuffle() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.add_all_to_queue)) }, onClick = { showMenu = false; onAddToQueue() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_album)) }, onClick = { showMenu = false; onRename() })
                }
            }
        }
    }
}

  @Composable
  fun ArtistsTab(
      artists: List<ArtistGroup>,
      sort: ArtistSort,
      listState: LazyListState,
      onSortChange: (ArtistSort) -> Unit,
      onArtistClick: (String) -> Unit,
      onPlay: (List<MusicTrack>) -> Unit,
      onShuffle: (List<MusicTrack>) -> Unit,
      onAddToQueue: (List<MusicTrack>) -> Unit,
      selectedArtistNames: Set<String>,
      onSelectionChange: (Set<String>) -> Unit,
      onRename: (String) -> Unit
  ) {
      var showSort by remember { mutableStateOf(false) }
      val scope = rememberCoroutineScope()
      SectionSortSheet(
          visible = showSort, title = stringResource(R.string.sort_artists), selected = sort,
          choices = listOf(
              ArtistSort.NAME_ASC to stringResource(R.string.sort_artist_name_az), ArtistSort.NAME_DESC to stringResource(R.string.sort_artist_name_za),
              ArtistSort.SONG_COUNT_ASC to stringResource(R.string.sort_count_ascending), ArtistSort.SONG_COUNT_DESC to stringResource(R.string.sort_count_descending),
              ArtistSort.ALBUM_COUNT_ASC to stringResource(R.string.sort_album_count_ascending), ArtistSort.ALBUM_COUNT_DESC to stringResource(R.string.sort_album_count_descending)
          ), onSelect = onSortChange, onDismiss = { showSort = false }
      )
      if (artists.isEmpty()) { EmptyState(stringResource(R.string.no_artists_found)) ; return }

      Box(modifier = Modifier.fillMaxSize()) {
          LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
              item { SectionToolbar(pluralStringResource(R.plurals.artist_count, artists.size, artists.size), artistSortLabel(sort), onSortClick = { showSort = true }) }
              items(artists.size, key = { artists[it].name.lowercase() }) { i ->
                  val artist = artists[i]
                  val artistName = artist.name
                  val artistTracks = artist.tracks
                  val selected = artistName in selectedArtistNames
                  val toggleSelection = { onSelectionChange(if (selected) selectedArtistNames - artistName else selectedArtistNames + artistName) }
                  SimpleListRow(
                      letter = artistName.take(1).uppercase(),
                      primary = artistName.ifEmpty { stringResource(R.string.unknown_artist) },
                      secondary = pluralStringResource(R.plurals.song_count, artistTracks.size, artistTracks.size),
                      color = avatarColorFor(artistName),
                      artworkUri = artistTracks.firstOrNull()?.uri,
                      isCircle = true,
                      selected = selected,
                      selectionMode = selectedArtistNames.isNotEmpty(),
                      onClick = { if (selectedArtistNames.isNotEmpty()) toggleSelection() else onArtistClick(artistName) },
                      onLongClick = toggleSelection,
                      onPlay = { onPlay(artistTracks) },
                      onShuffle = { onShuffle(artistTracks) },
                      onAddToQueue = { onAddToQueue(artistTracks) },
                      onRename = { onRename(artistName) }
                  )
              }
          }
          ScrollToTopButton(
              visible = listState.firstVisibleItemIndex > 4,
              onClick = { scope.launch { listState.animateScrollToItem(0) } },
              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
          )
  }
}

@Composable
private fun artistSortLabel(sort: ArtistSort) = when (sort) {
    ArtistSort.NAME_ASC, ArtistSort.NAME_DESC -> stringResource(R.string.name)
    ArtistSort.SONG_COUNT_ASC, ArtistSort.SONG_COUNT_DESC -> stringResource(R.string.songs)
    ArtistSort.ALBUM_COUNT_ASC, ArtistSort.ALBUM_COUNT_DESC -> stringResource(R.string.albums)
}

@Composable
private fun GenresTab(
    genres: List<GenreGroup>,
    sort: GenreSort,
    listState: LazyListState,
    onSortChange: (GenreSort) -> Unit,
    onGenreClick: (String) -> Unit,
    onPlay: (List<MusicTrack>) -> Unit,
    onShuffle: (List<MusicTrack>) -> Unit,
    onAddToQueue: (List<MusicTrack>) -> Unit,
    selectedGenreNames: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    var showSort by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SectionSortSheet(
        visible = showSort, title = stringResource(R.string.sort_genres), selected = sort,
        choices = listOf(
            GenreSort.NAME_ASC to stringResource(R.string.sort_genre_az), GenreSort.NAME_DESC to stringResource(R.string.sort_genre_za),
            GenreSort.SONG_COUNT_ASC to stringResource(R.string.sort_count_ascending), GenreSort.SONG_COUNT_DESC to stringResource(R.string.sort_count_descending)
        ), onSelect = onSortChange, onDismiss = { showSort = false }
    )
    if (genres.isEmpty()) { EmptyState(stringResource(R.string.no_genre_metadata)); return }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionToolbar(pluralStringResource(R.plurals.genre_count, genres.size, genres.size), genreSortLabel(sort), onSortClick = { showSort = true }) }
            items(genres.size, key = { genres[it].name.lowercase() }) { index ->
                val genre = genres[index]
                val selected = genre.name in selectedGenreNames
                val toggleSelection = { onSelectionChange(if (selected) selectedGenreNames - genre.name else selectedGenreNames + genre.name) }
                SimpleListRow(
                    letter = genre.name.take(1).uppercase(), primary = genre.name, secondary = pluralStringResource(R.plurals.song_count, genre.tracks.size, genre.tracks.size),
                    color = avatarColorFor(genre.name), artworkUri = genre.tracks.firstOrNull()?.uri,
                    selected = selected, selectionMode = selectedGenreNames.isNotEmpty(),
                    onClick = { if (selectedGenreNames.isNotEmpty()) toggleSelection() else onGenreClick(genre.name) },
                    onLongClick = toggleSelection, onPlay = { onPlay(genre.tracks) },
                    onShuffle = { onShuffle(genre.tracks) }, onAddToQueue = { onAddToQueue(genre.tracks) }
                )
            }
        }
        ScrollToTopButton(
            visible = listState.firstVisibleItemIndex > 4,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
        )
    }
}

@Composable
private fun genreSortLabel(sort: GenreSort) = when (sort) {
    GenreSort.NAME_ASC, GenreSort.NAME_DESC -> stringResource(R.string.name)
    GenreSort.SONG_COUNT_ASC, GenreSort.SONG_COUNT_DESC -> stringResource(R.string.songs)
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleListRow(
    letter: String,
    primary: String,
    secondary: String,
    color: Color,
    artworkUri: String? = null,
    isCircle: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val shape = if (isCircle) CircleShape else RoundedCornerShape(10.dp)
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) XargooshTheme.colors.primaryContainer else XargooshTheme.colors.surfaceVariant)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).clip(shape).background(color), contentAlignment = Alignment.Center) {
                Text(letter, color = if (color.luminance() > 0.45f) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                artworkUri?.let { AudioThumbnail(it, modifier = Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(primary, color = XargooshTheme.colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(secondary, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            }
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = XargooshTheme.colors.primary)
                )
            } else Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options), tint = XargooshTheme.colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, onClick = { showMenu = false; onClick() })
                    onPlay?.let { action -> DropdownMenuItem(text = { Text(stringResource(R.string.play)) }, onClick = { showMenu = false; action() }) }
                    onShuffle?.let { action -> DropdownMenuItem(text = { Text(stringResource(R.string.shuffle)) }, onClick = { showMenu = false; action() }) }
                    onAddToQueue?.let { action -> DropdownMenuItem(text = { Text(stringResource(R.string.add_all_to_queue)) }, onClick = { showMenu = false; action() }) }
                    onRename?.let { action -> DropdownMenuItem(text = { Text(stringResource(if (isCircle) R.string.edit_artist else R.string.edit_album)) }, onClick = { showMenu = false; action() }) }
                }
            }
        }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = XargooshTheme.colors.onSurfaceVariant, fontSize = 16.sp)
    }
}




