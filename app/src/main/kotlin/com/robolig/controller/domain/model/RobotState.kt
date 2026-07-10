package com.robolig.controller.domain.model

data class RobotState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val currentMode: RobotMode = RobotMode.DRIVE,
    val battery: BatteryState = BatteryState(),
    val signal: SignalState = SignalState(),
    val vehicle: VehicleState = VehicleState(),
    val arm: ArmState = ArmState(),
    val camera: CameraState = CameraState(),
    val mission: MissionState = MissionState(),
    val telemetry: Telemetry = Telemetry(),
    val safety: SafetyState = SafetyState(),
    val diagnostics: DiagnosticsState = DiagnosticsState(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
)
