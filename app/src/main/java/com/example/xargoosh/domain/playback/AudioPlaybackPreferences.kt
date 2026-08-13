package com.example.xargoosh.domain.playback

import android.content.Context
import android.content.SharedPreferences

class AudioPlaybackPreferences(context: Context) {
    val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): AudioPlaybackConfig = AudioPlaybackConfig.sanitized(
        transitionMode = sharedPreferences.getString(KEY_TRANSITION_MODE, null),
        fadeThroughDurationMs = sharedPreferences.getInt(
            KEY_FADE_THROUGH_DURATION,
            AudioPlaybackConfig.DEFAULT_FADE_THROUGH_MS
        ),
        fadeUserPauseResume = sharedPreferences.getBoolean(KEY_USER_PAUSE_FADE, true),
        replayGainMode = sharedPreferences.getString(KEY_REPLAY_GAIN_MODE, null)
    )

    fun setTransitionMode(value: TransitionMode) =
        sharedPreferences.edit().putString(KEY_TRANSITION_MODE, value.name).apply()

    fun setFadeThroughDurationMs(value: Int) = sharedPreferences.edit().putInt(
        KEY_FADE_THROUGH_DURATION,
        value.takeIf(AudioPlaybackConfig.SUPPORTED_FADE_DURATIONS_MS::contains)
            ?: AudioPlaybackConfig.DEFAULT_FADE_THROUGH_MS
    ).apply()

    fun setFadeUserPauseResume(value: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_USER_PAUSE_FADE, value).apply()

    fun setReplayGainMode(value: ReplayGainMode) =
        sharedPreferences.edit().putString(KEY_REPLAY_GAIN_MODE, value.name).apply()

    fun pauseOnOtherAudio() = sharedPreferences.getBoolean(KEY_PAUSE_ON_OTHER_AUDIO, true)

    fun setPauseOnOtherAudio(value: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_PAUSE_ON_OTHER_AUDIO, value).apply()

    fun pauseOnDetach() = sharedPreferences.getBoolean(KEY_PAUSE_ON_DETACH, true)

    fun setPauseOnDetach(value: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_PAUSE_ON_DETACH, value).apply()

    fun isAudioPlaybackKey(key: String?) = key in AUDIO_KEYS

    fun isTransitionSettingKey(key: String?) = key == KEY_TRANSITION_MODE || key == KEY_FADE_THROUGH_DURATION

    fun isPauseOnOtherAudioKey(key: String?) = key == KEY_PAUSE_ON_OTHER_AUDIO

    fun isPauseOnDetachKey(key: String?) = key == KEY_PAUSE_ON_DETACH

    fun isObservableAudioKey(key: String?) = isAudioPlaybackKey(key) ||
        isPauseOnOtherAudioKey(key) || isPauseOnDetachKey(key)

    companion object {
        const val PREFERENCES_NAME = "xargoosh_prefs"
        private const val KEY_TRANSITION_MODE = "audio_transition_mode"
        private const val KEY_FADE_THROUGH_DURATION = "audio_fade_through_duration_ms"
        private const val KEY_USER_PAUSE_FADE = "audio_user_pause_fade"
        private const val KEY_REPLAY_GAIN_MODE = "audio_replay_gain_mode"
        private const val KEY_PAUSE_ON_OTHER_AUDIO = "pause_on_other_audio"
        private const val KEY_PAUSE_ON_DETACH = "pause_on_detach"
        private val AUDIO_KEYS = setOf(
            KEY_TRANSITION_MODE,
            KEY_FADE_THROUGH_DURATION,
            KEY_USER_PAUSE_FADE,
            KEY_REPLAY_GAIN_MODE
        )
    }
}
