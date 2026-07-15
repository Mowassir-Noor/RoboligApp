package com.robolig.controller.communication

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogTag
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.protocol.PacketBuilder
import com.robolig.controller.protocol.PacketType
import com.robolig.controller.usb.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val POLL_FALLBACK_MS = 64L

@Singleton
class PacketTransmitter
    @Inject
    constructor(
        private val commandQueue: CommandQueue,
        private val packetBuilder: PacketBuilder,
        private val usbSerialManager: UsbSerialManager,
        private val stateStore: CommunicationStateStore,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        private val lastOutboundBytesState = MutableStateFlow<ByteArray?>(null)
        val lastOutboundBytes: StateFlow<ByteArray?> = lastOutboundBytesState.asStateFlow()

        private var started = false

        fun start() {
            if (started) {
                return
            }
            started = true
            applicationScope.launch {
                while (applicationScope.isActive) {
                    withTimeoutOrNull(POLL_FALLBACK_MS) {
                        commandQueue.packetAvailableSignal().receive()
                    }
                    drainAndSend()
                }
            }
        }

        private suspend fun drainAndSend() {
            while (true) {
                val queuedPacket = commandQueue.poll() ?: return
                val bytes = packetBuilder.build(queuedPacket.packet)
                if (shouldCapture(queuedPacket.packet.type)) {
                    lastOutboundBytesState.value = bytes
                }
                val sent = usbSerialManager.send(bytes)
                if (!sent) {
                    logger.w(
                        LogTag.USB,
                        "Dropping ${queuedPacket.packet.type} because the USB writer is unavailable",
                    )
                }
            }
        }

        private fun shouldCapture(type: PacketType): Boolean {
            if (type == PacketType.HEARTBEAT) {
                return false
            }
            if (type == PacketType.EMERGENCY_STOP) {
                return true
            }
            val mode = stateStore.state.value.currentMode
            return when (type) {
                PacketType.VEHICLE_CONTROL -> mode == RobotMode.DRIVE
                PacketType.ARM_CONTROL -> mode == RobotMode.GRIPPER || mode == RobotMode.ZIPLINE
                else -> false
            }
        }
    }
