package com.robolig.controller.domain.model

enum class ConnectionState(
    val displayName: String,
    val isOperational: Boolean,
) {
    DISCONNECTED("Disconnected", false),
    USB_CONNECTED("USB Connected", false),
    SERIAL_OPEN("Serial Open", false),
    HEARTBEAT_RUNNING("Heartbeat Running", false),
    CONNECTED("Connected", true),
}
