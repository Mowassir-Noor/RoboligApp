package com.robolig.controller.communication

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogTag
import com.robolig.controller.protocol.PacketEncoder
import com.robolig.controller.usb.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketTransmitter
    @Inject
    constructor(
        private val commandQueue: CommandQueue,
        private val packetEncoder: PacketEncoder,
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
                    val queuedPacket = commandQueue.poll()
                    if (queuedPacket == null) {
                        delay(4L)
                        continue
                    }

                    val sent = usbSerialManager.send(packetEncoder.encode(queuedPacket.packet))
                    if (!sent) {
                        logger.w(
                            LogTag.USB,
                            "Dropping ${queuedPacket.packet.type} because the USB writer is unavailable",
                        )
                    }
                }
            }
        }
    }
