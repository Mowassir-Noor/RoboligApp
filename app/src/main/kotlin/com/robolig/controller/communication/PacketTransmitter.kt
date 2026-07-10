package com.robolig.controller.communication

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogTag
import com.robolig.controller.protocol.PacketBuilder
import com.robolig.controller.usb.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
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
                val sent = usbSerialManager.send(packetBuilder.build(queuedPacket.packet))
                if (!sent) {
                    logger.w(
                        LogTag.USB,
                        "Dropping ${queuedPacket.packet.type} because the USB writer is unavailable",
                    )
                }
            }
        }
    }
