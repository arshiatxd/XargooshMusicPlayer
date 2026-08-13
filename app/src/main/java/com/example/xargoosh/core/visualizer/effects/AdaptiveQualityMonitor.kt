package com.example.xargoosh.core.visualizer.effects

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import com.example.xargoosh.domain.visualizer.QualityLevel

class AdaptiveQualityMonitor(
    private val context: Context
) : ComponentCallbacks2 {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val frameTimes = LongArray(60)
    private var frameIndex = 0
    private var isBufferFull = false

    var currentQuality: QualityLevel = QualityLevel.HIGH
        private set

    var shouldRender: Boolean = true
        private set

    private var stableFrameCount = 0

    init {
        context.registerComponentCallbacks(this)
        updateQualityAndRenderState()
    }

    fun onFrameRendered(frameTimeNanos: Long) {
        val frameTimeMs = frameTimeNanos / 1_000_000
        frameTimes[frameIndex] = frameTimeMs
        frameIndex = (frameIndex + 1) % 60
        if (frameIndex == 0) isBufferFull = true

        evaluatePerformance()
    }

    private fun evaluatePerformance() {
        if (!isBufferFull) return

        val averageFrameTimeMs = frameTimes.average().toLong()

        if (averageFrameTimeMs > 25) {
            stableFrameCount = 0
            downgradeQuality()
        } else if (averageFrameTimeMs < 12) {
            stableFrameCount++
            if (stableFrameCount >= 60) {
                upgradeQuality()
                stableFrameCount = 0
            }
        } else {
            stableFrameCount = 0
        }
    }

    private fun downgradeQuality() {
        currentQuality = when (currentQuality) {
            QualityLevel.ULTRA -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.LOW
            QualityLevel.LOW -> QualityLevel.SAVER
            QualityLevel.SAVER -> QualityLevel.ULTRA_SAVER
            QualityLevel.ULTRA_SAVER -> QualityLevel.ULTRA_SAVER
        }
    }

    private fun upgradeQuality() {
        currentQuality = when (currentQuality) {
            QualityLevel.ULTRA_SAVER -> QualityLevel.SAVER
            QualityLevel.SAVER -> QualityLevel.LOW
            QualityLevel.LOW -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.ULTRA
            QualityLevel.ULTRA -> QualityLevel.ULTRA
        }
    }

    private fun updateQualityAndRenderState() {
        if (powerManager.isPowerSaveMode) {
            currentQuality = QualityLevel.SAVER
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thermalStatus = powerManager.currentThermalStatus
            if (thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                currentQuality = QualityLevel.ULTRA_SAVER
            }
        }

        shouldRender = !powerManager.isPowerSaveMode
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        currentQuality = QualityLevel.ULTRA_SAVER
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            currentQuality = QualityLevel.SAVER
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            currentQuality = QualityLevel.ULTRA_SAVER
            shouldRender = false
        }
    }

    fun release() {
        context.unregisterComponentCallbacks(this)
    }
}
