package com.example.xargoosh.domain.visualizer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "visualizer_settings")

class VisualizerSettings private constructor(private val context: Context) {

    private val dataStore = context.dataStore
    private val safeData = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    val enabled: Flow<Boolean> = safeData.map { it[ENABLED_KEY] ?: false }
    val style: Flow<VisualizerStyle> = safeData.map { prefs ->
        val name = prefs[STYLE_KEY] ?: "NIER_WAVE"
        runCatching { VisualizerStyle.valueOf(name) }.getOrDefault(VisualizerStyle.valueOf("NIER_WAVE")) 
    }
    val intensity: Flow<Float> = safeData.map { it[INTENSITY_KEY] ?: 0.7f }
    val blurStrength: Flow<Float> = safeData.map { it[BLUR_STRENGTH_KEY] ?: 0.5f }
    val renderSize: Flow<Float> = safeData.map { it[RENDER_SIZE_KEY] ?: 1f }
    val glowIntensity: Flow<Float> = safeData.map { it[GLOW_INTENSITY_KEY] ?: 0.6f }
    val particleDensity: Flow<Float> = safeData.map { it[PARTICLE_DENSITY_KEY] ?: 0.5f }
    val waveThickness: Flow<Float> = safeData.map { it[WAVE_THICKNESS_KEY] ?: 0.5f }
    val animationSpeed: Flow<Float> = safeData.map { it[ANIMATION_SPEED_KEY] ?: 1.0f }
    val beatSensitivity: Flow<Float> = safeData.map { it[BEAT_SENSITIVITY_KEY] ?: 1.2f }
    val dimAmount: Flow<Float> = safeData.map { it[DIM_AMOUNT_KEY] ?: 0.35f }
    val reduceMotion: Flow<Boolean> = safeData.map { it[REDUCE_MOTION_KEY] ?: false }
    val batterySaver: Flow<Boolean> = safeData.map { it[BATTERY_SAVER_KEY] ?: false }
    val useTabbedPlayerLayout: Flow<Boolean> = safeData.map { it[TABBED_PLAYER_LAYOUT_KEY] ?: false }
    val lyricsBlurEnabled: Flow<Boolean> = safeData.map { it[LYRICS_BLUR_ENABLED_KEY] ?: true }
    val placementBehindLyrics: Flow<Boolean> = safeData.map { it[PLACEMENT_BEHIND_LYRICS_KEY] ?: true }
    val fullscreenEnabled: Flow<Boolean> = safeData.map { it[FULLSCREEN_ENABLED_KEY] ?: true }
    val overrideAutoColor: Flow<Boolean> = safeData.map { it[OVERRIDE_AUTO_COLOR_KEY] ?: false }
    val manualColorArgb: Flow<Int> = safeData.map { it[MANUAL_COLOR_ARGB_KEY] ?: android.graphics.Color.RED }
    suspend fun setEnabled(value: Boolean) = dataStore.edit { it[ENABLED_KEY] = value }
    suspend fun setStyle(value: VisualizerStyle) = dataStore.edit { it[STYLE_KEY] = value.name }
    suspend fun setIntensity(value: Float) = dataStore.edit { it[INTENSITY_KEY] = value }
    suspend fun setBlurStrength(value: Float) = dataStore.edit { it[BLUR_STRENGTH_KEY] = value }
    suspend fun setRenderSize(value: Float) = dataStore.edit { it[RENDER_SIZE_KEY] = value.coerceIn(0.6f, 2f) }
    suspend fun setGlowIntensity(value: Float) = dataStore.edit { it[GLOW_INTENSITY_KEY] = value }
    suspend fun setParticleDensity(value: Float) = dataStore.edit { it[PARTICLE_DENSITY_KEY] = value }
    suspend fun setWaveThickness(value: Float) = dataStore.edit { it[WAVE_THICKNESS_KEY] = value }
    suspend fun setAnimationSpeed(value: Float) = dataStore.edit { it[ANIMATION_SPEED_KEY] = value }
    suspend fun setBeatSensitivity(value: Float) = dataStore.edit { it[BEAT_SENSITIVITY_KEY] = value }
    suspend fun setDimAmount(value: Float) = dataStore.edit { it[DIM_AMOUNT_KEY] = value }
    suspend fun setReduceMotion(value: Boolean) = dataStore.edit { it[REDUCE_MOTION_KEY] = value }
    suspend fun setBatterySaver(value: Boolean) = dataStore.edit { it[BATTERY_SAVER_KEY] = value }
    suspend fun setUseTabbedPlayerLayout(value: Boolean) = dataStore.edit { it[TABBED_PLAYER_LAYOUT_KEY] = value }
    suspend fun setLyricsBlurEnabled(value: Boolean) = dataStore.edit { it[LYRICS_BLUR_ENABLED_KEY] = value }
    suspend fun setPlacementBehindLyrics(value: Boolean) = dataStore.edit { it[PLACEMENT_BEHIND_LYRICS_KEY] = value }
    suspend fun setFullscreenEnabled(value: Boolean) = dataStore.edit { it[FULLSCREEN_ENABLED_KEY] = value }
    suspend fun setOverrideAutoColor(value: Boolean) = dataStore.edit { it[OVERRIDE_AUTO_COLOR_KEY] = value }
    suspend fun setManualColorArgb(value: Int) = dataStore.edit { it[MANUAL_COLOR_ARGB_KEY] = value }
    suspend fun resetEffects() = dataStore.edit {
        it[INTENSITY_KEY] = 0.7f
        it[BLUR_STRENGTH_KEY] = 0.5f
        it[RENDER_SIZE_KEY] = 1f
        it[GLOW_INTENSITY_KEY] = 0.6f
        it[PARTICLE_DENSITY_KEY] = 0.5f
        it[WAVE_THICKNESS_KEY] = 0.5f
        it[BEAT_SENSITIVITY_KEY] = 1.2f
        it[ANIMATION_SPEED_KEY] = 1f
        it[DIM_AMOUNT_KEY] = 0.3f
    }
    companion object {
        private val ENABLED_KEY = booleanPreferencesKey("enabled")
        private val STYLE_KEY = stringPreferencesKey("style")
        private val INTENSITY_KEY = floatPreferencesKey("intensity")
        private val BLUR_STRENGTH_KEY = floatPreferencesKey("blur_strength")
        private val RENDER_SIZE_KEY = floatPreferencesKey("render_size")
        private val GLOW_INTENSITY_KEY = floatPreferencesKey("glow_intensity")
        private val PARTICLE_DENSITY_KEY = floatPreferencesKey("particle_density")
        private val WAVE_THICKNESS_KEY = floatPreferencesKey("wave_thickness")
        private val ANIMATION_SPEED_KEY = floatPreferencesKey("animation_speed")
        private val BEAT_SENSITIVITY_KEY = floatPreferencesKey("beat_sensitivity")
        private val DIM_AMOUNT_KEY = floatPreferencesKey("dim_amount")
        private val REDUCE_MOTION_KEY = booleanPreferencesKey("reduce_motion")
        private val BATTERY_SAVER_KEY = booleanPreferencesKey("battery_saver")
        private val TABBED_PLAYER_LAYOUT_KEY = booleanPreferencesKey("tabbed_player_layout")
        private val LYRICS_BLUR_ENABLED_KEY = booleanPreferencesKey("lyrics_blur_enabled")
        private val PLACEMENT_BEHIND_LYRICS_KEY = booleanPreferencesKey("placement_behind_lyrics")
        private val FULLSCREEN_ENABLED_KEY = booleanPreferencesKey("fullscreen_enabled")
        private val OVERRIDE_AUTO_COLOR_KEY = booleanPreferencesKey("override_auto_color")
        private val MANUAL_COLOR_ARGB_KEY = intPreferencesKey("manual_color_argb")
        @Volatile
        private var INSTANCE: VisualizerSettings? = null

        fun getInstance(context: Context): VisualizerSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VisualizerSettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
