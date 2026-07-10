package com.robolig.controller.domain.model

enum class ConnectionState(
    val displayName: String,
) {
    DISCONNECTED("Disconnected"),
    USB_CONNECTED("USB Connected"),
    SERIAL_OPEN("Serial Open"),
    HEARTBEAT_RUNNING("Heartbeat Running"),
    CONNECTED("Connected"),
}
