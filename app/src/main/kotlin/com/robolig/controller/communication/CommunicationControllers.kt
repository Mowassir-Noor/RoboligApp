package com.robolig.controller.communication

import com.robolig.controller.core.ArmConstants
import com.robolig.controller.core.LogLevel
import com.robolig.controller.core.LogTag
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.MissionState
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.RobotPacketFactory
import com.robolig.controller.usb.UsbSerialManager
import com.robolig.controller.utils.ControllerPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationDriveController
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
    ) {
        fun updateInput(
            translationInput: ControlVector,
            rotationInput: Float,
        ) {
            stateStore.update { currentState ->
                currentState.copy(
                    vehicle =
                        currentState.vehicle.copy(
                            translationInput = translationInput.clamp(),
                            rotationInput = rotationInput.coerceIn(-1f, 1f),
                            throttlePercent = (translationInput.magnitude * 100f).toInt(),
                            brakePercent =
                                if (currentState.vehicle.brakePercent > 0) {
                                    currentState.vehicle.brakePercent
                                } else {
                                    0
                                },
                        ),
                )
            }
        }

        fun setBoostEnabled(enabled: Boolean) {
            stateStore.update { currentState ->
                currentState.copy(
                    vehicle = currentState.vehicle.copy(boostEnabled = enabled),
                )
            }
        }

        fun setBrakeActive(active: Boolean) {
            stateStore.update { currentState ->
                currentState.copy(
                    vehicle =
                        currentState.vehicle.copy(
                            brakePercent = if (active) 100 else 0,
                            throttlePercent = if (active) 0 else currentState.vehicle.throttlePercent,
                        ),
                )
            }
        }
    }

@Singleton
class CommunicationArmController
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
    ) {
        fun updatePlanarInput(planarInput: ControlVector) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm =
                        currentState.arm.copy(
                            planarInput = planarInput.clamp(),
                            activePreset = if (planarInput.magnitude > 0f) null else currentState.arm.activePreset,
                        ),
                )
            }
        }

        fun updateDepthInput(depthInput: Float) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm =
                        currentState.arm.copy(
                            depthInput = depthInput.coerceIn(-1f, 1f),
                            activePreset = if (depthInput != 0f) null else currentState.arm.activePreset,
                        ),
                )
            }
        }

        fun updateWristRotation(wristRotationInput: Float) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm =
                        currentState.arm.copy(
                            wristRotationInput = wristRotationInput.coerceIn(-1f, 1f),
                            activePreset = if (wristRotationInput != 0f) null else currentState.arm.activePreset,
                        ),
                )
            }
        }

        fun setGripperOpen(open: Boolean) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm = currentState.arm.copy(gripperOpen = open),
                )
            }
        }

        fun setPrecisionMode(enabled: Boolean) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm = currentState.arm.copy(precisionModeEnabled = enabled),
                )
            }
        }

        fun activatePreset(presetLabel: String) {
            stateStore.update { currentState ->
                currentState.copy(
                    arm = currentState.arm.copy(activePreset = presetLabel),
                    mission =
                        currentState.mission.copy(
                            currentTask = "Executing $presetLabel preset",
                            progressPercent = 20,
                        ),
                )
            }
        }

        fun updateZiplineHeight(height: Float) {
            val normalizedHeight = height.coerceIn(0f, 1f)
            val targetHeight =
                ArmConstants.MIN_HEIGHT_METERS +
                    normalizedHeight * (ArmConstants.MAX_HEIGHT_METERS - ArmConstants.MIN_HEIGHT_METERS)
            stateStore.update { currentState ->
                currentState.copy(
                    arm =
                        currentState.arm.copy(
                            ziplineHeight = normalizedHeight,
                            targetPose = currentState.arm.targetPose.copy(heightMeters = targetHeight),
                            activePreset = null,
                        ),
                )
            }
        }
    }

@Singleton
class CommunicationMissionController
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
    ) {
        fun setPaused(paused: Boolean) {
            stateStore.update { currentState ->
                currentState.copy(
                    mission =
                        currentState.mission.copy(
                            isPaused = paused,
                            currentTask = if (paused) "Mission paused" else "Mission active",
                        ),
                )
            }
        }

        fun abort() {
            stateStore.update { currentState ->
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

@Singleton
class CommunicationSystemController
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
        private val usbSerialManager: UsbSerialManager,
        private val heartbeatManager: HeartbeatManager,
        private val commandQueue: CommandQueue,
        private val packetFactory: RobotPacketFactory,
        private val controllerPreferences: ControllerPreferences,
        private val logger: com.robolig.controller.core.AppLogger,
    ) {
        suspend fun refreshStatus() {
            usbSerialManager.refreshConnectionState()
            if (!usbSerialManager.connection.value.isPermissionGranted) {
                heartbeatManager.reset()
            }
            logger.i(LogTag.COMMUNICATION, "Communication status refresh requested")
        }

        fun setRobotMode(mode: RobotMode) {
            stateStore.update { currentState -> applyMode(currentState, mode) }
        }

        fun updateLogLevel(level: LogLevel) {
            controllerPreferences.updateLogLevel(level)
        }

        fun updateShowPacketsOverlay(enabled: Boolean) {
            controllerPreferences.updateShowPacketsOverlay(enabled)
        }

        suspend fun emergencyStop() {
            commandQueue.clear()
            stateStore.update { currentState ->
                currentState.copy(
                    vehicle =
                        currentState.vehicle.copy(
                            translationInput = ControlVector(),
                            rotationInput = 0f,
                            throttlePercent = 0,
                            brakePercent = 100,
                            locked = true,
                        ),
                    arm =
                        currentState.arm.copy(
                            planarInput = ControlVector(),
                            depthInput = 0f,
                            wristRotationInput = 0f,
                            locked = true,
                        ),
                    safety =
                        currentState.safety.copy(
                            emergencyStopLatched = true,
                            lastEmergencyStopAtMs = currentState.safety.lastValidPacketAtMs,
                        ),
                    errors = currentState.errors + "Emergency stop armed by operator",
                )
            }
            val stateSnapshot = stateStore.state.value
            commandQueue.enqueue(
                packet = packetFactory.createEmergencyStopPacket(stateSnapshot.currentMode),
                priority = PacketPriority.EMERGENCY_STOP,
                description = "Emergency stop requested by operator",
            )
            logger.w(LogTag.SAFETY, "Emergency stop queued")
        }

        suspend fun resetEmergencyStop() {
            val state = stateStore.state.value
            if (!state.safety.emergencyStopLatched && !state.safety.watchdogTriggered) {
                return
            }
            stateStore.update { currentState ->
                applyMode(currentState, currentState.currentMode).copy(
                    errors = currentState.errors + "Emergency stop released",
                )
            }
            logger.i(LogTag.SAFETY, "Emergency stop released by operator")
        }
    }

internal fun applyMode(
    currentState: CommunicationState,
    mode: RobotMode,
): CommunicationState =
    when (mode) {
        RobotMode.DRIVE ->
            currentState.copy(
                currentMode = mode,
                vehicle =
                    currentState.vehicle.copy(
                        locked = false,
                        translationInput = ControlVector(),
                        rotationInput = 0f,
                        throttlePercent = 0,
                        brakePercent = 0,
                    ),
                arm = currentState.arm.copy(locked = true),
                mission = currentState.mission.copy(missionName = "Manual Driving", currentTask = "Driving"),
                safety = currentState.safety.copy(emergencyStopLatched = false, watchdogTriggered = false),
            )
        RobotMode.GRIPPER ->
            currentState.copy(
                currentMode = mode,
                vehicle =
                    currentState.vehicle.copy(
                        locked = false,
                        translationInput = ControlVector(),
                        rotationInput = 0f,
                        throttlePercent = 0,
                        brakePercent = 0,
                    ),
                arm = currentState.arm.copy(locked = false),
                mission =
                    currentState.mission.copy(
                        missionName = "Manipulator Control",
                        currentTask = "Cartesian control active",
                    ),
                safety = currentState.safety.copy(watchdogTriggered = false),
            )
        RobotMode.ZIPLINE ->
            currentState.copy(
                currentMode = mode,
                vehicle =
                    currentState.vehicle.copy(
                        locked = false,
                        translationInput = ControlVector(),
                        rotationInput = 0f,
                        throttlePercent = 0,
                        brakePercent = 0,
                    ),
                arm =
                    currentState.arm.copy(
                        locked = false,
                        planarInput = ControlVector(),
                        depthInput = 0f,
                        wristRotationInput = 0f,
                        activePreset = null,
                    ),
                mission =
                    currentState.mission.copy(
                        missionName = "Zipline Operation",
                        currentTask = "Zipline controls active",
                    ),
                safety = currentState.safety.copy(watchdogTriggered = false),
            )
        RobotMode.AUTO ->
            currentState.copy(
                currentMode = mode,
                vehicle = currentState.vehicle.copy(locked = true),
                arm = currentState.arm.copy(locked = true),
                mission =
                    MissionState(
                        missionName = "Autonomous Mission",
                        currentTask = "Mission ready",
                        progressPercent = currentState.mission.progressPercent,
                        waypointLabel = currentState.mission.waypointLabel,
                        waypointIndex = currentState.mission.waypointIndex,
                        waypointCount = currentState.mission.waypointCount,
                    ),
            )
    }
