package com.example.xargoosh.domain.visualizer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VisualizerRepository {

    private val _waveformFlow = MutableStateFlow<ByteArray>(ByteArray(0))
    val waveformFlow: StateFlow<ByteArray> = _waveformFlow.asStateFlow()

    private val _fftFlow = MutableStateFlow<ByteArray?>(null)
    val fftFlow: StateFlow<ByteArray?> = _fftFlow.asStateFlow()

    private val _audioSessionIdFlow = MutableStateFlow(0)
    val audioSessionIdFlow: StateFlow<Int> = _audioSessionIdFlow.asStateFlow()

    private val _fftAvailable = MutableStateFlow(false)
    val fftAvailable: StateFlow<Boolean> = _fftAvailable.asStateFlow()

    fun updateWaveform(waveform: ByteArray) {
        _waveformFlow.value = waveform
    }

    fun updateFft(fft: ByteArray) {
        _fftAvailable.value = true
        _fftFlow.value = fft
    }

    fun updateAudioSessionId(id: Int) {
        _audioSessionIdFlow.value = id
    }

    fun markFftUnavailable() {
        _fftAvailable.value = false
    }
}
