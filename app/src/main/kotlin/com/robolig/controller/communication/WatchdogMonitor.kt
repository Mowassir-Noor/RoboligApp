package com.robolig.controller.communication

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.ControlLoopConstants
import com.robolig.controller.core.LogTag
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.RobotPacketFactory
import com.robolig.controller.usb.UsbSerialManager
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchdogMonitor
    @Inject
    constructor(
        private val stateStore: CommunicationStateStore,
        private val commandQueue: CommandQueue,
        private val packetFactory: RobotPacketFactory,
        private val clock: MonotonicClock,
        private val usbSerialManager: UsbSerialManager,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        private var started = false

        fun start() {
            if (started) {
                return
            }
            started = true
            applicationScope.launch { monitorLoop() }
        }

        private suspend fun monitorLoop() {
            while (applicationScope.isActive) {
                delay(200L)
                val state = stateStore.state.value
                if (shouldTriggerWatchdog(state)) {
                    triggerWatchdogStop(state)
                }
            }
        }

        private fun shouldTriggerWatchdog(state: CommunicationState): Boolean {
            val lastValidPacketAtMs = state.safety.lastValidPacketAtMs ?: return false
            return usbSerialManager.connection.value.isSerialOpen &&
                !state.safety.emergencyStopLatched &&
                !state.safety.watchdogTriggered &&
                clock.elapsedRealtimeMs() - lastValidPacketAtMs > ControlLoopConstants.WATCHDOG_TIMEOUT_MS
        }

        private suspend fun triggerWatchdogStop(state: CommunicationState) {
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
                    safety = currentState.safety.copy(watchdogTriggered = true),
                    errors = currentState.errors + "Communication watchdog triggered",
                )
            }
            commandQueue.clear()
            commandQueue.enqueue(
                packet = packetFactory.createEmergencyStopPacket(state.currentMode),
                priority = PacketPriority.EMERGENCY_STOP,
                description = "Watchdog safety stop",
            )
            logger.w(LogTag.SAFETY, "Watchdog triggered automatic safety stop")
        }
    }
