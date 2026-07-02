package com.robolig.controller.protocol

import com.robolig.controller.core.ProtocolConstants
import javax.inject.Inject
import javax.inject.Singleton

interface PacketBuilder {
    fun build(packet: Packet): ByteArray
}

@Singleton
class BinaryPacketBuilder
    @Inject
    constructor(
        private val checksum: Checksum,
    ) : PacketBuilder {
        override fun build(packet: Packet): ByteArray {
            val rawPacket = ByteArray(ProtocolConstants.PACKET_SIZE_BYTES)
            rawPacket[0] = ProtocolConstants.HEADER.toByte()
            rawPacket[1] = packet.type.wireValue.toByte()
            rawPacket[2] = packet.sequenceNumber.toByte()
            rawPacket[3] = packet.flags.toWireValue().toByte()
            packet.payload.copyInto(rawPacket, destinationOffset = 4)
            writeTimestamp(packet.timestampMillis, rawPacket)
            rawPacket[ProtocolConstants.PACKET_SIZE_BYTES - 1] =
                checksum.compute(
                    rawPacket.copyOf(ProtocolConstants.PACKET_SIZE_BYTES - 1),
                ).toByte()
            return rawPacket
        }
    }

internal fun writeTimestamp(
    timestampMillis: Int,
    destination: ByteArray,
) {
    val normalizedTimestamp = timestampMillis and ProtocolConstants.TIMESTAMP_MASK
    destination[28] = normalizedTimestamp.toByte()
    destination[29] = (normalizedTimestamp shr 8).toByte()
    destination[30] = (normalizedTimestamp shr 16).toByte()
}

internal fun readTimestamp(rawPacket: ByteArray): Int =
    rawPacket[28].unsignedValue() or
        (rawPacket[29].unsignedValue() shl 8) or
        (rawPacket[30].unsignedValue() shl 16)
