package com.robolig.controller.domain.model

import com.robolig.controller.core.ControlLoopConstants

data class SafetyState(
    val emergencyStopLatched: Boolean = false,
    val heartbeatHealthy: Boolean = false,
    val watchdogTriggered: Boolean = false,
    val lastValidPacketAtMs: Long? = null,
    val lastEmergencyStopAtMs: Long? = null,
    val connectionTimeoutMs: Long = ControlLoopConstants.WATCHDOG_TIMEOUT_MS,
)
