package com.robolig.controller.communication

import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.ControlLoopConstants
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.RobotPacketFactory
import com.robolig.controller.robot.ArmKinematicsSolver
import com.robolig.controller.usb.UsbSerialManager
import com.robolig.controller.utils.ControllerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboundCommandScheduler
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
        private val packetFactory: RobotPacketFactory,
        private val commandQueue: CommandQueue,
        private val armKinematicsSolver: ArmKinematicsSolver,
        private val heartbeatManager: HeartbeatManager,
        private val usbSerialManager: UsbSerialManager,
        private val controllerPreferences: ControllerPreferences,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        private var started = false

        fun start() {
            if (started) {
                return
            }
            started = true
            applicationScope.launch { vehicleLoop() }
            applicationScope.launch { armLoop() }
            applicationScope.launch { heartbeatLoop() }
        }

        private suspend fun vehicleLoop() {
            while (applicationScope.isActive) {
                delay(ControlLoopConstants.VEHICLE_CONTROL_PERIOD_MS)
                val state = stateStore.state.value
                if (!shouldSendVehicle(state) || !isTransmissionAllowed()) {
                    continue
                }

                commandQueue.enqueue(
                    packet = packetFactory.createVehicleControlPacket(state.vehicle, state.currentMode, state.safety),
                    priority = PacketPriority.STANDARD,
                    description = "Vehicle control frame",
                )
            }
        }

        private suspend fun armLoop() {
            while (applicationScope.isActive) {
                delay(ControlLoopConstants.ARM_CONTROL_PERIOD_MS)
                val state = stateStore.state.value
                if (!shouldSendArm(state) || !isTransmissionAllowed()) {
                    continue
                }

                val motionResult = armKinematicsSolver.resolve(state.arm)
                stateStore.update { currentState ->
                    currentState.copy(
                        arm =
                            currentState.arm.copy(
                                targetPose = motionResult.targetPose,
                                jointAngles = motionResult.jointAngles,
                                workspaceClamped = motionResult.workspaceClamped,
                            ),
                    )
                }
                commandQueue.enqueue(
                    packet =
                        packetFactory.createArmControlPacket(
                            stateStore.state.value.arm,
                            motionResult.jointAngles,
                            state.currentMode,
                            state.safety,
                        ),
                    priority = PacketPriority.STANDARD,
                    description = "Arm control frame",
                )
            }
        }

        private suspend fun heartbeatLoop() {
            while (applicationScope.isActive) {
                delay(ControlLoopConstants.HEARTBEAT_INTERVAL_MS)
                if (!isTransmissionAllowed()) {
                    heartbeatManager.reset()
                    continue
                }

                val state = stateStore.state.value
                heartbeatManager.markSent()
                commandQueue.enqueue(
                    packet = packetFactory.createHeartbeatPacket(state.currentMode, state.safety),
                    priority = PacketPriority.HEARTBEAT,
                    description = "Heartbeat",
                )
            }
        }

        private fun isTransmissionAllowed(): Boolean =
            usbSerialManager.connection.value.isSerialOpen ||
                controllerPreferences.showPacketsOverlay.value
    }

private fun shouldSendVehicle(state: CommunicationState): Boolean =
    !state.safety.emergencyStopLatched &&
        !state.safety.watchdogTriggered &&
        !state.vehicle.locked &&
        state.currentMode in setOf(RobotMode.DRIVE, RobotMode.GRIPPER, RobotMode.ZIPLINE)

private fun shouldSendArm(state: CommunicationState): Boolean =
    !state.safety.emergencyStopLatched &&
        !state.safety.watchdogTriggered &&
        !state.arm.locked &&
        state.currentMode in setOf(RobotMode.GRIPPER, RobotMode.ZIPLINE)
