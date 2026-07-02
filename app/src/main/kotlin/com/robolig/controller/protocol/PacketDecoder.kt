package com.robolig.controller.protocol

import javax.inject.Inject
import javax.inject.Singleton

interface PacketDecoder {
    fun decode(rawPacket: ByteArray): PacketDecodeResult

    fun resetSequenceTracking()
}

@Singleton
class ProtocolPacketDecoder
    @Inject
    constructor(
        private val packetParser: PacketParser,
    ) : PacketDecoder {
        private var lastAcceptedSequence: Int? = null

        override fun decode(rawPacket: ByteArray): PacketDecodeResult =
            when (val parseResult = packetParser.parse(rawPacket)) {
                is PacketParseResult.Failure -> PacketDecodeResult.Failure(parseResult.reason)
                is PacketParseResult.Success -> validateSequence(parseResult.packet)
            }

        override fun resetSequenceTracking() {
            lastAcceptedSequence = null
        }

        private fun validateSequence(packet: Packet): PacketDecodeResult {
            val previousSequence = lastAcceptedSequence
            val decodeResult =
                if (previousSequence == null) {
                    PacketDecodeResult.Success(packet = packet, droppedSequenceCount = 0)
                } else {
                    val delta = (packet.sequenceNumber - previousSequence) and 0xFF
                    if (delta == 0 || delta > 127) {
                        PacketDecodeResult.Failure(PacketValidationFailure.INVALID_SEQUENCE)
                    } else {
                        PacketDecodeResult.Success(
                            packet = packet,
                            droppedSequenceCount = (delta - 1).coerceAtLeast(0),
                        )
                    }
                }

            if (decodeResult is PacketDecodeResult.Success) {
                lastAcceptedSequence = packet.sequenceNumber
            }
            return decodeResult
        }
    }
