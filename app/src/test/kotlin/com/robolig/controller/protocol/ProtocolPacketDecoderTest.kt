package com.robolig.controller.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolPacketDecoderTest {
    private val checksum = XorChecksum()
    private val builder = BinaryPacketBuilder(checksum)
    private val decoder = ProtocolPacketDecoder(BinaryPacketParser(checksum))

    @Test
    fun acceptsFirstPacketAndTracksDroppedSequenceCount() {
        val first = decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 10)))
        val second = decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 13)))

        assertTrue(first is PacketDecodeResult.Success)
        assertEquals(0, (first as PacketDecodeResult.Success).droppedSequenceCount)
        assertTrue(second is PacketDecodeResult.Success)
        assertEquals(2, (second as PacketDecodeResult.Success).droppedSequenceCount)
    }

    @Test
    fun rejectsDuplicateSequenceNumbers() {
        decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 22)))

        val duplicate = decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 22)))

        assertEquals(
            PacketDecodeResult.Failure(PacketValidationFailure.INVALID_SEQUENCE),
            duplicate,
        )
    }

    @Test
    fun resetsSequenceTracking() {
        decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 22)))
        decoder.resetSequenceTracking()

        val result = decoder.decode(builder.build(Packet(type = PacketType.HEARTBEAT, sequenceNumber = 22)))

        assertTrue(result is PacketDecodeResult.Success)
        assertEquals(0, (result as PacketDecodeResult.Success).droppedSequenceCount)
    }
}
