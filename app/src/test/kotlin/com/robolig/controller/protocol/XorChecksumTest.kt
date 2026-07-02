package com.robolig.controller.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class XorChecksumTest {
    private val checksum = XorChecksum()

    @Test
    fun computesXorAcrossAllBytes() {
        val rawPacket =
            byteArrayOf(
                0xAA.toByte(),
                0x01,
                0x10,
                0x03,
                0x7F,
                0x55,
                0x00,
            )

        val expectedChecksum = 0xAA xor 0x01 xor 0x10 xor 0x03 xor 0x7F xor 0x55 xor 0x00

        assertEquals(expectedChecksum and 0xFF, checksum.compute(rawPacket))
    }
}
