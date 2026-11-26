package me.magnum.melonds.domain.model

import android.content.SharedPreferences

const val PREF_KEY_MUTE_ON_FAST_FORWARD = "audio_mute_on_fast_forward"
const val PREF_KEY_KEEP_AUDIO_NORMAL_SPEED_ON_FAST_FORWARD = "audio_keep_normal_speed_on_fast_forward"
const val DEFAULT_KEEP_AUDIO_NORMAL_SPEED_ON_FAST_FORWARD = false

data class KeepNormalSpeedPreferenceUiState(
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val overriddenByMute: Boolean,
)

fun resolveKeepNormalSpeedPreferenceUiState(
    soundEnabled: Boolean,
    muteOnFastForward: Boolean,
): KeepNormalSpeedPreferenceUiState = KeepNormalSpeedPreferenceUiState(
    isVisible = soundEnabled,
    isEnabled = soundEnabled && !muteOnFastForward,
    overriddenByMute = soundEnabled && muteOnFastForward,
)

fun isKeepAudioNormalSpeedOnFastForwardEnabled(preferences: SharedPreferences): Boolean =
    preferences.getBoolean(
        PREF_KEY_KEEP_AUDIO_NORMAL_SPEED_ON_FAST_FORWARD,
        DEFAULT_KEEP_AUDIO_NORMAL_SPEED_ON_FAST_FORWARD,
    )
