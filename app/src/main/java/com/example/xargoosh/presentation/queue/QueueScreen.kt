package com.example.xargoosh.presentation.queue

import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import com.example.xargoosh.presentation.components.AudioThumbnail
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.xargoosh.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val queue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val currentItemId by viewModel.currentQueueItemId.collectAsStateWithLifecycle()
    var displayQueue by remember { mutableStateOf(queue) }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        displayQueue = displayQueue.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.moveQueueItem(from.index, to.index)
    }
    LaunchedEffect(queue) {
        if (displayQueue.map { it.id } != queue.map { it.id }) displayQueue = queue
    }

    Scaffold(
        containerColor = XargooshTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.queue), fontWeight = FontWeight.SemiBold)
                        Text(pluralStringResource(R.plurals.track_count, queue.size, queue.size), style = MaterialTheme.typography.labelMedium, color = XargooshTheme.colors.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearQueue, enabled = queue.isNotEmpty()) {
                        Icon(Icons.Default.ClearAll, contentDescription = stringResource(R.string.clear_queue))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XargooshTheme.colors.background)
            )
        }
    ) { padding ->
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.queue_empty), color = XargooshTheme.colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(displayQueue, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(reorderableState, key = item.id) { isDragging ->
                        val active = item.id == currentItemId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isDragging -> XargooshTheme.colors.primary.copy(alpha = 0.2f)
                                        active -> XargooshTheme.colors.primary.copy(alpha = 0.1f)
                                        else -> XargooshTheme.colors.surface
                                    }
                                )
                                .longPressDraggableHandle()
                                .clickable { viewModel.playQueueItem(item.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        AudioThumbnail(
                            uri = item.track.uri,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.track.title, color = if (active) XargooshTheme.colors.primary else XargooshTheme.colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.track.artist, color = XargooshTheme.colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (active) Icon(Icons.Default.GraphicEq, contentDescription = stringResource(R.string.playing), tint = XargooshTheme.colors.primary)
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.reorder_queue_hint),
                            tint = XargooshTheme.colors.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.removeQueueItem(item.id) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_from_queue), tint = XargooshTheme.colors.onSurfaceVariant)
                        }
                        }
                    }
                }
            }
        }
    }
}
