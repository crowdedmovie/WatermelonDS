package me.magnum.melonds.domain.model

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastForwardAudioSettingsTest {
    @Test
    fun emulatorConfigurationDefaultsToOffAndCanBeCopiedWithTheSettingEnabled() {
        val configuration = EmulatorConfiguration(
            useCustomBios = false,
            showBootScreen = false,
            dsBios7Uri = null,
            dsBios9Uri = null,
            dsFirmwareUri = null,
            dsiBios7Uri = null,
            dsiBios9Uri = null,
            dsiFirmwareUri = null,
            dsiNandUri = null,
            internalDirectory = "",
            fastForwardSpeedMultiplier = 2f,
            frameLimitSpeedMultiplier = 1f,
            frameskipMode = 0,
            frameskipManualValue = 0,
            vulkanDrsEnabled = false,
            rewindEnabled = false,
            rewindPeriodSeconds = 10,
            rewindWindowSeconds = 60,
            useJit = false,
            hgEngineFixEnabled = false,
            consoleType = ConsoleType.DS,
            soundEnabled = true,
            audioInterpolation = AudioInterpolation.NONE,
            audioBitrate = AudioBitrate.AUTO,
            volume = 256,
            audioLatency = AudioLatency.LOW,
            micSource = MicSource.NONE,
            firmwareConfiguration = FirmwareConfiguration("", "", 0, 0, 1, 1, false, null),
            rendererConfiguration = RendererConfiguration(
                renderer = VideoRenderer.SOFTWARE,
                videoFiltering = VideoFiltering.NONE,
                threadedRendering = false,
                internalResolutionScaling = 1,
                rendererDebugToolsEnabled = false,
                rendererDebugBgObjEnabled = false,
                rendererDebugLatchTraceEnabled = false,
                conservativeCoverageEnabled = false,
                conservativeCoveragePx = 0f,
                conservativeCoverageDepthBias = 0f,
                conservativeCoverageApplyRepeat = false,
                conservativeCoverageApplyClamp = false,
                debug3dClearMagenta = false,
                retroArchShader = RetroArchShaderConfiguration(
                    presetPath = null,
                    sourceResolution = RetroArchShaderSourceResolution.NATIVE,
                    passCount = 0,
                    sourceBytes = 0L,
                    parameterOverrides = emptyMap(),
                    clearHistory = false,
                ),
            ),
            dldiSdCardConfiguration = DldiSdCardConfiguration(false, null, 0, false, null),
        )

        assertFalse(configuration.keepAudioNormalSpeedOnFastForward)
        assertTrue(configuration.copy(keepAudioNormalSpeedOnFastForward = true).keepAudioNormalSpeedOnFastForward)
    }

    @Test
    fun readsTheStoredBooleanAndDefaultsToOffWhenMissing() {
        val key = "audio_keep_normal_speed_on_fast_forward"
        val preferences = BooleanPreferences()

        assertEquals(key, PREF_KEY_KEEP_AUDIO_NORMAL_SPEED_ON_FAST_FORWARD)
        assertFalse(isKeepAudioNormalSpeedOnFastForwardEnabled(preferences))

        preferences.values[key] = true
        assertTrue(isKeepAudioNormalSpeedOnFastForwardEnabled(preferences))

        preferences.values[key] = false
        assertFalse(isKeepAudioNormalSpeedOnFastForwardEnabled(preferences))

        preferences.values[key] = true
        preferences.values.remove(key)
        assertFalse(isKeepAudioNormalSpeedOnFastForwardEnabled(preferences))
    }

    @Test
    fun hidesThePreferenceWhenSoundIsOffAndMuteIsOff() {
        assertEquals(
            KeepNormalSpeedPreferenceUiState(isVisible = false, isEnabled = false, overriddenByMute = false),
            resolveKeepNormalSpeedPreferenceUiState(soundEnabled = false, muteOnFastForward = false),
        )
    }

    @Test
    fun hidesThePreferenceWhenSoundIsOffAndMuteIsOn() {
        assertEquals(
            KeepNormalSpeedPreferenceUiState(isVisible = false, isEnabled = false, overriddenByMute = false),
            resolveKeepNormalSpeedPreferenceUiState(soundEnabled = false, muteOnFastForward = true),
        )
    }

    @Test
    fun enablesThePreferenceWhenSoundIsOnAndMuteIsOff() {
        assertEquals(
            KeepNormalSpeedPreferenceUiState(isVisible = true, isEnabled = true, overriddenByMute = false),
            resolveKeepNormalSpeedPreferenceUiState(soundEnabled = true, muteOnFastForward = false),
        )
    }

    @Test
    fun showsTheMuteOverrideWhenSoundIsOnAndMuteIsOn() {
        assertEquals(
            KeepNormalSpeedPreferenceUiState(isVisible = true, isEnabled = false, overriddenByMute = true),
            resolveKeepNormalSpeedPreferenceUiState(soundEnabled = true, muteOnFastForward = true),
        )
    }

    private class BooleanPreferences : SharedPreferences {
        val values = mutableMapOf<String, Boolean>()

        override fun getBoolean(key: String, defValue: Boolean) = values[key] ?: defValue
        override fun getAll() = values.toMap()
        override fun contains(key: String) = key in values
        override fun getString(key: String, defValue: String?): String? = throw UnsupportedOperationException()
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = throw UnsupportedOperationException()
        override fun getInt(key: String, defValue: Int): Int = throw UnsupportedOperationException()
        override fun getLong(key: String, defValue: Long): Long = throw UnsupportedOperationException()
        override fun getFloat(key: String, defValue: Float): Float = throw UnsupportedOperationException()
        override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            throw UnsupportedOperationException()

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
            throw UnsupportedOperationException()
    }
}
