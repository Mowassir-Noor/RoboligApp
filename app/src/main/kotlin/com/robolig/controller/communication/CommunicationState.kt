package com.robolig.controller.communication

import com.robolig.controller.domain.model.ArmState
import com.robolig.controller.domain.model.BatteryState
import com.robolig.controller.domain.model.ConnectionState
import com.robolig.controller.domain.model.DiagnosticsState
import com.robolig.controller.domain.model.MissionState
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.SafetyState
import com.robolig.controller.domain.model.SignalState
import com.robolig.controller.domain.model.Telemetry
import com.robolig.controller.domain.model.VehicleState

data class CommunicationState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val currentMode: RobotMode = RobotMode.DRIVE,
    val battery: BatteryState = BatteryState(),
    val signal: SignalState = SignalState(),
    val vehicle: VehicleState = VehicleState(locked = false),
    val arm: ArmState = ArmState(locked = true),
    val mission: MissionState = MissionState(),
    val telemetry: Telemetry = Telemetry(),
    val safety: SafetyState = SafetyState(),
    val diagnostics: DiagnosticsState = DiagnosticsState(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
)
