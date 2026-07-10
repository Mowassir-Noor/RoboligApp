package com.robolig.controller.testing

import com.robolig.controller.core.LogLevel
import com.robolig.controller.domain.model.CameraStreamStatus
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.DiagnosticsState
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.model.VehicleState
import com.robolig.controller.domain.repository.ArmController
import com.robolig.controller.domain.repository.DriveController
import com.robolig.controller.domain.repository.MissionController
import com.robolig.controller.domain.repository.RobotRepository
import com.robolig.controller.domain.repository.SystemController
import com.robolig.controller.domain.repository.VideoController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeRobotRepository(
    initialState: RobotState = RobotState(),
) : RobotRepository {
    private val robotStateFlow = MutableStateFlow(initialState)

    override val robotState: StateFlow<RobotState> = robotStateFlow.asStateFlow()

    override val driveController: DriveController =
        object : DriveController {
            override fun updateInput(
                translationInput: ControlVector,
                rotationInput: Float,
            ) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        vehicle =
                            currentState.vehicle.copy(
                                translationInput = translationInput,
                                rotationInput = rotationInput,
                                speedPercent = (translationInput.magnitude * 100f).toInt(),
                            ),
                    )
                }
            }

            override fun setBoostEnabled(enabled: Boolean) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        vehicle = currentState.vehicle.copy(boostEnabled = enabled),
                    )
                }
            }

            override fun setBrakeActive(active: Boolean) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        vehicle = currentState.vehicle.copy(brakePercent = if (active) 100 else 0),
                    )
                }
            }
        }

    override val armController: ArmController =
        object : ArmController {
            override fun updatePlanarInput(planarInput: ControlVector) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(planarInput = planarInput),
                    )
                }
            }

            override fun updateDepthInput(depthInput: Float) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(depthInput = depthInput),
                    )
                }
            }

            override fun updateWristRotation(wristRotationInput: Float) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(wristRotationInput = wristRotationInput),
                    )
                }
            }

            override fun setGripperOpen(open: Boolean) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(gripperOpen = open),
                    )
                }
            }

            override fun setPrecisionMode(enabled: Boolean) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(precisionModeEnabled = enabled),
                    )
                }
            }

            override fun activatePreset(presetLabel: String) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(activePreset = presetLabel),
                    )
                }
            }

            override fun updateZiplineHeight(height: Float) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        arm = currentState.arm.copy(ziplineHeight = height.coerceIn(0f, 1f)),
                    )
                }
            }
        }

    override val missionController: MissionController =
        object : MissionController {
            override fun setPaused(paused: Boolean) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        mission =
                            currentState.mission.copy(
                                isPaused = paused,
                                currentTask = if (paused) "Mission paused" else "Mission active",
                            ),
                    )
                }
            }

            override fun abort() {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        mission =
                            currentState.mission.copy(
                                currentTask = "Mission aborted",
                                progressPercent = 0,
                                isPaused = false,
                            ),
                    )
                }
            }
        }

    override val systemController: SystemController =
        object : SystemController {
            override suspend fun refreshStatus() = Unit

            override fun setRobotMode(mode: RobotMode) {
                robotStateFlow.update { currentState -> currentState.copy(currentMode = mode) }
            }

            override fun updateLogLevel(level: LogLevel) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        diagnostics = currentState.diagnostics.copy(logLevel = level),
                    )
                }
            }

            override suspend fun emergencyStop() {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        errors = currentState.errors + "Emergency stop requested",
                        vehicle = VehicleState(brakePercent = 100),
                    )
                }
            }

            override suspend fun resetEmergencyStop() {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        vehicle = currentState.vehicle.copy(brakePercent = 0, locked = false),
                        safety = currentState.safety.copy(emergencyStopLatched = false),
                    )
                }
            }
        }

    override val videoController: VideoController =
        object : VideoController {
            override fun updateStreamUrl(url: String) {
                robotStateFlow.update { currentState ->
                    currentState.copy(
                        camera =
                            currentState.camera.copy(
                                streamUrl = url.trim(),
                                status =
                                    if (url.isBlank()) {
                                        CameraStreamStatus.IDLE
                                    } else {
                                        CameraStreamStatus.CONFIGURED
                                    },
                            ),
                        diagnostics = DiagnosticsState(logLevel = currentState.diagnostics.logLevel),
                    )
                }
            }
        }
}
