package com.robolig.controller.utils

import android.content.SharedPreferences
import com.robolig.controller.core.LogLevel
import com.robolig.controller.core.PreferenceConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ControllerPreferences {
    val videoStreamUrl: StateFlow<String>
    val logLevel: StateFlow<LogLevel>
    val showPacketsOverlay: StateFlow<Boolean>

    fun updateVideoStreamUrl(url: String)

    fun updateLogLevel(level: LogLevel)

    fun updateShowPacketsOverlay(enabled: Boolean)
}

@Singleton
class ControllerPreferencesImpl
    @Inject
    constructor(
        private val sharedPreferences: SharedPreferences,
    ) : ControllerPreferences {
        private val videoStreamUrlState =
            MutableStateFlow(
                sharedPreferences.getString(PreferenceConstants.VIDEO_STREAM_URL, "").orEmpty(),
            )
        private val logLevelState =
            MutableStateFlow(
                LogLevel.fromPersistedValue(
                    sharedPreferences.getString(PreferenceConstants.LOG_LEVEL, LogLevel.DEBUG.name),
                ),
            )
        private val showPacketsOverlayState =
            MutableStateFlow(
                sharedPreferences.getBoolean(PreferenceConstants.SHOW_PACKETS_OVERLAY, false),
            )

        override val videoStreamUrl: StateFlow<String> = videoStreamUrlState.asStateFlow()
        override val logLevel: StateFlow<LogLevel> = logLevelState.asStateFlow()
        override val showPacketsOverlay: StateFlow<Boolean> = showPacketsOverlayState.asStateFlow()

        override fun updateVideoStreamUrl(url: String) {
            val normalizedUrl = url.trim()
            if (videoStreamUrlState.value == normalizedUrl) {
                return
            }

            sharedPreferences
                .edit()
                .putString(PreferenceConstants.VIDEO_STREAM_URL, normalizedUrl)
                .apply()

            videoStreamUrlState.value = normalizedUrl
        }

        override fun updateLogLevel(level: LogLevel) {
            if (logLevelState.value == level) {
                return
            }

            sharedPreferences
                .edit()
                .putString(PreferenceConstants.LOG_LEVEL, level.name)
                .apply()

            logLevelState.value = level
        }

        override fun updateShowPacketsOverlay(enabled: Boolean) {
            if (showPacketsOverlayState.value == enabled) {
                return
            }

            sharedPreferences
                .edit()
                .putBoolean(PreferenceConstants.SHOW_PACKETS_OVERLAY, enabled)
                .apply()

            showPacketsOverlayState.value = enabled
        }
    }
