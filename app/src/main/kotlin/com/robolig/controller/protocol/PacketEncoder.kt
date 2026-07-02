package com.robolig.controller.protocol

import javax.inject.Inject
import javax.inject.Singleton

interface PacketEncoder {
    fun encode(packet: Packet): ByteArray
}

@Singleton
class BinaryPacketEncoder
    @Inject
    constructor(
        private val packetBuilder: PacketBuilder,
    ) : PacketEncoder {
        override fun encode(packet: Packet): ByteArray = packetBuilder.build(packet)
    }
