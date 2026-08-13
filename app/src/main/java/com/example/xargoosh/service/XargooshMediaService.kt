package com.example.xargoosh.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class XargooshMediaService : MediaSessionService() {

    companion object {
        const val ACTION_RETRY_VISUALIZER = "com.xargoosh.music.action.RETRY_VISUALIZER"
        const val ACTION_SET_SLEEP_TIMER = "com.xargoosh.music.action.SET_SLEEP_TIMER"
        const val ACTION_SET_SHUFFLE_ORDER = "com.xargoosh.music.action.SET_SHUFFLE_ORDER"
        const val EXTRA_SLEEP_TIMER_END = "sleep_timer_end"
        const val EXTRA_QUEUE_IDS = "queue_ids"
        val COMMAND_RETRY_VISUALIZER = SessionCommand(ACTION_RETRY_VISUALIZER, Bundle.EMPTY)
        val COMMAND_SET_SLEEP_TIMER = SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)
        val COMMAND_SET_SHUFFLE_ORDER = SessionCommand(ACTION_SET_SHUFFLE_ORDER, Bundle.EMPTY)
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var visualizer: android.media.audiofx.Visualizer? = null
    private var visualizerEnabled = false
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
    )
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val activeListeningTracker = com.example.xargoosh.domain.playback.ActiveListeningTracker()
    private var activeListeningItemId: String? = null
    private var activeListeningJob: kotlinx.coroutines.Job? = null
    private lateinit var repository: com.example.xargoosh.data.DataRepository
    private var playerListener: Player.Listener? = null
    private lateinit var audioPreferences: com.example.xargoosh.domain.playback.AudioPlaybackPreferences
    private var audioConfig = com.example.xargoosh.domain.playback.AudioPlaybackConfig()
    private lateinit var volumeCoordinator: com.example.xargoosh.domain.playback.PlaybackVolumeCoordinator
    private var pauseFadeJob: kotlinx.coroutines.Job? = null
    private var transitionFadeJob: kotlinx.coroutines.Job? = null
    private var transitionMonitorJob: kotlinx.coroutines.Job? = null
    private var playbackPersistenceJob: kotlinx.coroutines.Job? = null
    private val pendingPlayCountJobs = java.util.Collections.synchronizedSet(mutableSetOf<kotlinx.coroutines.Job>())
    private var userWantsPlayback = false
    private var destroyed = false

    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (!::player.isInitialized) return@OnSharedPreferenceChangeListener
        if (audioPreferences.isPauseOnOtherAudioKey(key)) {
            val pauseOnOtherAudio = audioPreferences.pauseOnOtherAudio()
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(audioAttributes, pauseOnOtherAudio)
        } else if (audioPreferences.isPauseOnDetachKey(key)) {
            val pauseOnDetach = audioPreferences.pauseOnDetach()
            player.setHandleAudioBecomingNoisy(pauseOnDetach)
        } else if (::audioPreferences.isInitialized && audioPreferences.isAudioPlaybackKey(key)) {
            val transitionSettingChanged = audioPreferences.isTransitionSettingKey(key)
            audioConfig = audioPreferences.read()
            if (!audioConfig.fadeUserPauseResume) {
                pauseFadeJob?.cancel()
                volumeCoordinator.pauseEnvelope = 1f
                if (!userWantsPlayback && player.playWhenReady) player.pause()
            }
            if (transitionSettingChanged) {
                transitionFadeJob?.cancel()
                volumeCoordinator.transitionEnvelope = 1f
            }
            applyReplayGain(player.currentMediaItem?.mediaId)
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioPreferences = com.example.xargoosh.domain.playback.AudioPlaybackPreferences(this)
        audioConfig = audioPreferences.read()
        val prefs = audioPreferences.sharedPreferences
        val pauseOnOtherAudio = audioPreferences.pauseOnOtherAudio()
        val pauseOnDetach = audioPreferences.pauseOnDetach()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, pauseOnOtherAudio)
            .setHandleAudioBecomingNoisy(pauseOnDetach)
            .build()
        volumeCoordinator = com.example.xargoosh.domain.playback.PlaybackVolumeCoordinator { volume ->
            if (!destroyed) player.volume = volume
        }
        volumeCoordinator.apply()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        repository = com.example.xargoosh.data.DataRepositoryImpl(
            this,
            com.example.xargoosh.data.local.MediaScanner(this),
            com.example.xargoosh.data.local.db.AppDatabase.getDatabase(this)
        )
        prefs.edit().putBoolean("widget_is_playing", player.isPlaying).apply()
        player.repeatMode = prefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)

        val restored = kotlinx.coroutines.runBlocking {
            com.example.xargoosh.domain.queue.QueueManager.restoreState(this@XargooshMediaService)
        }
        if (restored.queue.isNotEmpty()) {
            val playbackQueue = if (restored.shuffleEnabled) restored.normalQueue else restored.queue
            val items = playbackQueue.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(item.track.uri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(item.track.title)
                            .setArtist(item.track.artist)
                            .setAlbumTitle(item.track.album)
                            .setArtworkUri(item.track.albumArtUri?.let(android.net.Uri::parse))
                            .build()
                    )
                    .build()
            }
            val startIndex = playbackQueue.indexOfFirst { it.id == restored.currentItemId }
                .takeIf { it >= 0 }
                ?: 0
            player.setMediaItems(items, startIndex, restored.position)
            if (restored.shuffleEnabled) {
                val indexById = playbackQueue.mapIndexed { index, item -> item.id to index }.toMap()
                val permutation = restored.queue.mapNotNull { indexById[it.id] }.toIntArray()
                if (permutation.size == playbackQueue.size) {
                    player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(permutation, 0L))
                }
            }
            player.shuffleModeEnabled = restored.shuffleEnabled
            player.prepare()
            applyReplayGain(player.currentMediaItem?.mediaId)
        }
        serviceScope.launch {
            repository.getAllTracksAsc().collectLatest { tracks ->
                com.example.xargoosh.domain.queue.QueueManager.updateTracks(tracks.associateBy { it.uri })
                applyReplayGain(player.currentMediaItem?.mediaId)
            }
        }

        val initialMediaButtons = listOf(createShuffleButton(), createRepeatButton())
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.example.xargoosh.MainActivity::class.java).apply {
                action = com.example.xargoosh.MainActivity.ACTION_OPEN_NOW_PLAYING
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val defaultNotificationProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this).build()
        val notificationProvider = object : androidx.media3.session.MediaNotification.Provider {
            private fun androidx.media3.session.MediaNotification.withSessionIntent(): androidx.media3.session.MediaNotification {
                notification.contentIntent = sessionActivity
                return this
            }

            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                callback: androidx.media3.session.MediaNotification.Provider.Callback
            ): androidx.media3.session.MediaNotification {
                val forwardingCallback = androidx.media3.session.MediaNotification.Provider.Callback { updated ->
                    callback.onNotificationChanged(updated.withSessionIntent())
                }
                return defaultNotificationProvider.createNotification(
                    mediaSession,
                    customLayout,
                    actionFactory,
                    forwardingCallback
                ).withSessionIntent()
            }

            override fun handleCustomCommand(mediaSession: MediaSession, action: String, extras: Bundle): Boolean {
                return defaultNotificationProvider.handleCustomCommand(mediaSession, action, extras)
            }
        }
        setMediaNotificationProvider(notificationProvider)

        val controllerPlayer = object : ForwardingPlayer(player) {
            override fun play() = requestUserPlay()
            override fun pause() = requestUserPause()
            override fun setPlayWhenReady(playWhenReady: Boolean) {
                if (playWhenReady == player.playWhenReady) return
                if (playWhenReady) requestUserPlay() else requestUserPause()
            }
            override fun setVolume(volume: Float) = Unit
        }
        mediaSession = MediaSession.Builder(this, controllerPlayer)
            .setSessionActivity(sessionActivity)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    if (controller.packageName != packageName && !controller.isTrusted) {
                        return MediaSession.ConnectionResult.reject()
                    }
                    val connectionResult = super.onConnect(session, controller)
                    val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    if (controller.packageName == packageName) {
                        sessionCommands.add(COMMAND_RETRY_VISUALIZER)
                            .add(COMMAND_SET_SLEEP_TIMER)
                            .add(COMMAND_SET_SHUFFLE_ORDER)
                    }

                    val shuffleButton = createShuffleButton()
                    val repeatButton = createRepeatButton()

                    session.setCustomLayout(controller, listOf(shuffleButton, repeatButton))

                    val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                        .remove(Player.COMMAND_SET_VOLUME)
                        .build()
                    return MediaSession.ConnectionResult.accept(
                        sessionCommands.build(),
                        playerCommands
                    )
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    return when (customCommand.customAction) {
                        ACTION_RETRY_VISUALIZER -> {
                            if (visualizerEnabled && player.isPlaying) setupVisualizer(player.audioSessionId)
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        ACTION_SET_SLEEP_TIMER -> {
                            scheduleSleepTimer(args.getLong(EXTRA_SLEEP_TIMER_END, 0L))
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        ACTION_SET_SHUFFLE_ORDER -> {
                            val ids = args.getStringArrayList(EXTRA_QUEUE_IDS).orEmpty()
                            applyShuffleOrder(ids)
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        else -> super.onCustomCommand(session, controller, customCommand, args)
                    }
                }
            })
            .setCustomLayout(initialMediaButtons)
            .setMediaButtonPreferences(initialMediaButtons)
            .build()

        serviceScope.launch {
            com.example.xargoosh.domain.visualizer.VisualizerSettings.getInstance(this@XargooshMediaService)
                .enabled.collectLatest { enabled ->
                    visualizerEnabled = enabled
                    if (enabled && player.isPlaying) setupVisualizer(player.audioSessionId) else releaseVisualizer()
                }
        }
        scheduleSleepTimer(
            prefs.getLong("sleep_timer_end", 0L).takeIf { it > System.currentTimeMillis() } ?: 0L
        )

        startActiveListeningSession(player.currentMediaItem)
        activeListeningJob = serviceScope.launch {
            while (isActive) {
                updateActiveListening()
                delay(200L)
            }
        }

        playerListener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                com.example.xargoosh.domain.visualizer.VisualizerRepository.updateAudioSessionId(audioSessionId)
                if (visualizerEnabled && player.isPlaying) {
                    setupVisualizer(audioSessionId)
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateActiveListening(isPlaying)
                prefs.edit().putBoolean("widget_is_playing", isPlaying).apply()
                serviceScope.launch {
                    runCatching { com.example.xargoosh.widget.updateAllXargooshWidgets(this@XargooshMediaService) }
                }
                if (visualizerEnabled && isPlaying) {
                    setupVisualizer(player.audioSessionId)
                } else {
                    releaseVisualizer()
                    persistPlaybackState()
                }
                if (isPlaying && userWantsPlayback && volumeCoordinator.pauseEnvelope < 1f && pauseFadeJob?.isActive != true) {
                    animatePauseEnvelope(1f, com.example.xargoosh.domain.playback.AudioPlaybackConfig.USER_RESUME_FADE_MS)
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val sameIdPlaylistChange =
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                    mediaItem?.mediaId == activeListeningItemId
                applyReplayGain(mediaItem?.mediaId)
                if (sameIdPlaylistChange) {
                    updateActiveListening()
                } else {
                    startActiveListeningSession(mediaItem)
                    startIncomingTransitionFade()
                }
                persistPlaybackState()
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateCustomLayout()
                serviceScope.launch {
                    synchronizeQueueOrder(shuffleModeEnabled)
                    persistPlaybackState()
                }
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (player.shuffleModeEnabled) {
                    serviceScope.launch { synchronizeQueueOrder(true) }
                }
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                prefs.edit().putInt("repeat_mode", repeatMode).apply()
                updateCustomLayout()
            }
        }
        player.addListener(requireNotNull(playerListener))
        transitionMonitorJob = serviceScope.launch {
            while (isActive) {
                updateOutgoingTransitionEnvelope()
                delay(25L)
            }
        }
    }

    private fun requestUserPause() {
        userWantsPlayback = false
        pauseFadeJob?.cancel()
        if (!audioConfig.fadeUserPauseResume || !player.playWhenReady) {
            volumeCoordinator.pauseEnvelope = 1f
            player.pause()
            return
        }
        animatePauseEnvelope(0f, com.example.xargoosh.domain.playback.AudioPlaybackConfig.USER_PAUSE_FADE_MS) {
            if (!userWantsPlayback) player.pause()
        }
    }

    private fun requestUserPlay() {
        userWantsPlayback = true
        pauseFadeJob?.cancel()
        if (!audioConfig.fadeUserPauseResume) {
            volumeCoordinator.pauseEnvelope = 1f
            player.play()
            return
        }
        if (!player.playWhenReady) {
            volumeCoordinator.pauseEnvelope = 0f
            player.play()
        }
        if (player.isPlaying) {
            animatePauseEnvelope(1f, com.example.xargoosh.domain.playback.AudioPlaybackConfig.USER_RESUME_FADE_MS)
        }
    }

    private fun animatePauseEnvelope(target: Float, durationMs: Long, onComplete: () -> Unit = {}) {
        pauseFadeJob?.cancel()
        pauseFadeJob = serviceScope.launch {
            val start = volumeCoordinator.pauseEnvelope
            val startedAt = SystemClock.elapsedRealtime()
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                volumeCoordinator.pauseEnvelope = com.example.xargoosh.domain.playback.PlaybackEnvelopes.interpolate(
                    start, target, elapsed, durationMs
                )
                if (elapsed >= durationMs) break
                delay(16L)
            }
            volumeCoordinator.pauseEnvelope = target
            onComplete()
        }
    }

    private fun startIncomingTransitionFade() {
        transitionFadeJob?.cancel()
        if (audioConfig.transitionMode != com.example.xargoosh.domain.playback.TransitionMode.FADE_THROUGH) {
            volumeCoordinator.transitionEnvelope = 1f
            return
        }
        transitionFadeJob = serviceScope.launch {
            volumeCoordinator.transitionEnvelope = 0f
            val duration = audioConfig.fadeThroughDurationMs.toLong()
            val startedAt = SystemClock.elapsedRealtime()
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                volumeCoordinator.transitionEnvelope = com.example.xargoosh.domain.playback.PlaybackEnvelopes.interpolate(
                    0f, 1f, elapsed, duration
                )
                if (elapsed >= duration) break
                delay(16L)
            }
            volumeCoordinator.transitionEnvelope = 1f
        }
    }

    private fun updateOutgoingTransitionEnvelope() {
        if (audioConfig.transitionMode != com.example.xargoosh.domain.playback.TransitionMode.FADE_THROUGH) return
        if (transitionFadeJob?.isActive == true) return
        val duration = player.duration
        if (duration == androidx.media3.common.C.TIME_UNSET || duration <= 0L) {
            volumeCoordinator.transitionEnvelope = 1f
            return
        }
        volumeCoordinator.transitionEnvelope = com.example.xargoosh.domain.playback.PlaybackEnvelopes.outgoing(
            remainingMs = (duration - player.currentPosition).coerceAtLeast(0L),
            fadeDurationMs = audioConfig.fadeThroughDurationMs.toLong()
        )
    }

    private fun applyReplayGain(mediaId: String?) {
        val track = com.example.xargoosh.domain.queue.QueueManager.currentQueue.value
            .firstOrNull { it.id == mediaId }
            ?.track
        volumeCoordinator.replayGainFactor = com.example.xargoosh.domain.playback.ReplayGainNormalizer.factor(
            audioConfig.replayGainMode,
            track
        )
    }

    private fun startActiveListeningSession(mediaItem: MediaItem?) {
        activeListeningItemId = mediaItem?.mediaId
        activeListeningTracker.startSession(
            itemId = activeListeningItemId,
            trackUri = mediaItem?.localConfiguration?.uri?.toString(),
            durationMs = player.duration,
            isPlaying = player.isPlaying,
            elapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
    }

    private fun updateActiveListening(isPlaying: Boolean = player.isPlaying) {
        val mediaItem = player.currentMediaItem
        val itemId = mediaItem?.mediaId
        val trackUri = activeListeningTracker.update(
            itemId = itemId,
            trackUri = mediaItem?.localConfiguration?.uri?.toString(),
            durationMs = player.duration,
            isPlaying = isPlaying,
            elapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
        activeListeningItemId = itemId
        if (trackUri != null) {
            val job = serviceScope.launch(
                kotlinx.coroutines.Dispatchers.IO,
                start = kotlinx.coroutines.CoroutineStart.LAZY
            ) { repository.incrementPlayCount(trackUri) }
            pendingPlayCountJobs += job
            job.invokeOnCompletion { pendingPlayCountJobs -= job }
            job.start()
        }
    }

    private suspend fun synchronizeQueueOrder(shuffleEnabled: Boolean) {
        if (shuffleEnabled) {
            val timeline = player.currentTimeline
            val orderedIds = mutableListOf<String>()
            val seen = mutableSetOf<Int>()
            var index = timeline.getFirstWindowIndex(true)
            while (index != androidx.media3.common.C.INDEX_UNSET && seen.add(index)) {
                if (index in 0 until player.mediaItemCount) orderedIds += player.getMediaItemAt(index).mediaId
                index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
            }
            com.example.xargoosh.domain.queue.QueueManager.applyPlaybackOrder(orderedIds)
        } else {
            com.example.xargoosh.domain.queue.QueueManager.restoreNormalOrder()
        }
    }

    private fun applyShuffleOrder(orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        val indexById = (0 until player.mediaItemCount).associateBy { player.getMediaItemAt(it).mediaId }
        val requested = orderedIds.mapNotNull(indexById::get)
        val included = requested.toSet()
        val permutation = (requested + (0 until player.mediaItemCount).filterNot(included::contains)).toIntArray()
        if (permutation.size != player.mediaItemCount) return
        player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(permutation, 0L))
        serviceScope.launch {
            com.example.xargoosh.domain.queue.QueueManager.applyPlaybackOrder(orderedIds)
            persistPlaybackState()
        }
    }

    private fun createShuffleButton(): androidx.media3.session.CommandButton {
        return androidx.media3.session.CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
            .setIconResId(com.example.xargoosh.R.drawable.ic_notification_shuffle)
            .setDisplayName(getString(com.example.xargoosh.R.string.shuffle))
            .build()
    }

    private fun createRepeatButton(): androidx.media3.session.CommandButton {
        return androidx.media3.session.CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .setIconResId(com.example.xargoosh.R.drawable.ic_notification_repeat)
            .setDisplayName(getString(com.example.xargoosh.R.string.repeat))
            .build()
    }

    private fun updateCustomLayout() {
        val session = mediaSession ?: return
        val layout = listOf(createShuffleButton(), createRepeatButton())
        session.setCustomLayout(layout)
        session.setMediaButtonPreferences(layout)
        session.connectedControllers.forEach { controller ->
            session.setCustomLayout(controller, layout)
            session.setMediaButtonPreferences(controller, layout)
        }
    }

    private fun setupVisualizer(audioSessionId: Int) {
        if (!visualizerEnabled || audioSessionId == 0) return
        com.example.xargoosh.domain.visualizer.VisualizerRepository.updateAudioSessionId(audioSessionId)
        try {
            releaseVisualizer()
            visualizer = android.media.audiofx.Visualizer(audioSessionId).apply {
                captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1] 
                setDataCaptureListener(
                    object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: android.media.audiofx.Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { com.example.xargoosh.domain.visualizer.VisualizerRepository.updateWaveform(it.clone()) }
                        }
                        override fun onFftDataCapture(
                            v: android.media.audiofx.Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { com.example.xargoosh.domain.visualizer.VisualizerRepository.updateFft(it.clone()) }
                        }
                    },
                    android.media.audiofx.Visualizer.getMaxCaptureRate(),
                    true,
                    true
                )
                enabled = true
            }
        } catch (_: Exception) {
            com.example.xargoosh.domain.visualizer.VisualizerRepository.markFftUnavailable()
        }
    }

    private fun releaseVisualizer() {
        val effect = visualizer
        visualizer = null
        if (effect != null) {
            runCatching { effect.setDataCaptureListener(null, 0, false, false) }
            runCatching { effect.enabled = false }
            runCatching { effect.release() }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun scheduleSleepTimer(endTime: Long) {
        sleepTimerJob?.cancel()
        audioPreferences.sharedPreferences.edit().putLong("sleep_timer_end", endTime).apply()
        if (endTime <= System.currentTimeMillis()) return
        sleepTimerJob = serviceScope.launch {
            kotlinx.coroutines.delay(endTime - System.currentTimeMillis())
            player.pause()
            audioPreferences.sharedPreferences.edit().remove("sleep_timer_end").apply()
        }
    }

    private fun persistPlaybackState(position: Long = player.currentPosition) {
        val itemId = player.currentMediaItem?.mediaId
        val track = com.example.xargoosh.domain.queue.QueueManager.currentQueue.value
            .firstOrNull { it.id == itemId }
            ?.track
        playbackPersistenceJob?.cancel()
        playbackPersistenceJob = serviceScope.launch {
            com.example.xargoosh.domain.queue.QueueManager.saveState(
                this@XargooshMediaService,
                track,
                position.coerceAtLeast(0L),
                itemId,
                player.shuffleModeEnabled
            )
            runCatching { com.example.xargoosh.widget.updateAllXargooshWidgets(this@XargooshMediaService) }
        }
    }

    override fun onDestroy() {
        destroyed = true
        pauseFadeJob?.cancel()
        transitionFadeJob?.cancel()
        transitionMonitorJob?.cancel()
        activeListeningJob?.cancel()
        activeListeningJob = null
        updateActiveListening()
        playbackPersistenceJob?.cancel()
        val position = player.currentPosition.coerceAtLeast(0L)
        val itemId = player.currentMediaItem?.mediaId
        val shuffleEnabled = player.shuffleModeEnabled
        val track = com.example.xargoosh.domain.queue.QueueManager.currentQueue.value
            .firstOrNull { it.id == itemId }
            ?.track
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.xargoosh.domain.queue.QueueManager.saveState(
                        this@XargooshMediaService,
                        track,
                        position,
                        itemId,
                        shuffleEnabled
                    )
                }
                val countJobs = synchronized(pendingPlayCountJobs) { pendingPlayCountJobs.toList() }
                countJobs.forEach { it.join() }
            }
        }
        playerListener?.let(player::removeListener)
        playerListener = null
        releaseVisualizer()
        com.example.xargoosh.presentation.equalizer.EqualizerController.release()
        mediaSession?.release()
        mediaSession = null
        player.release()
        val prefs = audioPreferences.sharedPreferences
        prefs.edit().putBoolean("widget_is_playing", false).apply()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        serviceScope.cancel()
        super.onDestroy()
    }
}
