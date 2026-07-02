package com.robolig.controller.communication

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogTag
import com.robolig.controller.core.TelemetryConstants
import com.robolig.controller.protocol.PacketDecodeResult
import com.robolig.controller.protocol.PacketDecoder
import com.robolig.controller.protocol.PacketType
import com.robolig.controller.protocol.TelemetryResponsePayload
import com.robolig.controller.usb.UsbSerialManager
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboundPacketProcessor
    @Inject
    constructor(
        private val usbSerialManager: UsbSerialManager,
        private val packetDecoder: PacketDecoder,
        private val heartbeatManager: HeartbeatManager,
        private val stateStore: CommunicationStateStore,
        private val clock: MonotonicClock,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        private var started = false

        fun start() {
            if (started) {
                return
            }
            started = true
            applicationScope.launch {
                usbSerialManager.incomingPackets.collectLatest { rawPacket ->
                    when (val decodeResult = packetDecoder.decode(rawPacket)) {
                        is PacketDecodeResult.Failure -> {
                            stateStore.update { currentState ->
                                currentState.copy(
                                    diagnostics =
                                        currentState.diagnostics.copy(
                                            invalidPackets = currentState.diagnostics.invalidPackets + 1,
                                        ),
                                    errors =
                                        when (decodeResult.reason) {
                                            com.robolig.controller.protocol.PacketValidationFailure.INVALID_SEQUENCE ->
                                                currentState.errors + "Duplicate or out-of-order packet rejected"
                                            else -> currentState.errors
                                        },
                                )
                            }
                        }

                        is PacketDecodeResult.Success -> {
                            val packet = decodeResult.packet
                            stateStore.update { currentState ->
                                currentState.copy(
                                    diagnostics =
                                        currentState.diagnostics.copy(
                                            packetsReceived = currentState.diagnostics.packetsReceived + 1,
                                            droppedPackets =
                                                currentState.diagnostics.droppedPackets +
                                                    decodeResult.droppedSequenceCount,
                                        ),
                                    safety =
                                        currentState.safety.copy(
                                            lastValidPacketAtMs = clock.elapsedRealtimeMs(),
                                            watchdogTriggered = false,
                                        ),
                                )
                            }
                            processPacket(packet.type, packet.payload)
                        }
                    }
                }
            }
        }

        private fun processPacket(
            packetType: PacketType,
            payload: ByteArray,
        ) {
            when (packetType) {
                PacketType.HEARTBEAT -> heartbeatManager.markReceived()
                PacketType.TELEMETRY_RESPONSE -> applyTelemetry(payload)
                PacketType.EMERGENCY_STOP ->
                    stateStore.update { currentState ->
                        currentState.copy(
                            safety = currentState.safety.copy(emergencyStopLatched = true),
                            errors = currentState.errors + "Robot reported emergency stop",
                        )
                    }
                else -> Unit
            }
        }

        private fun applyTelemetry(payload: ByteArray) {
            val telemetry = TelemetryResponsePayload.fromPayload(payload)
            val speedPercent = ((telemetry.currentSpeedRaw / 255f) * 100f).toInt().coerceIn(0, 100)
            val speedMetersPerSecond =
                (telemetry.currentSpeedRaw / 255f) * TelemetryConstants.MAX_SPEED_METERS_PER_SECOND
            stateStore.update { currentState ->
                applyMode(currentState, telemetry.currentMode).copy(
                    battery =
                        currentState.battery.copy(
                            percentage = telemetry.batteryPercent,
                            isLow = telemetry.batteryPercent <= 20,
                        ),
                    signal =
                        currentState.signal.copy(
                            strengthPercent = telemetry.signalPercent,
                            warning = telemetry.signalPercent <= 25,
                        ),
                    vehicle =
                        currentState.vehicle.copy(
                            speedPercent = speedPercent,
                            speedMetersPerSecond = speedMetersPerSecond,
                        ),
                    telemetry =
                        currentState.telemetry.copy(
                            temperatureCelsius = telemetry.temperatureCelsius,
                            cpuLoadPercent = telemetry.cpuLoadPercent,
                            motorCurrentAmps =
                                telemetry.motorCurrentRaw * TelemetryConstants.CURRENT_SCALE_AMPS_PER_LSB,
                            armCurrentAmps =
                                telemetry.armCurrentRaw * TelemetryConstants.CURRENT_SCALE_AMPS_PER_LSB,
                            errorCode = telemetry.errorCode,
                            latencyMs =
                                heartbeatManager.status.value.lastReceivedAtMs?.let { receivedAtMs ->
                                    heartbeatManager.status.value.lastSentAtMs?.let { sentAtMs ->
                                        (receivedAtMs - sentAtMs).toInt()
                                    }
                                },
                        ),
                )
            }
            if (telemetry.errorCode != 0) {
                logger.w(LogTag.PACKET, "Robot telemetry reported error code ${telemetry.errorCode}")
            }
        }
    }
