package com.robolig.controller.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryPacketParserTest {
    private val checksum = XorChecksum()
    private val builder = BinaryPacketBuilder(checksum)
    private val parser = BinaryPacketParser(checksum)

    @Test
    fun parsesValidVehiclePacket() {
        val packet =
            Packet(
                type = PacketType.VEHICLE_CONTROL,
                sequenceNumber = 7,
                flags = PacketFlags(precisionMode = true),
                payload =
                    VehicleControlPayload(
                        moveX = 64,
                        moveY = -32,
                        rotation = 15,
                        throttle = 70,
                        brake = 0,
                        boost = 127,
                    ).toPayloadBytes(),
                timestampMillis = 0x0012_3456,
            )

        val result = parser.parse(builder.build(packet))

        assertTrue(result is PacketParseResult.Success)
        assertEquals(packet, (result as PacketParseResult.Success).packet)
    }

    @Test
    fun rejectsPacketWithInvalidChecksum() {
        val rawPacket =
            builder.build(
                Packet(type = PacketType.HEARTBEAT, sequenceNumber = 2),
            ).also { packetBytes ->
                packetBytes[packetBytes.lastIndex] =
                    (packetBytes.last().toInt() xor 0xFF).toByte()
            }

        val result = parser.parse(rawPacket)

        assertEquals(
            PacketParseResult.Failure(PacketValidationFailure.INVALID_CHECKSUM),
            result,
        )
    }

    @Test
    fun rejectsPacketWithUnknownType() {
        val rawPacket = builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 5))
        rawPacket[1] = 0x44
        rawPacket[rawPacket.lastIndex] = checksum.compute(rawPacket.copyOf(rawPacket.lastIndex)).toByte()

        val result = parser.parse(rawPacket)

        assertEquals(
            PacketParseResult.Failure(PacketValidationFailure.UNKNOWN_PACKET_TYPE),
            result,
        )
    }

    @Test
    fun rejectsArmPacketWithOutOfRangeServoPayload() {
        val rawPacket = builder.build(Packet(type = PacketType.ARM_CONTROL, sequenceNumber = 3))
        rawPacket[4] = 0xFF.toByte()
        rawPacket[rawPacket.lastIndex] = checksum.compute(rawPacket.copyOf(rawPacket.lastIndex)).toByte()

        val result = parser.parse(rawPacket)

        assertEquals(
            PacketParseResult.Failure(PacketValidationFailure.INVALID_PAYLOAD),
            result,
        )
    }
}
