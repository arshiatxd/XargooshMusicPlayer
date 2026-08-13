package com.example.xargoosh.core.visualizer.effects

import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.AeroFloatingActionButton as FloatingActionButton

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.feature.player.presentation.PlayerViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.example.xargoosh.R

@Composable
fun FullscreenVisualizerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val visualizerState by viewModel.visualizerState.collectAsStateWithLifecycle()
    val currentStyle by viewModel.currentVisualizerStyle.collectAsStateWithLifecycle()
    val visualizerSettings = viewModel.visualizerSettings
    val enabled by visualizerSettings.enabled.collectAsStateWithLifecycle(initialValue = false)
    val renderSize by visualizerSettings.renderSize.collectAsStateWithLifecycle(initialValue = 1f)
    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
    }
    val touchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled == true
    val showPlaybackControlsLabel = stringResource(R.string.show_playback_controls)

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls, touchExplorationEnabled) {
        if (showControls && !touchExplorationEnabled) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {


        LyricsVisualizerBg(
            albumArtUri = currentTrack?.uri,
            visualizerState = visualizerState,
            style = currentStyle,
            blurStrength = 0f,
            enabled = enabled,
            isLyricsMode = false,
            renderSize = renderSize
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showControls = true }
                )
                .semantics {
                    onClick(showPlaybackControlsLabel) {
                        showControls = true
                        true
                    }
                }
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }


                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    currentTrack?.let { track ->
                        Text(
                            text = track.title,
                            style = XargooshTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = XargooshTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.skipToPrevious() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.previous),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        FloatingActionButton(
                            onClick = { viewModel.playPause() },
                            containerColor = XargooshTheme.colors.primary,
                            contentColor = XargooshTheme.colors.onPrimary,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = { viewModel.skipToNext() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.next),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

