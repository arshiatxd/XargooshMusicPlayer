package com.example.xargoosh.feature.library.presentation

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroFloatingActionButton as FloatingActionButton

import com.example.xargoosh.core.design.themes.XargooshTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xargoosh.domain.models.MusicFolder
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R

@Composable
fun FoldersTab(
    viewModel: HomeViewModel,
    onFolderClick: (MusicFolder) -> Unit,
    onAddFolderClick: () -> Unit,
    listState: LazyListState,
    sort: FolderSort,
    onSortChange: (FolderSort) -> Unit,
    selectedFolderIds: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val sourceFolders by viewModel.explicitFolders.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()
    val trackCounts = remember(tracks) { tracks.groupingBy { it.folderPath }.eachCount() }
    val folders = remember(sourceFolders, sort, trackCounts) {
        when (sort) {
            FolderSort.NAME_ASC -> sourceFolders.sortedBy { it.name.lowercase() }
            FolderSort.NAME_DESC -> sourceFolders.sortedByDescending { it.name.lowercase() }
            FolderSort.SONG_COUNT_ASC -> sourceFolders.sortedBy { trackCounts[it.uriString] ?: 0 }
            FolderSort.SONG_COUNT_DESC -> sourceFolders.sortedByDescending { trackCounts[it.uriString] ?: 0 }
            FolderSort.DATE_ASC -> sourceFolders.sortedBy { it.dateAdded }
            FolderSort.DATE_DESC -> sourceFolders.sortedByDescending { it.dateAdded }
        }
    }
    var showSort by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SectionSortSheet(
        visible = showSort, title = stringResource(R.string.sort_folders), selected = sort,
        choices = listOf(
            FolderSort.NAME_ASC to stringResource(R.string.sort_name_az), FolderSort.NAME_DESC to stringResource(R.string.sort_name_za),
            FolderSort.SONG_COUNT_ASC to stringResource(R.string.sort_count_ascending), FolderSort.SONG_COUNT_DESC to stringResource(R.string.sort_count_descending),
            FolderSort.DATE_DESC to stringResource(R.string.sort_newest_added), FolderSort.DATE_ASC to stringResource(R.string.sort_oldest_added)
        ), onSelect = onSortChange, onDismiss = { showSort = false }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.no_folders_added),
                    color = XargooshTheme.colors.onSurfaceVariant,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.add_folder_hint),
                    color = XargooshTheme.colors.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionToolbar(pluralStringResource(R.plurals.folder_count, folders.size, folders.size), folderSortLabel(sort), onSortClick = { showSort = true }) }
                items(folders, key = { it.id }) { folder ->
                    val selected = folder.id in selectedFolderIds
                    val toggleSelection = { onSelectionChange(if (selected) selectedFolderIds - folder.id else selectedFolderIds + folder.id) }
                    FolderItem(
                        folder = folder,
                        trackCount = trackCounts[folder.uriString] ?: 0,
                        selected = selected,
                        selectionMode = selectedFolderIds.isNotEmpty(),
                        onClick = { if (selectedFolderIds.isNotEmpty()) toggleSelection() else onFolderClick(folder) },
                        onLongClick = toggleSelection,
                        onDeleteClick = { viewModel.deleteFolder(folder.id) }
                    )
                }
            }
        }

        if (selectedFolderIds.isEmpty()) {
            FloatingActionButton(
                onClick = onAddFolderClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 90.dp).navigationBarsPadding(),
                containerColor = XargooshTheme.colors.primary,
                shape = androidx.compose.foundation.shape.CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_folder))
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
private fun folderSortLabel(sort: FolderSort) = when (sort) {
    FolderSort.NAME_ASC, FolderSort.NAME_DESC -> stringResource(R.string.name)
    FolderSort.SONG_COUNT_ASC, FolderSort.SONG_COUNT_DESC -> stringResource(R.string.songs)
    FolderSort.DATE_ASC, FolderSort.DATE_DESC -> stringResource(R.string.date)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: MusicFolder,
    trackCount: Int,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) XargooshTheme.colors.primaryContainer else XargooshTheme.colors.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(XargooshTheme.colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = XargooshTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = XargooshTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(pluralStringResource(R.plurals.song_count, trackCount, trackCount), color = XargooshTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        }
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = XargooshTheme.colors.primary))
        } else {
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = XargooshTheme.colors.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}
