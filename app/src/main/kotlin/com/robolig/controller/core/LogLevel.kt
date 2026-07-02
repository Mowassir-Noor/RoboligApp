package com.robolig.controller.core

import android.util.Log

enum class LogLevel(val androidPriority: Int) {
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
    NONE(Int.MAX_VALUE),
    ;

    companion object {
        fun fromPersistedValue(value: String?): LogLevel = entries.firstOrNull { it.name == value } ?: DEBUG
    }
}
