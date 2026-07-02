package com.robolig.controller.communication

import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.domain.model.ConnectionState
import com.robolig.controller.usb.UsbConnection
import com.robolig.controller.usb.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionMonitor
    @Inject
    constructor(
        usbSerialManager: UsbSerialManager,
        heartbeatManager: HeartbeatManager,
        @ApplicationScope applicationScope: CoroutineScope,
    ) {
        val connectionState: StateFlow<ConnectionState> =
            combine(
                usbSerialManager.connection,
                heartbeatManager.status,
            ) { usbConnection, heartbeatStatus ->
                determineConnectionState(
                    usbConnection = usbConnection,
                    heartbeatStatus = heartbeatStatus,
                )
            }.stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = ConnectionState.DISCONNECTED,
            )

        private fun determineConnectionState(
            usbConnection: UsbConnection,
            heartbeatStatus: HeartbeatStatus,
        ): ConnectionState =
            when {
                usbConnection.attachedDeviceCount == 0 -> ConnectionState.DISCONNECTED
                !usbConnection.isPermissionGranted -> ConnectionState.USB_CONNECTED
                !usbConnection.isSerialOpen -> ConnectionState.USB_CONNECTED
                heartbeatStatus.lastSentAtMs != null && heartbeatStatus.lastReceivedAtMs == null ->
                    ConnectionState.HEARTBEAT_RUNNING
                heartbeatStatus.isHealthy -> ConnectionState.CONNECTED
                else -> ConnectionState.SERIAL_OPEN
            }
    }
