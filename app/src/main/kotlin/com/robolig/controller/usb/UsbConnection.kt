package com.robolig.controller.usb

import com.robolig.controller.core.AppConstants

data class UsbConnection(
    val isUsbHostSupported: Boolean = false,
    val attachedDeviceCount: Int = 0,
    val attachedDeviceNames: List<String> = emptyList(),
    val activeDeviceName: String? = null,
    val isPermissionGranted: Boolean = false,
    val isPermissionRequested: Boolean = false,
    val isSerialOpen: Boolean = false,
    val baudRate: Int = AppConstants.USB_BAUD_RATE,
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val packetsSent: Long = 0L,
    val reconnectAttempts: Int = 0,
    val lastPacketAtMs: Long? = null,
    val lastError: String? = null,
    val lastUpdatedAtMs: Long = 0L,
)
