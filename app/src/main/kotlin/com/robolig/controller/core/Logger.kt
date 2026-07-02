package com.robolig.controller.core

import android.util.Log
import com.robolig.controller.BuildConfig
import com.robolig.controller.utils.ControllerPreferences
import javax.inject.Inject
import javax.inject.Singleton

enum class LogTag(val value: String) {
    USB("USB"),
    VIDEO("VIDEO"),
    PACKET("PACKET"),
    IK("IK"),
    UI("UI"),
    VIEWMODEL("VIEWMODEL"),
    REPOSITORY("REPOSITORY"),
    SAFETY("SAFETY"),
    COMMUNICATION("COMMUNICATION"),
    SETTINGS("SETTINGS"),
}

interface AppLogger {
    fun d(
        tag: LogTag,
        message: String,
    )

    fun i(
        tag: LogTag,
        message: String,
    )

    fun w(
        tag: LogTag,
        message: String,
    )

    fun e(
        tag: LogTag,
        message: String,
        throwable: Throwable? = null,
    )
}

@Singleton
class AndroidLogger
    @Inject
    constructor(
        private val controllerPreferences: ControllerPreferences,
    ) : AppLogger {
        override fun d(
            tag: LogTag,
            message: String,
        ) {
            if (BuildConfig.DEBUG && shouldLog(LogLevel.DEBUG)) {
                Log.d(tag.value, message)
            }
        }

        override fun i(
            tag: LogTag,
            message: String,
        ) {
            if (shouldLog(LogLevel.INFO)) {
                Log.i(tag.value, message)
            }
        }

        override fun w(
            tag: LogTag,
            message: String,
        ) {
            if (shouldLog(LogLevel.WARN)) {
                Log.w(tag.value, message)
            }
        }

        override fun e(
            tag: LogTag,
            message: String,
            throwable: Throwable?,
        ) {
            if (shouldLog(LogLevel.ERROR)) {
                Log.e(tag.value, message, throwable)
            }
        }

        private fun shouldLog(level: LogLevel): Boolean =
            level.androidPriority >= controllerPreferences.logLevel.value.androidPriority &&
                controllerPreferences.logLevel.value != LogLevel.NONE
    }
