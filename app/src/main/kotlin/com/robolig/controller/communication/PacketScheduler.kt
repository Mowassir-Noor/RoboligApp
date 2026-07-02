package com.robolig.controller.communication

import com.robolig.controller.core.ControlLoopConstants
import com.robolig.controller.utils.hzToPeriodMillis

data class PacketSchedule(
    val vehicleControlPeriodMs: Long = hzToPeriodMillis(ControlLoopConstants.VEHICLE_CONTROL_HZ),
    val armControlPeriodMs: Long = hzToPeriodMillis(ControlLoopConstants.ARM_CONTROL_HZ),
    val telemetryPeriodMs: Long = hzToPeriodMillis(ControlLoopConstants.TELEMETRY_HZ),
    val heartbeatIntervalMs: Long = ControlLoopConstants.HEARTBEAT_INTERVAL_MS,
)

class PacketScheduler {
    val schedule: PacketSchedule = PacketSchedule()
}
