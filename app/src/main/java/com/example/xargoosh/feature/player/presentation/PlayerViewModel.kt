package com.example.xargoosh.feature.player.presentation

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.example.xargoosh.domain.models.MusicTrack
import com.example.xargoosh.domain.queue.QueueItem
import com.example.xargoosh.domain.queue.QueueManager
import com.example.xargoosh.domain.visualizer.VisualizerRepository
import com.example.xargoosh.domain.visualizer.VisualizerState
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import com.example.xargoosh.service.XargooshMediaService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import com.example.xargoosh.data.local.MediaScanner
import com.example.xargoosh.data.local.db.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val saveStateMutex = Mutex()

    private val repository: com.example.xargoosh.data.DataRepository = com.example.xargoosh.data.DataRepositoryImpl(
        application,
        MediaScanner(application),
        AppDatabase.getDatabase(application)
    )

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingPlayRequest: Pair<List<MusicTrack>, Int>? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _albumPalette = MutableStateFlow<com.example.xargoosh.domain.visualizer.VisualizerPalette?>(null)
    val albumPalette: StateFlow<com.example.xargoosh.domain.visualizer.VisualizerPalette?> = _albumPalette.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()
    private val _currentQueueItemId = MutableStateFlow<String?>(null)
    val currentQueueItemId: StateFlow<String?> = _currentQueueItemId.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    val audioSessionId: StateFlow<Int> = VisualizerRepository.audioSessionIdFlow

    private val _sleepTimerMinutes = MutableStateFlow(
        application.getSharedPreferences("xargoosh_prefs", Context.MODE_PRIVATE)
            .getLong("sleep_timer_end", 0L)
            .takeIf { it > System.currentTimeMillis() }
            ?.let { ((it - System.currentTimeMillis() + 59_999L) / 60_000L).toInt() }
    )
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentTrackFavorite: StateFlow<Boolean> = _currentTrack
        .flatMapLatest { track ->
            if (track != null) repository.isFavorite(track.uri) else kotlinx.coroutines.flow.flowOf(false)
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    val currentQueue: StateFlow<List<QueueItem>> = QueueManager.currentQueue

    val libraryTracks: StateFlow<List<MusicTrack>> = repository.getAllTracksAsc().stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )



    val visualizerSettings: com.example.xargoosh.domain.visualizer.VisualizerSettings by lazy {
        com.example.xargoosh.domain.visualizer.VisualizerSettings.getInstance(application)
    }

    private val adaptiveQualityMonitor: com.example.xargoosh.core.visualizer.effects.AdaptiveQualityMonitor by lazy {
        com.example.xargoosh.core.visualizer.effects.AdaptiveQualityMonitor(application)
    }

    private val visualizerEngine: com.example.xargoosh.core.visualizer.effects.VisualizerEngine by lazy {
        com.example.xargoosh.core.visualizer.effects.VisualizerEngine(
            repository = VisualizerRepository,
            settings = visualizerSettings,
            qualityMonitor = adaptiveQualityMonitor,
            scope = viewModelScope
        )
    }

    val visualizerState: StateFlow<VisualizerState> get() = visualizerEngine.visualizerState

    val currentVisualizerStyle: StateFlow<VisualizerStyle> = visualizerSettings.style.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = VisualizerStyle.NIER_WAVE
    )

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch { visualizerSettings.setStyle(style) }
    }

    fun setTabbedPlayerLayout(enabled: Boolean) {
        viewModelScope.launch { visualizerSettings.setUseTabbedPlayerLayout(enabled) }
    }

    fun setVisualizerIntensity(value: Float) {
        viewModelScope.launch { visualizerSettings.setIntensity(value) }
    }

    fun setVisualizerBlurStrength(value: Float) {
        viewModelScope.launch { visualizerSettings.setBlurStrength(value) }
    }

    fun setVisualizerRenderSize(value: Float) {
        viewModelScope.launch { visualizerSettings.setRenderSize(value) }
    }

    fun resetVisualizerEffects() {
        viewModelScope.launch { visualizerSettings.resetEffects() }
    }

    init {
        initializeController()
        startPositionPolling()

        viewModelScope.launch { visualizerEngine }

        viewModelScope.launch {
            _currentTrack.collectLatest { track ->
                if (track?.uri != null) { extractPalette(track.uri)
                } else {
                    _albumPalette.value = null
                    visualizerEngine.updateAlbumPalette(null)
                }
            }
        }
    }

        private fun extractPalette(uri: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var bitmap: android.graphics.Bitmap? = null
                if (uri.startsWith("content://")) {
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(getApplication(), android.net.Uri.parse(uri))
                        val art = retriever.embeddedPicture
                        if (art != null) {
                            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size, bounds)
                            var sampleSize = 1
                            while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) {
                                sampleSize *= 2
                            }
                            bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                art, 0, art.size,
                                android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                            )
                        }
                    } finally {
                        retriever.release()
                    }
                }

                if (bitmap != null) {
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    val primary = palette.getVibrantColor(palette.getDominantColor(android.graphics.Color.DKGRAY))
                    val secondary = palette.getMutedColor(palette.getDominantColor(android.graphics.Color.GRAY))
                    val glow = palette.getLightVibrantColor(primary)
                    val accent = palette.getDarkVibrantColor(secondary)
                    
                    val p = com.example.xargoosh.domain.visualizer.VisualizerPalette(
                        primary = androidx.compose.ui.graphics.Color(primary),
                        secondary = androidx.compose.ui.graphics.Color(secondary),
                        glow = androidx.compose.ui.graphics.Color(glow),
                        background = androidx.compose.ui.graphics.Color.Black,
                        accent = androidx.compose.ui.graphics.Color(accent)
                    )
                    _albumPalette.value = p
                    visualizerEngine.updateAlbumPalette(p)
                    bitmap.recycle()
                } else {
                    _albumPalette.value = null
                    visualizerEngine.updateAlbumPalette(null)
                }
            } catch (e: Exception) {
                _albumPalette.value = null
                visualizerEngine.updateAlbumPalette(null)
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        val endTime = minutes?.takeIf { it > 0 }?.let { System.currentTimeMillis() + it * 60_000L } ?: 0L
        mediaController?.sendCustomCommand(
            XargooshMediaService.COMMAND_SET_SLEEP_TIMER,
            Bundle().apply { putLong(XargooshMediaService.EXTRA_SLEEP_TIMER_END, endTime) }
        )
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isConnected && controller.isPlaying) {
                    _currentPosition.value = controller.currentPosition
                    if (controller.currentPosition % 5000 in 0..200) saveState()
                }
                delay(200L)
            }
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), XargooshMediaService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()
                    setupControllerListeners()
                    viewModelScope.launch {
                        val controller = mediaController ?: return@launch
                        if (controller.mediaItemCount == 0) restoreState() else adoptControllerState(controller)
                        synchronizeVisibleQueue(controller.shuffleModeEnabled)
                        pendingPlayRequest?.also { pendingPlayRequest = null }?.let { (tracks, index) ->
                            playQueue(tracks, index)
                        }
                    }
                } catch (_: Exception) {}
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    private suspend fun restoreState() {
        val restored = QueueManager.restoreState(getApplication())
        if (restored.queue.isNotEmpty() && restored.track != null) {
            _currentTrack.value = restored.track
            _currentPosition.value = restored.position
            _shuffleEnabled.value = restored.shuffleEnabled
            val controller = mediaController ?: return
            val playbackQueue = if (restored.shuffleEnabled) restored.normalQueue else restored.queue
            val mediaItems = playbackQueue.map { q -> createMediaItem(q.id, q.track) }
            val startIndex = playbackQueue.indexOfFirst { it.id == restored.currentItemId }
                .takeIf { it >= 0 }
                ?: playbackQueue.indexOfFirst { it.track.uri == restored.track.uri }.coerceAtLeast(0)
            _currentQueueIndex.value = startIndex
            _currentQueueItemId.value = playbackQueue.getOrNull(startIndex)?.id
            controller.setMediaItems(mediaItems, startIndex, restored.position)
            controller.shuffleModeEnabled = restored.shuffleEnabled
            controller.prepare()
        }
    }

    private fun saveState() {
        viewModelScope.launch {
            saveStateMutex.withLock {
                QueueManager.saveState(
                    getApplication(),
                    _currentTrack.value,
                    _currentPosition.value,
                    mediaController?.currentMediaItem?.mediaId,
                    _shuffleEnabled.value
                )
            }
            runCatching {
                com.example.xargoosh.widget.updateAllXargooshWidgets(getApplication())
            }
        }
    }

    private fun setupControllerListeners() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        visualizerEngine.updatePlayingState(controller.isPlaying)
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _currentQueueIndex.value = controller.currentMediaItemIndex.takeIf { it >= 0 } ?: -1
        _currentQueueItemId.value = controller.currentMediaItem?.mediaId
        _repeatMode.value = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> 1
            Player.REPEAT_MODE_ONE -> 2
            else -> 0
        }
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                visualizerEngine.updatePlayingState(isPlaying)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> 1
                    Player.REPEAT_MODE_ONE -> 2
                    else -> 0
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
                viewModelScope.launch { synchronizeVisibleQueue(shuffleModeEnabled) }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (controller.shuffleModeEnabled) {
                    viewModelScope.launch { synchronizeVisibleQueue(true) }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPosition.value = newPosition.positionMs
                val newId = newPosition.mediaItemIndex
                    .takeIf { it >= 0 && it < controller.mediaItemCount }
                    ?.let { controller.getMediaItemAt(it).mediaId }
                _currentQueueIndex.value = currentQueue.value.indexOfFirst { it.id == newId }
                _currentQueueItemId.value = newId
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                visualizerEngine.resetBeatTracking()
                val id = mediaItem?.mediaId
                _currentQueueItemId.value = id
                _currentQueueIndex.value = currentQueue.value.indexOfFirst { it.id == id }
                currentQueue.value.find { it.id == id }?.let { _currentTrack.value = it.track }
                _currentPosition.value = controller.currentPosition
                saveState()
            }
        })
    }

    private fun createMediaItem(id: String, track: MusicTrack): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(track.uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(if (track.albumArtUri != null) android.net.Uri.parse(track.albumArtUri) else null)
                    .build()
            )
            .build()
    }

    private suspend fun synchronizeVisibleQueue(shuffleEnabled: Boolean) {
        val controller = mediaController ?: return
        if (shuffleEnabled) {
            val timeline = controller.currentTimeline
            val orderedIds = mutableListOf<String>()
            val seen = mutableSetOf<Int>()
            var index = timeline.getFirstWindowIndex(true)
            while (index != androidx.media3.common.C.INDEX_UNSET && seen.add(index)) {
                if (index in 0 until controller.mediaItemCount) {
                    orderedIds += controller.getMediaItemAt(index).mediaId
                }
                index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
            }
            QueueManager.applyPlaybackOrder(orderedIds)
        } else {
            QueueManager.restoreNormalOrder()
        }
        val currentId = controller.currentMediaItem?.mediaId
        _currentQueueIndex.value = currentQueue.value.indexOfFirst { it.id == currentId }
        saveState()
    }

    private fun updateControllerQueue(queue: List<QueueItem>) {
        val controller = mediaController ?: return
        val currentId = controller.currentMediaItem?.mediaId
        val position = controller.currentPosition
        val playWhenReady = controller.playWhenReady
        val byId = queue.associateBy { it.id }
        val controllerOrder = (0 until controller.mediaItemCount).mapNotNull { index ->
            byId[controller.getMediaItemAt(index).mediaId]
        }.ifEmpty { queue }
        val mediaItems = controllerOrder.map { createMediaItem(it.id, it.track) }
        val newIndex = controllerOrder.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        controller.setMediaItems(mediaItems, newIndex, position)
        controller.prepare()
        controller.playWhenReady = playWhenReady
        _currentQueueIndex.value = newIndex
        _currentQueueItemId.value = currentId
    }

    fun playQueue(tracks: List<MusicTrack>, startIndex: Int) {
        if (mediaController == null) {
            pendingPlayRequest = tracks to startIndex
            return
        }
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            if (tracks.isEmpty()) return@launch
            val safeIndex = startIndex.coerceIn(tracks.indices)
            val (finalQueue, newStartIndex) = QueueManager.setQueue(tracks, safeIndex, false)
            val mediaItems = finalQueue.map { createMediaItem(it.id, it.track) }
            controller.setMediaItems(mediaItems, newStartIndex, 0L)
            controller.shuffleModeEnabled = _shuffleEnabled.value
            controller.prepare()
            controller.play()
            _currentQueueIndex.value = newStartIndex
            _currentQueueItemId.value = finalQueue.getOrNull(newStartIndex)?.id
            if (_shuffleEnabled.value) synchronizeVisibleQueue(true) else saveState()
        }
    }

    fun playNext(track: MusicTrack) {
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            if (currentQueue.value.isEmpty()) {
                playQueue(listOf(track), 0)
                return@launch
            }
            val currentId = controller.currentMediaItem?.mediaId
            val result = QueueManager.putNext(track, currentId, _shuffleEnabled.value)
            if (!result.changed) return@launch
            if (_shuffleEnabled.value) {
                if (result.wasAdded) controller.addMediaItem(createMediaItem(result.item.id, result.item.track))
                applyVisibleShuffleOrder(result.queue)
            } else {
                val currentControllerIndex = controller.currentMediaItemIndex
                val existingIndex = controllerIndexFor(result.item.id)
                val targetIndex = (
                    if (existingIndex in 0 until currentControllerIndex) currentControllerIndex
                    else currentControllerIndex + 1
                ).coerceIn(0, controller.mediaItemCount)
                if (result.wasAdded || existingIndex < 0) {
                    controller.addMediaItem(targetIndex, createMediaItem(result.item.id, result.item.track))
                } else if (existingIndex != targetIndex) {
                    controller.moveMediaItem(existingIndex, targetIndex)
                }
            }
            saveState()
        }
    }

    fun addToQueue(track: MusicTrack) {
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            if (currentQueue.value.isEmpty()) {
                playQueue(listOf(track), 0)
                return@launch
            }
            val (queue, qItem) = QueueManager.addToEnd(track)
            controller.addMediaItem(createMediaItem(qItem.id, qItem.track))
            if (_shuffleEnabled.value) applyVisibleShuffleOrder(queue)
            saveState()
        }
    }

    fun toggleShuffle() {
        setShuffleEnabled(!_shuffleEnabled.value)
    }

    fun retryVisualizerCapture() {
        mediaController?.sendCustomCommand(XargooshMediaService.COMMAND_RETRY_VISUALIZER, Bundle.EMPTY)
    }

    fun addAllToQueue(tracks: List<MusicTrack>) {
        viewModelScope.launch {
            val controller = mediaController ?: return@launch
            if (tracks.isEmpty()) return@launch
            if (currentQueue.value.isEmpty()) {
                playQueue(tracks, 0)
                return@launch
            }
            val items = tracks.map { track ->
                val (_, item) = QueueManager.addToEnd(track)
                createMediaItem(item.id, item.track)
            }
            controller.addMediaItems(items)
            if (_shuffleEnabled.value) applyVisibleShuffleOrder(currentQueue.value)
            saveState()
        }
    }

    fun refreshTrackMetadata(tracks: List<MusicTrack>) {
        viewModelScope.launch {
            val updates = tracks.associateBy { it.uri }
            val queue = QueueManager.updateTracks(updates)
            _currentTrack.value?.let { current ->
                updates[current.uri]?.let { _currentTrack.value = it }
            }
            if (queue.isNotEmpty()) updateControllerQueue(queue)
            saveState()
        }
    }

    private fun adoptControllerState(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        _currentQueueIndex.value = controller.currentMediaItemIndex.takeIf { it >= 0 } ?: -1
        _currentQueueItemId.value = controller.currentMediaItem?.mediaId
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> 1
            Player.REPEAT_MODE_ONE -> 2
            else -> 0
        }
        controller.currentMediaItem?.mediaId?.let { id ->
            currentQueue.value.find { it.id == id }?.let { _currentTrack.value = it.track }
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        val controller = mediaController ?: return
        if (controller.shuffleModeEnabled == enabled) {
            viewModelScope.launch { synchronizeVisibleQueue(enabled) }
            return
        }
        controller.shuffleModeEnabled = enabled
        _shuffleEnabled.value = enabled
    }

    fun playShuffled(tracks: List<MusicTrack>) {
        _shuffleEnabled.value = true
        playQueue(tracks, 0)
    }

    fun toggleLoop() {
        mediaController?.let { controller ->
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _repeatMode.value = when (nextMode) {
                Player.REPEAT_MODE_ONE -> 2
                Player.REPEAT_MODE_ALL -> 1
                else -> 0
            }
            if (nextMode != Player.REPEAT_MODE_OFF && controller.playbackState == Player.STATE_ENDED) {
                controller.seekToDefaultPosition(if (nextMode == Player.REPEAT_MODE_ONE) controller.currentMediaItemIndex else 0)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun removeQueueItem(itemId: String) {
        viewModelScope.launch {
            val index = currentQueue.value.indexOfFirst { it.id == itemId }
            if (index < 0) return@launch
            val item = currentQueue.value.getOrNull(index) ?: return@launch
            val controllerIndex = controllerIndexFor(item.id)
            QueueManager.removeAt(index) ?: return@launch
            if (controllerIndex >= 0) mediaController?.removeMediaItem(controllerIndex)
            if (currentQueue.value.isEmpty()) {
                _currentTrack.value = null
                _currentPosition.value = 0L
                _currentQueueIndex.value = -1
                _currentQueueItemId.value = null
            } else {
                val currentId = mediaController?.currentMediaItem?.mediaId
                _currentQueueIndex.value = currentQueue.value.indexOfFirst { it.id == currentId }
                _currentQueueItemId.value = currentId
            }
            saveState()
        }
    }

    fun removeTrackFromQueue(trackUri: String) {
        viewModelScope.launch {
            val items = currentQueue.value.filter { it.track.uri == trackUri }
            val indexes = currentQueue.value.mapIndexedNotNull { index, item -> index.takeIf { item.track.uri == trackUri } }
            indexes.asReversed().forEach { index ->
                QueueManager.removeAt(index)
            }
            items.map { controllerIndexFor(it.id) }
                .filter { it >= 0 }
                .sortedDescending()
                .forEach { mediaController?.removeMediaItem(it) }
            if (currentQueue.value.isEmpty()) {
                _currentTrack.value = null
                _currentPosition.value = 0L
                _currentQueueIndex.value = -1
                _currentQueueItemId.value = null
            }
            saveState()
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            QueueManager.clear()
            mediaController?.clearMediaItems()
            mediaController?.stop()
            _currentTrack.value = null
            _currentPosition.value = 0L
            _currentQueueIndex.value = -1
            _currentQueueItemId.value = null
            saveState()
        }
    }

    fun playQueueItem(itemId: String) {
        val controller = mediaController ?: return
        val index = currentQueue.value.indexOfFirst { it.id == itemId }
        if (index < 0) return
        val controllerIndex = controllerIndexFor(itemId)
        if (controllerIndex < 0) return
        controller.seekToDefaultPosition(controllerIndex)
        controller.play()
        _currentQueueIndex.value = index
        _currentQueueItemId.value = itemId
    }

    private fun controllerIndexFor(itemId: String): Int {
        val controller = mediaController ?: return -1
        return (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).mediaId == itemId
        } ?: -1
    }

    private fun applyVisibleShuffleOrder(queue: List<QueueItem>) {
        val ids = ArrayList(queue.map { it.id })
        mediaController?.sendCustomCommand(
            XargooshMediaService.COMMAND_SET_SHUFFLE_ORDER,
            Bundle().apply { putStringArrayList(XargooshMediaService.EXTRA_QUEUE_IDS, ids) }
        )
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentId = mediaController?.currentMediaItem?.mediaId
            QueueManager.move(fromIndex, toIndex, _shuffleEnabled.value)
            if (_shuffleEnabled.value) {
                applyVisibleShuffleOrder(currentQueue.value)
            } else {
                mediaController?.moveMediaItem(fromIndex, toIndex)
            }
            _currentQueueIndex.value = currentQueue.value.indexOfFirst { it.id == currentId }
            saveState()
        }
    }

    fun playPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun toggleFavorite() {
        val currentTrackUri = _currentTrack.value?.uri ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentTrackUri)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
        saveState()
    }

    fun skipToNext() { mediaController?.seekToNextMediaItem() }

    fun skipToPrevious() { mediaController?.seekToPreviousMediaItem() }

    override fun onCleared() {
        super.onCleared()
        visualizerEngine.stop()
        adaptiveQualityMonitor.release()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}





