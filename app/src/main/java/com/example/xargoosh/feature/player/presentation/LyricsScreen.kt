package com.example.xargoosh.feature.player.presentation

import com.example.xargoosh.core.design.themes.XargooshTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.xargoosh.domain.visualizer.VisualizerSettings
import com.example.xargoosh.domain.models.LyricLine

@Composable
fun LyricsOverlay(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = XargooshTheme.colors.primary,
    inactiveColor: Color = XargooshTheme.colors.onBackground.copy(alpha = 0.82f)
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val reduceMotion by remember(context) { VisualizerSettings.getInstance(context).reduceMotion }
        .collectAsState(initial = false)
    val hasSyncedLyrics = remember(lyrics) { lyrics.any { it.isSynced } }
    var followCurrentLine by remember(lyrics) { mutableStateOf(true) }

    val activeIndex by remember(lyrics, hasSyncedLyrics, currentPositionMs) {
        derivedStateOf {
            if (lyrics.isEmpty() || !hasSyncedLyrics) -1
            else {
                lyrics.indexOfLast { it.timestampMs <= currentPositionMs }
            }
        }
    }

    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isUserDragging) {
        if (isUserDragging) followCurrentLine = false
    }
    LaunchedEffect(activeIndex, followCurrentLine) {
        if (followCurrentLine && activeIndex >= 0 && activeIndex < lyrics.size) {
            val target = maxOf(0, activeIndex - 2)
            if (reduceMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = hasSyncedLyrics && index == activeIndex
            val color by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                animationSpec = if (reduceMotion) snap() else tween(300),
                label = "lyricColor"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 1f,
                animationSpec = if (reduceMotion) snap() else tween(280),
                label = "lyricScale"
            )

            Text(
                text = line.text,
                color = color,
                fontSize = if (isActive) 23.sp else 16.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(enabled = line.isSynced) {
                        onSeekTo(line.timestampMs)
                        followCurrentLine = true
                    }
                    .padding(vertical = 7.dp)
            )
        }
    }
}
