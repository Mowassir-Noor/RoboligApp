package com.robolig.controller.protocol

import javax.inject.Inject
import javax.inject.Singleton

interface Checksum {
    fun compute(rawPacket: ByteArray): Int
}

@Singleton
class XorChecksum
    @Inject
    constructor() : Checksum {
        override fun compute(rawPacket: ByteArray): Int {
            var checksum = 0
            for (byteIndex in rawPacket.indices) {
                checksum = checksum xor rawPacket[byteIndex].toInt().and(0xFF)
            }
            return checksum and 0xFF
        }
    }
