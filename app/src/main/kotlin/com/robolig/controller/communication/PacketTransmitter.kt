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

    private val lastOutboundSecondaryBytesState = MutableStateFlow<ByteArray?>(null)
    val lastOutboundSecondaryBytes: StateFlow<ByteArray?> = lastOutboundSecondaryBytesState.asStateFlow()

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
                when (classify(queuedPacket.packet.type)) {
                    CaptureSlot.PRIMARY -> lastOutboundBytesState.value = bytes
                    CaptureSlot.SECONDARY -> lastOutboundSecondaryBytesState.value = bytes
                    CaptureSlot.IGNORE -> Unit
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

        private fun classify(type: PacketType): CaptureSlot {
            if (type == PacketType.HEARTBEAT) {
                return CaptureSlot.IGNORE
            }
            if (type == PacketType.EMERGENCY_STOP) {
                return CaptureSlot.PRIMARY
            }
            if (type != PacketType.VEHICLE_CONTROL && type != PacketType.ARM_CONTROL) {
                return CaptureSlot.IGNORE
            }
            val mode = stateStore.state.value.currentMode
            val matchesMode =
                when (type) {
                    PacketType.VEHICLE_CONTROL -> mode == RobotMode.DRIVE
                    PacketType.ARM_CONTROL -> mode == RobotMode.GRIPPER || mode == RobotMode.ZIPLINE
                    else -> false
                }
            return if (matchesMode) CaptureSlot.PRIMARY else CaptureSlot.SECONDARY
        }
    }

    private enum class CaptureSlot {
        PRIMARY,
        SECONDARY,
        IGNORE,
    }
