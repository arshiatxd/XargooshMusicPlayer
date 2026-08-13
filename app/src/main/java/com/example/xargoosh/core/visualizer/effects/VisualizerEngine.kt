package com.example.xargoosh.core.visualizer.effects

import com.example.xargoosh.domain.visualizer.*
import com.example.xargoosh.core.design.themes.ThemeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VisualizerEngine(
    private val repository: VisualizerRepository,
    private val settings: VisualizerSettings,
    private val qualityMonitor: AdaptiveQualityMonitor,
    private val scope: CoroutineScope
) {
    private val _visualizerState = MutableStateFlow(VisualizerState.IDLE)
    val visualizerState: StateFlow<VisualizerState> = _visualizerState.asStateFlow()

    private var processingJob: Job? = null
    private val fftAnalyzer = FFTAnalyzer(binCount = QualityLevel.HIGH.fftResolution)
    private val beatDetector = BeatDetector()
    private var animationTimeNanos = 0L
    private var lastFrameNanos = 0L

    private val normalizedWaveform = FloatArray(1024)

    @Volatile private var isPlaying = false
    @Volatile private var reduceMotion = false
    @Volatile private var enabled = true

    @Volatile private var currentAlbumPalette: com.example.xargoosh.domain.visualizer.VisualizerPalette? = null

    fun updateAlbumPalette(palette: com.example.xargoosh.domain.visualizer.VisualizerPalette?) {
        currentAlbumPalette = palette
    }
    
    @Volatile private var animationSpeed = 1.0f
    @Volatile private var intensity = 0.7f
    @Volatile private var glowIntensity = 0.6f
    @Volatile private var particleDensity = 0.5f
    @Volatile private var waveThickness = 0.5f
    @Volatile private var beatSensitivity = 1.2f

    init {

        scope.launch {
            settings.reduceMotion.collect { reduceMotion = it }
        }
        scope.launch {
            settings.enabled.collect { enabled = it }
        }
        scope.launch { settings.animationSpeed.collect { animationSpeed = it } }
        scope.launch { settings.intensity.collect { intensity = it } }
        scope.launch { settings.glowIntensity.collect { glowIntensity = it } }
        scope.launch { settings.particleDensity.collect { particleDensity = it } }
        scope.launch { settings.waveThickness.collect { waveThickness = it } }
        scope.launch { settings.beatSensitivity.collect { beatSensitivity = it } }
        startProcessing()
    }

    private fun startProcessing() {
        processingJob?.cancel()
        processingJob = scope.launch(Dispatchers.Default) {
            combine(
                repository.waveformFlow,
                repository.fftFlow,
                ThemeManager.currentTheme,
                settings.overrideAutoColor,
                settings.manualColorArgb
            ) { args ->
                val waveform = args[0] as ByteArray
                val fft = args[1] as ByteArray?
                val theme = args[2] as com.example.xargoosh.core.design.themes.AppTheme
                val overrideColor = args[3] as Boolean
                val manualColor = args[4] as Int

                if (!enabled || !isPlaying || waveform.isEmpty()) {
                    lastFrameNanos = 0L
                    beatDetector.reset()
                    val baseConfig = ThemeAdapter.getConfig(theme)
                    val p = if (overrideColor) VisualizerPalette(
                        primary = androidx.compose.ui.graphics.Color(manualColor),
                        secondary = androidx.compose.ui.graphics.Color(manualColor),
                        glow = androidx.compose.ui.graphics.Color(manualColor),
                        background = androidx.compose.ui.graphics.Color.Black,
                        accent = androidx.compose.ui.graphics.Color.White
                    ) else currentAlbumPalette ?: baseConfig.palette
                    return@combine VisualizerState(
                        animationTimeNanos = animationTimeNanos,
                        palette = p
                    )
                }

                val waveLen = minOf(waveform.size, normalizedWaveform.size)
                for (i in 0 until waveLen) {
                    normalizedWaveform[i] = ((waveform[i].toInt() and 0xFF) - 128) / 128f
                }

                for (i in waveLen until normalizedWaveform.size) normalizedWaveform[i] = 0f

                var bassEnergy = 0f
                var midEnergy = 0f
                var trebleEnergy = 0f
                val smoothedBands: FloatArray
                val fftAvail = repository.fftAvailable.value

                if (fftAvail && fft != null && fft.isNotEmpty()) {
                    fftAnalyzer.processFFT(fft)
                    bassEnergy = fftAnalyzer.bassEnergy
                    midEnergy = fftAnalyzer.midEnergy
                    trebleEnergy = fftAnalyzer.trebleEnergy
                    smoothedBands = fftAnalyzer.smoothedBands.copyOf()
                } else {

                    var rmsSum = 0f
                    for (s in normalizedWaveform) rmsSum += s * s
                    val rms = kotlin.math.sqrt(rmsSum / normalizedWaveform.size)
                    bassEnergy = rms * 0.8f
                    midEnergy = rms * 0.5f
                    trebleEnergy = rms * 0.3f
                    smoothedBands = FloatArray(QualityLevel.HIGH.fftResolution) { bassEnergy }
                }

                val timestampNanos = System.nanoTime()
                val beatState = beatDetector.analyze(
                    normalizedWaveform.copyOf(waveLen),
                    timestampNanos,
                    beatSensitivity,
                    bassEnergy
                )
                val tempoSpeed = (beatState.bpm / 120f).coerceIn(0.55f, 1.9f)
                val frameDelta = if (lastFrameNanos == 0L) 0L else (timestampNanos - lastFrameNanos).coerceAtLeast(0L)
                lastFrameNanos = timestampNanos
                animationTimeNanos += (frameDelta * tempoSpeed * animationSpeed.coerceIn(0f, 2f)).toLong()
                val config = ThemeAdapter.getConfig(theme)
                val finalPalette = if (overrideColor) VisualizerPalette(
                    primary = androidx.compose.ui.graphics.Color(manualColor),
                    secondary = androidx.compose.ui.graphics.Color(manualColor),
                    glow = androidx.compose.ui.graphics.Color(manualColor),
                    background = androidx.compose.ui.graphics.Color.Black,
                    accent = androidx.compose.ui.graphics.Color.White
                ) else currentAlbumPalette ?: config.palette

                VisualizerState(
                    isPlaying = true,
                    fftAvailable = fftAvail,
                    smoothedBands = smoothedBands,
                    waveform = normalizedWaveform.copyOf(),
                    bassEnergy = bassEnergy,
                    midEnergy = midEnergy,
                    trebleEnergy = trebleEnergy,
                    rmsLoudness = beatState.rmsLoudness,
                    isOnBeat = beatState.isOnBeat,
                    bpm = beatState.bpm,
                    reduceMotion = reduceMotion,
                    quality = qualityMonitor.currentQuality,
                    animationSpeed = animationSpeed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    particleDensity = particleDensity,
                    waveThickness = waveThickness,
                    beatSensitivity = beatSensitivity,
                    timestampNanos = timestampNanos,
                    animationTimeNanos = animationTimeNanos,
                    palette = finalPalette
                )
            }.collect { state ->
                _visualizerState.value = state

                delay(qualityMonitor.currentQuality.targetFrameMs)
            }
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (!isPlaying) {
            _visualizerState.value = VisualizerState(
                animationTimeNanos = animationTimeNanos,
                palette = _visualizerState.value.palette
            )
            fftAnalyzer.reset()
            beatDetector.reset()
            lastFrameNanos = 0L
        }
    }

    fun resetBeatTracking() {
        beatDetector.reset()
        lastFrameNanos = 0L
    }

    fun stop() {
        processingJob?.cancel()
    }

    private fun buildIdleState(theme: com.example.xargoosh.core.design.themes.AppTheme): VisualizerState {
        val config = ThemeAdapter.getConfig(theme)
        return VisualizerState(palette = config.palette)
    }
}


