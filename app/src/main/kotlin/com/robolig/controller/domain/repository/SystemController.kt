package com.robolig.controller.domain.repository

import com.robolig.controller.core.LogLevel
import com.robolig.controller.domain.model.RobotMode

/**
 * Coordinates shared controller operations that affect the full robot or runtime.
 */
interface SystemController {
    suspend fun refreshStatus()

    fun setRobotMode(mode: RobotMode)

    fun updateLogLevel(level: LogLevel)

    suspend fun emergencyStop()

    suspend fun resetEmergencyStop()
}
