package com.example.xargoosh.feature.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.xargoosh.R
import com.example.xargoosh.core.components.surface.AeroButton as Button
import com.example.xargoosh.core.components.surface.AeroIconButton as IconButton
import com.example.xargoosh.core.components.surface.GlassSurface
import com.example.xargoosh.core.design.themes.XargooshTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Public AudD demo token, limited to 10 recognition requests per day.
// Keep this isolated so production integration can replace it with secured configuration.
private const val AUDD_PUBLIC_DEMO_TOKEN = "test"

sealed interface MusicRecognitionUiState {
    data object Idle : MusicRecognitionUiState
    data object Listening : MusicRecognitionUiState
    data object Matching : MusicRecognitionUiState
    data class Match(val music: RecognizedMusic) : MusicRecognitionUiState
    data object NoMatch : MusicRecognitionUiState
    data object PermissionDenied : MusicRecognitionUiState
    data class Error(val error: RecognitionError? = null) : MusicRecognitionUiState
}

class MusicRecognitionViewModel(application: Application) : AndroidViewModel(application) {
    private val isScreenVisible = AtomicBoolean(false)
    private val repository = MusicRecognitionRepository(
        context = application,
        apiToken = AUDD_PUBLIC_DEMO_TOKEN,
        isAppInForeground = isScreenVisible::get,
    )
    private val _uiState = MutableStateFlow<MusicRecognitionUiState>(MusicRecognitionUiState.Idle)
    val uiState: StateFlow<MusicRecognitionUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    private val activeRequestId = AtomicLong(0L)

    fun onScreenVisible() {
        isScreenVisible.set(true)
    }

    fun onScreenDisposed() {
        isScreenVisible.set(false)
        cancelRecognition()
    }

    fun onPermissionDenied() {
        if (isScreenVisible.get() && !_uiState.value.isWorking) {
            _uiState.value = MusicRecognitionUiState.PermissionDenied
        }
    }

    fun onPermissionRequired() {
        if (isScreenVisible.get() && !_uiState.value.isWorking) {
            _uiState.value = MusicRecognitionUiState.PermissionDenied
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecognition() {
        if (!isScreenVisible.get() || recognitionJob?.isActive == true) return

        val requestId = activeRequestId.incrementAndGet()
        _uiState.value = MusicRecognitionUiState.Listening
        recognitionJob = viewModelScope.launch {
            try {
                val result = repository.recognize(
                    onCaptureFinished = {
                        if (isScreenVisible.get() && activeRequestId.get() == requestId) {
                            _uiState.update { current ->
                                if (current == MusicRecognitionUiState.Listening) {
                                    MusicRecognitionUiState.Matching
                                } else {
                                    current
                                }
                            }
                        }
                    },
                )
                if (isScreenVisible.get() && activeRequestId.get() == requestId) {
                    _uiState.value = result.toUiState()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (isScreenVisible.get() && activeRequestId.get() == requestId) {
                    _uiState.value = MusicRecognitionUiState.Error()
                }
            } finally {
                if (activeRequestId.get() == requestId) recognitionJob = null
            }
        }
    }

    fun cancelRecognition() {
        activeRequestId.incrementAndGet()
        recognitionJob?.cancel()
        recognitionJob = null
        _uiState.value = MusicRecognitionUiState.Idle
    }

    override fun onCleared() {
        isScreenVisible.set(false)
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRecognitionScreen(
    onBack: () -> Unit,
    isPlaybackActive: Boolean,
    onPausePlayback: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicRecognitionViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasMicrophone = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }
    val currentPlaybackActive by rememberUpdatedState(isPlaybackActive)
    val currentPausePlayback by rememberUpdatedState(onPausePlayback)

    DisposableEffect(lifecycleOwner, viewModel) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> viewModel.onScreenDisposed()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onScreenVisible()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onScreenDisposed()
        }
    }

    val startCapture = {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            if (currentPlaybackActive) currentPausePlayback()
            viewModel.startRecognition()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startCapture() else viewModel.onPermissionDenied()
    }
    val microphonePermissionGranted =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val primaryAction = {
        if (state.isWorking) {
            viewModel.cancelRecognition()
        } else if (hasMicrophone) {
            if (microphonePermissionGranted) {
                startCapture()
            } else {
                viewModel.onPermissionRequired()
            }
        }
    }

    val statusText = stringResource(
        if (hasMicrophone) state.statusResource() else R.string.music_recognition_microphone_unavailable,
    )
    val actionDescription = stringResource(
        when {
            !hasMicrophone -> R.string.music_recognition_microphone_unavailable
            state.isWorking -> R.string.music_recognition_cancel
            else -> R.string.music_recognition_start
        },
    )

    Scaffold(
        modifier = modifier,
        containerColor = XargooshTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.music_recognition_title),
                        color = XargooshTheme.colors.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = XargooshTheme.colors.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = XargooshTheme.colors.background,
                ),
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecognitionControl(
                    working = state.isWorking,
                    enabled = hasMicrophone,
                    actionDescription = actionDescription,
                    stateDescription = statusText,
                    onClick = primaryAction,
                )

                Text(
                    text = statusText,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        !hasMicrophone || state is MusicRecognitionUiState.PermissionDenied ||
                            state is MusicRecognitionUiState.Error -> XargooshTheme.colors.error
                        state is MusicRecognitionUiState.Match -> XargooshTheme.colors.primary
                        else -> XargooshTheme.colors.onSurface
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                PrivacyDisclosure()

                if (hasMicrophone && !microphonePermissionGranted) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = XargooshTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.music_recognition_allow_microphone))
                    }
                }

                (state as? MusicRecognitionUiState.Match)?.let { match ->
                    MatchContent(music = match.music)
                }

                if (
                    hasMicrophone && (
                        state is MusicRecognitionUiState.NoMatch ||
                            state is MusicRecognitionUiState.PermissionDenied ||
                            state is MusicRecognitionUiState.Error
                        )
                ) {
                    Button(
                        onClick = primaryAction,
                        modifier = Modifier.fillMaxWidth(),
                        shape = XargooshTheme.shapes.medium,
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.music_recognition_retry))
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecognitionControl(
    working: Boolean,
    enabled: Boolean,
    actionDescription: String,
    stateDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RECOGNITION_CONTROL_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        if (working) {
            val transition = rememberInfiniteTransition(label = "recognition pulse")
            val pulse by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1_800),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "recognition pulse progress",
            )
            PulsingRings(progress = pulse, modifier = Modifier.size(RECOGNITION_CONTROL_SIZE))
        }

        Button(
            onClick = onClick,
            modifier = Modifier
                .size(RECOGNITION_BUTTON_SIZE)
                .semantics(mergeDescendants = true) {
                    contentDescription = actionDescription
                    this.stateDescription = stateDescription
                },
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(18.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun PulsingRings(progress: Float, modifier: Modifier = Modifier) {
    val color = XargooshTheme.colors.primary
    Canvas(modifier = modifier) {
        val maximumRadius = size.minDimension / 2f
        val strokeWidth = 2.dp.toPx()
        repeat(3) { index ->
            val phase = (progress + index / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.42f),
                radius = maximumRadius * (0.64f + phase * 0.34f),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
private fun PrivacyDisclosure() {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = XargooshTheme.shapes.large,
        color = XargooshTheme.colors.surfaceVariant,
        borderColor = XargooshTheme.colors.glassBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = XargooshTheme.colors.primary,
            )
            Text(
                text = stringResource(R.string.music_recognition_privacy_disclosure),
                modifier = Modifier.weight(1f),
                color = XargooshTheme.colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MatchContent(music: RecognizedMusic) {
    val context = LocalContext.current
    val artworkUri = remember(music.artworkUrl) { music.artworkUrl.toHttpsUriOrNull() }
    val resultUri = remember(music) {
        listOf(
            music.songLink,
            music.spotifyUrl,
            music.appleMusicUrl,
            music.deezerUrl,
        ).firstNotNullOfOrNull { it.toHttpsUriOrNull() }
    }
    val searchUri = remember(music.artist, music.title) {
        Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .appendPath("search")
            .appendQueryParameter("q", "${music.artist} ${music.title}")
            .build()
    }
    val fallbackArtwork = painterResource(R.mipmap.ic_launcher_round)

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = XargooshTheme.shapes.large,
        color = XargooshTheme.colors.surface,
        borderColor = XargooshTheme.colors.glassBorder,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val artworkSize = maxWidth.coerceAtMost(220.dp)
                AsyncImage(
                    model = artworkUri,
                    contentDescription = stringResource(R.string.album_artwork),
                    placeholder = fallbackArtwork,
                    error = fallbackArtwork,
                    fallback = fallbackArtwork,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(24.dp)),
                )
            }

            Text(
                text = music.title,
                modifier = Modifier.fillMaxWidth(),
                color = XargooshTheme.colors.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = music.artist,
                modifier = Modifier.fillMaxWidth(),
                color = XargooshTheme.colors.primary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            music.album?.takeIf(String::isNotBlank)?.let { album ->
                MatchDetail(label = stringResource(R.string.album), value = album)
            }
            music.releaseDate?.takeIf(String::isNotBlank)?.let { releaseDate ->
                MatchDetail(
                    label = stringResource(R.string.music_recognition_release_date),
                    value = releaseDate,
                )
            }

            MatchActions(
                canOpenResult = resultUri != null,
                onOpenResult = { resultUri?.let { context.openHttps(it) } },
                onWebSearch = { context.openHttps(searchUri) },
            )
        }
    }
}

@Composable
private fun MatchDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.35f),
            color = XargooshTheme.colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.65f),
            color = XargooshTheme.colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MatchActions(
    canOpenResult: Boolean,
    onOpenResult: () -> Unit,
    onWebSearch: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 480.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MatchActionButton(
                    label = stringResource(R.string.music_recognition_open_result),
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    enabled = canOpenResult,
                    onClick = onOpenResult,
                    modifier = Modifier.weight(1f),
                )
                MatchActionButton(
                    label = stringResource(R.string.music_recognition_web_search),
                    icon = Icons.Default.Search,
                    enabled = true,
                    onClick = onWebSearch,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MatchActionButton(
                    label = stringResource(R.string.music_recognition_open_result),
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    enabled = canOpenResult,
                    onClick = onOpenResult,
                    modifier = Modifier.fillMaxWidth(),
                )
                MatchActionButton(
                    label = stringResource(R.string.music_recognition_web_search),
                    icon = Icons.Default.Search,
                    enabled = true,
                    onClick = onWebSearch,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MatchActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = XargooshTheme.shapes.medium,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text = label, textAlign = TextAlign.Center)
    }
}

private fun RecognitionResult.toUiState(): MusicRecognitionUiState = when (this) {
    is RecognitionResult.Match -> MusicRecognitionUiState.Match(music)
    RecognitionResult.NoMatch -> MusicRecognitionUiState.NoMatch
    is RecognitionResult.Failure -> when (error) {
        RecognitionError.PermissionDenied -> MusicRecognitionUiState.PermissionDenied
        else -> MusicRecognitionUiState.Error(error)
    }
}

private val MusicRecognitionUiState.isWorking: Boolean
    get() = this is MusicRecognitionUiState.Listening || this is MusicRecognitionUiState.Matching

@StringRes
private fun MusicRecognitionUiState.statusResource(): Int = when (this) {
    MusicRecognitionUiState.Idle -> R.string.music_recognition_idle
    MusicRecognitionUiState.Listening -> R.string.music_recognition_listening
    MusicRecognitionUiState.Matching -> R.string.music_recognition_matching
    is MusicRecognitionUiState.Match -> R.string.music_recognition_match_found
    MusicRecognitionUiState.NoMatch -> R.string.music_recognition_no_match
    MusicRecognitionUiState.PermissionDenied -> R.string.music_recognition_permission_denied
    is MusicRecognitionUiState.Error -> error.statusResource()
}

@StringRes
private fun RecognitionError?.statusResource(): Int = when (this) {
    null -> R.string.music_recognition_error
    RecognitionError.PermissionDenied -> R.string.music_recognition_permission_denied
    RecognitionError.AppNotInForeground -> R.string.music_recognition_error_not_foreground
    is RecognitionError.AudioInitialization,
    is RecognitionError.AudioRead,
    RecognitionError.EmptyCapture -> R.string.music_recognition_error_audio
    is RecognitionError.Network -> R.string.music_recognition_error_network
    is RecognitionError.HttpStatus,
    RecognitionError.ResponseTooLarge,
    is RecognitionError.AudD,
    is RecognitionError.InvalidResponse -> R.string.music_recognition_error_service
}

private fun String?.toHttpsUriOrNull(): Uri? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching { Uri.parse(value) }.getOrNull()?.takeIf { uri ->
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }
}

private fun Context.openHttps(uri: Uri) {
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    runCatching { startActivity(intent) }
}

private val RECOGNITION_CONTROL_SIZE: Dp = 224.dp
private val RECOGNITION_BUTTON_SIZE: Dp = 148.dp
