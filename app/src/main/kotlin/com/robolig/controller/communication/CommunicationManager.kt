package com.robolig.controller.communication

import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogLevel
import com.robolig.controller.domain.model.ConnectionState
import com.robolig.controller.domain.model.DiagnosticsState
import com.robolig.controller.domain.model.SafetyState
import com.robolig.controller.usb.UsbConnection
import com.robolig.controller.usb.UsbSerialManager
import com.robolig.controller.utils.ControllerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("LongParameterList")
class CommunicationManager
    @Inject
    constructor(
        stateStore: CommunicationStateStore,
        connectionMonitor: ConnectionMonitor,
        heartbeatManager: HeartbeatManager,
        commandQueue: CommandQueue,
        usbSerialManager: UsbSerialManager,
        controllerPreferences: ControllerPreferences,
        outboundCommandScheduler: OutboundCommandScheduler,
        packetTransmitter: PacketTransmitter,
        inboundPacketProcessor: InboundPacketProcessor,
        watchdogMonitor: WatchdogMonitor,
        @ApplicationScope applicationScope: CoroutineScope,
    ) {
    init {
        outboundCommandScheduler.start()
        packetTransmitter.start()
        inboundPacketProcessor.start()
        watchdogMonitor.start()
    }

    val state: StateFlow<CommunicationState> =
        combine(
            stateStore.state,
            connectionMonitor.connectionState,
            heartbeatManager.status,
            commandQueue.queueDepth,
            usbSerialManager.connection,
        ) { baseState, connectionState, heartbeatStatus, queueDepth, usbConnection ->
            baseState.copy(
                connectionState = connectionState,
                safety = mergeSafetyState(baseState.safety, heartbeatStatus.isHealthy),
                diagnostics =
                    mergeDiagnostics(
                        diagnosticsState = baseState.diagnostics,
                        queueDepth = queueDepth,
                        usbConnection = usbConnection,
                        logLevel = baseState.diagnostics.logLevel,
                    ),
                warnings = buildWarnings(baseState, connectionState, heartbeatStatus.isHealthy, usbConnection),
                errors = buildErrors(baseState.errors, usbConnection, connectionState, heartbeatStatus.isHealthy),
            )
        }.combine(packetTransmitter.lastOutboundBytes) { baseState, lastBytes ->
            baseState.copy(
                diagnostics = baseState.diagnostics.copy(lastOutboundBytes = lastBytes),
            )
        }.combine(packetTransmitter.lastOutboundSecondaryBytes) { baseState, lastSecondary ->
            baseState.copy(
                diagnostics = baseState.diagnostics.copy(lastOutboundSecondaryBytes = lastSecondary),
            )
        }.combine(controllerPreferences.logLevel) { baseState, logLevel ->
            baseState.copy(
                diagnostics = baseState.diagnostics.copy(logLevel = logLevel),
            )
        }.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = CommunicationState(),
        )
}

private fun mergeSafetyState(
    safetyState: SafetyState,
    heartbeatHealthy: Boolean,
): SafetyState = safetyState.copy(heartbeatHealthy = heartbeatHealthy)

private fun mergeDiagnostics(
    diagnosticsState: DiagnosticsState,
    queueDepth: Int,
    usbConnection: UsbConnection,
    logLevel: LogLevel,
): DiagnosticsState =
    diagnosticsState.copy(
        queuedPackets = queueDepth,
        packetsSent = usbConnection.packetsSent,
        bytesSent = usbConnection.bytesSent,
        bytesReceived = usbConnection.bytesReceived,
        reconnectAttempts = usbConnection.reconnectAttempts,
        logLevel = logLevel,
    )

private fun buildWarnings(
    state: CommunicationState,
    connectionState: ConnectionState,
    heartbeatHealthy: Boolean,
    usbConnection: UsbConnection,
): List<String> =
    buildList {
        addAll(state.warnings)
        if (!usbConnection.isUsbHostSupported) {
            add("USB host mode unavailable on this device")
        }
        if (usbConnection.attachedDeviceCount == 0) {
            add("Robot bridge is not attached")
        }
        if (usbConnection.attachedDeviceCount > 0 && !usbConnection.isPermissionGranted) {
            add("USB permission is pending")
        }
        if (usbConnection.isSerialOpen && !heartbeatHealthy) {
            add("Heartbeat handshake is not established yet")
        }
        if (state.battery.isLow) {
            add("Robot battery is low")
        }
        if (state.signal.warning) {
            add("Radio signal quality is low")
        }
        if (state.safety.watchdogTriggered) {
            add("Communication watchdog is active")
        }
        if (state.diagnostics.droppedPackets > 0) {
            add("Packet loss detected on telemetry link")
        }
        if (state.arm.workspaceClamped) {
            add("Manipulator workspace clamp is active")
        }
        if (connectionState == ConnectionState.SERIAL_OPEN && !heartbeatHealthy) {
            add("Serial link is open while the heartbeat is still stabilizing")
        }
    }

private fun buildErrors(
    baseErrors: List<String>,
    usbConnection: UsbConnection,
    connectionState: ConnectionState,
    heartbeatHealthy: Boolean,
): List<String> =
    buildList {
        addAll(baseErrors)
        usbConnection.lastError?.let(::add)
        if (connectionState == ConnectionState.SERIAL_OPEN && !heartbeatHealthy) {
            add("Serial link is open but heartbeat is unhealthy")
        }
    }
