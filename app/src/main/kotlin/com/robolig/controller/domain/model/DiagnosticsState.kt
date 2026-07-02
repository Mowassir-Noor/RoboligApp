package com.robolig.controller.domain.model

import com.robolig.controller.core.LogLevel

data class DiagnosticsState(
    val queuedPackets: Int = 0,
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val droppedPackets: Long = 0L,
    val invalidPackets: Long = 0L,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val reconnectAttempts: Int = 0,
    val logLevel: LogLevel = LogLevel.DEBUG,
)
