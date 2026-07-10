package com.robolig.controller.usb

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.os.Build
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.robolig.controller.core.AppConstants
import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.IoDispatcher
import com.robolig.controller.core.LogTag
import com.robolig.controller.core.ProtocolConstants
import com.robolig.controller.utils.MonotonicClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CDC_SET_LINE_CODING = 0x20
private const val CDC_SET_CONTROL_LINE_STATE = 0x22
private const val CDC_REQUEST_TYPE = 0x21
private const val CDC_CONTROL_LINE_DTR_RTS = 0x03
private const val USB_FALLBACK_POLL_MS = 30_000L

private data class UsbEndpointProfile(
    val controlInterface: UsbInterface?,
    val dataInterface: UsbInterface,
    val readEndpoint: UsbEndpoint,
    val writeEndpoint: UsbEndpoint,
)

private data class UsbSession(
    val device: UsbDevice,
    val connection: UsbDeviceConnection,
    val claimedInterfaces: List<UsbInterface>,
    val readEndpoint: UsbEndpoint,
    val writeEndpoint: UsbEndpoint,
    var readJob: Job? = null,
)

private data class OpenSessionFailure(
    val message: String,
    val logMessage: String = message,
)

@Singleton
class UsbSerialManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val usbPermissionManager: UsbPermissionManager,
        private val clock: MonotonicClock,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val usbConnectionState = MutableStateFlow(initialConnectionSnapshot(context, clock))
        private val incomingPacketFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
        private var activeSession: UsbSession? = null

        val connection: StateFlow<UsbConnection> = usbConnectionState.asStateFlow()
        val incomingPackets: SharedFlow<ByteArray> = incomingPacketFlow.asSharedFlow()

        init {
            registerAttachmentReceiver()
            refreshConnectionState()
            applicationScope.launch {
                while (isActive) {
                    delay(USB_FALLBACK_POLL_MS)
                    refreshConnectionState()
                }
            }
        }

        private val attachmentReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        UsbManager.ACTION_USB_DEVICE_ATTACHED,
                        UsbManager.ACTION_USB_DEVICE_DETACHED,
                        -> refreshConnectionState()
                    }
                }
            }

        private fun registerAttachmentReceiver() {
            val filter =
                IntentFilter().apply {
                    addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(attachmentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(attachmentReceiver, filter)
            }
        }

        fun refreshConnectionState() {
            val usbManager = context.getSystemService(UsbManager::class.java)
            val attachedDevices = usbManager?.deviceList?.values?.toList().orEmpty()
            val candidateDevice = selectCandidateDevice(attachedDevices)
            val activeDevice = activeSession?.device

            if (candidateDevice == null && activeDevice != null) {
                close("USB bridge detached")
            }

            if (candidateDevice != null && !usbPermissionManager.hasPermission(candidateDevice)) {
                usbPermissionManager.requestPermission(candidateDevice)
            }

            if (candidateDevice != null &&
                usbPermissionManager.hasPermission(candidateDevice) &&
                activeDevice?.deviceId != candidateDevice.deviceId
            ) {
                applicationScope.launch {
                    openIfPossible(candidateDevice)
                }
            }

            usbConnectionState.update { currentState ->
                currentState.copy(
                    isUsbHostSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
                    attachedDeviceCount = attachedDevices.size,
                    attachedDeviceNames = attachedDevices.map(::deviceLabel),
                    activeDeviceName = activeSession?.device?.let(::deviceLabel),
                    isPermissionGranted = candidateDevice?.let(usbPermissionManager::hasPermission) == true,
                    isPermissionRequested = candidateDevice?.let(usbPermissionManager::isPermissionRequested) == true,
                    isSerialOpen = activeSession != null,
                    lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                )
            }
        }

        suspend fun send(rawPacket: ByteArray): Boolean =
            withContext(ioDispatcher) {
                val session = activeSession ?: return@withContext false
                val bytesWritten =
                    session.connection.bulkTransfer(
                        session.writeEndpoint,
                        rawPacket,
                        rawPacket.size,
                        AppConstants.DEFAULT_USB_WRITE_TIMEOUT_MS,
                    )
                if (bytesWritten != rawPacket.size) {
                    recordWriteFailure(bytesWritten)
                    return@withContext false
                }

                usbConnectionState.update { currentState ->
                    currentState.copy(
                        bytesSent = currentState.bytesSent + bytesWritten,
                        packetsSent = currentState.packetsSent + 1,
                        lastPacketAtMs = clock.elapsedRealtimeMs(),
                        lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                    )
                }
                true
            }

        fun close(reason: String) {
            val session = activeSession ?: return
            activeSession = null
            applicationScope.launch(ioDispatcher) {
                session.readJob?.cancel()
                closeSession(session)
            }
            usbConnectionState.update { currentState ->
                currentState.copy(
                    isSerialOpen = false,
                    activeDeviceName = null,
                    reconnectAttempts = currentState.reconnectAttempts + 1,
                    lastError = reason,
                    lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                )
            }
            logger.w(LogTag.USB, reason)
        }

        @SuppressLint("MissingPermission")
        private suspend fun openIfPossible(device: UsbDevice) {
            if (!shouldOpenDevice(device)) {
                return
            }

            withContext(ioDispatcher) {
                val usbManager = context.getSystemService(UsbManager::class.java) ?: return@withContext
                val session = openSession(device = device, usbManager = usbManager) ?: return@withContext
                activeSession = session

                usbConnectionState.update { currentState ->
                    currentState.copy(
                        activeDeviceName = deviceLabel(device),
                        isPermissionGranted = true,
                        isSerialOpen = true,
                        lastError = null,
                        lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                    )
                }
                logger.i(LogTag.USB, "USB serial connection opened for ${deviceLabel(device)}")
            }
        }

        private fun shouldOpenDevice(device: UsbDevice): Boolean {
            if (activeSession?.device?.deviceId == device.deviceId) {
                return false
            }
            if (activeSession != null) {
                close("Switching USB serial target")
            }
            return usbPermissionManager.hasPermission(device)
        }

        @SuppressLint("MissingPermission")
        private fun openSession(
            device: UsbDevice,
            usbManager: UsbManager,
        ): UsbSession? {
            val endpointProfile = resolveEndpointProfile(device)
            val deviceConnection = endpointProfile?.let { usbManager.openDevice(device) }
            val claimedInterfaces =
                if (deviceConnection != null) {
                    claimInterfaces(deviceConnection, checkNotNull(endpointProfile))
                } else {
                    emptyList()
                }
            val failure =
                when {
                    endpointProfile == null ->
                        OpenSessionFailure(
                            message = "Unsupported USB serial profile",
                            logMessage = "Attached USB device does not expose a compatible bulk endpoint pair",
                        )
                    deviceConnection == null -> OpenSessionFailure("Failed to open USB device")
                    claimedInterfaces.isEmpty() -> {
                        deviceConnection.close()
                        OpenSessionFailure("Failed to claim USB interfaces")
                    }
                    else -> null
                }

            if (failure != null) {
                return reportOpenFailure(failure.message, failure.logMessage)
            }

            val openConnection = checkNotNull(deviceConnection)
            val openProfile = checkNotNull(endpointProfile)

            configureCdcSerial(openConnection, openProfile)
            return UsbSession(
                device = device,
                connection = openConnection,
                claimedInterfaces = claimedInterfaces,
                readEndpoint = openProfile.readEndpoint,
                writeEndpoint = openProfile.writeEndpoint,
            ).also(::startReadLoop)
        }

        private fun startReadLoop(session: UsbSession) {
            session.readJob =
                applicationScope.launch(ioDispatcher) {
                    runReadLoop(
                        session = session,
                        incomingPackets = incomingPacketFlow,
                        onPacketReceived = ::recordPacketReceived,
                        onFailure = ::close,
                    )
                }
        }

        private fun reportOpenFailure(
            message: String,
            logMessage: String = message,
        ): UsbSession? {
            logger.w(LogTag.USB, logMessage)
            usbConnectionState.update { currentState ->
                currentState.copy(
                    lastError = message,
                    lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                )
            }
            return null
        }

        private fun recordPacketReceived() {
            usbConnectionState.update { currentState ->
                currentState.copy(
                    bytesReceived = currentState.bytesReceived + ProtocolConstants.PACKET_SIZE_BYTES,
                    packetsReceived = currentState.packetsReceived + 1,
                    lastPacketAtMs = clock.elapsedRealtimeMs(),
                    lastError = null,
                    lastUpdatedAtMs = clock.elapsedRealtimeMs(),
                )
            }
        }

        private fun recordWriteFailure(bytesWritten: Int) {
            close("USB write failed with $bytesWritten bytes written")
        }
    }

private fun initialConnectionSnapshot(
    context: Context,
    clock: MonotonicClock,
): UsbConnection =
    UsbConnection(
        isUsbHostSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
        lastUpdatedAtMs = clock.elapsedRealtimeMs(),
    )

private fun deviceLabel(device: UsbDevice): String =
    when {
        !device.productName.isNullOrBlank() -> device.productName.orEmpty()
        !device.manufacturerName.isNullOrBlank() -> "${device.manufacturerName} ${device.deviceName}"
        else -> device.deviceName
    }

private fun selectCandidateDevice(devices: List<UsbDevice>): UsbDevice? = devices.firstOrNull(::hasBulkEndpoints)

private fun hasBulkEndpoints(device: UsbDevice): Boolean = resolveEndpointProfile(device) != null

private fun resolveEndpointProfile(device: UsbDevice): UsbEndpointProfile? {
    val interfaces = (0 until device.interfaceCount).map(device::getInterface)
    val controlInterface = interfaces.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }
    return interfaces.firstNotNullOfOrNull { usbInterface ->
        val readEndpoint =
            (0 until usbInterface.endpointCount)
                .map(usbInterface::getEndpoint)
                .firstOrNull { endpoint ->
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        endpoint.direction == UsbConstants.USB_DIR_IN
                }
        val writeEndpoint =
            (0 until usbInterface.endpointCount)
                .map(usbInterface::getEndpoint)
                .firstOrNull { endpoint ->
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        endpoint.direction == UsbConstants.USB_DIR_OUT
                }
        if (readEndpoint != null && writeEndpoint != null) {
            UsbEndpointProfile(
                controlInterface = controlInterface,
                dataInterface = usbInterface,
                readEndpoint = readEndpoint,
                writeEndpoint = writeEndpoint,
            )
        } else {
            null
        }
    }
}

private fun claimInterfaces(
    deviceConnection: UsbDeviceConnection,
    endpointProfile: UsbEndpointProfile,
): List<UsbInterface> {
    val interfaces =
        buildList {
            endpointProfile.controlInterface?.let(::add)
            if (endpointProfile.controlInterface?.id != endpointProfile.dataInterface.id) {
                add(endpointProfile.dataInterface)
            }
        }

    return interfaces.filter { usbInterface ->
        deviceConnection.claimInterface(usbInterface, true)
    }
}

private fun configureCdcSerial(
    deviceConnection: UsbDeviceConnection,
    endpointProfile: UsbEndpointProfile,
) {
    val controlInterface = endpointProfile.controlInterface ?: return
    val lineCoding =
        byteArrayOf(
            0x00,
            0xC2.toByte(),
            0x01,
            0x00,
            0x00,
            0x00,
            0x08,
        )
    deviceConnection.controlTransfer(
        CDC_REQUEST_TYPE,
        CDC_SET_LINE_CODING,
        0,
        controlInterface.id,
        lineCoding,
        lineCoding.size,
        AppConstants.DEFAULT_USB_WRITE_TIMEOUT_MS,
    )
    deviceConnection.controlTransfer(
        CDC_REQUEST_TYPE,
        CDC_SET_CONTROL_LINE_STATE,
        CDC_CONTROL_LINE_DTR_RTS,
        controlInterface.id,
        null,
        0,
        AppConstants.DEFAULT_USB_WRITE_TIMEOUT_MS,
    )
}

private suspend fun runReadLoop(
    session: UsbSession,
    incomingPackets: MutableSharedFlow<ByteArray>,
    onPacketReceived: () -> Unit,
    onFailure: (String) -> Unit,
) {
    val readBuffer = ByteArray(256)
    var pendingBytes = ByteArray(0)

    while (currentCoroutineContext().isActive) {
        val bytesRead =
            session.connection.bulkTransfer(
                session.readEndpoint,
                readBuffer,
                readBuffer.size,
                AppConstants.DEFAULT_USB_READ_TIMEOUT_MS,
            )

        if (bytesRead < 0) {
            continue
        }

        val mergedBytes =
            if (pendingBytes.isEmpty()) {
                readBuffer.copyOf(bytesRead)
            } else {
                pendingBytes + readBuffer.copyOf(bytesRead)
            }
        var offset = 0
        while (offset + ProtocolConstants.PACKET_SIZE_BYTES <= mergedBytes.size) {
            val packetBytes = mergedBytes.copyOfRange(offset, offset + ProtocolConstants.PACKET_SIZE_BYTES)
            if (!incomingPackets.tryEmit(packetBytes)) {
                onFailure("USB read queue overflow")
                return
            }
            onPacketReceived()
            offset += ProtocolConstants.PACKET_SIZE_BYTES
        }
        pendingBytes =
            if (offset < mergedBytes.size) {
                mergedBytes.copyOfRange(offset, mergedBytes.size)
            } else {
                ByteArray(0)
            }
    }
}

private fun closeSession(session: UsbSession) {
    session.claimedInterfaces.forEach { usbInterface ->
        session.connection.releaseInterface(usbInterface)
    }
    session.connection.close()
}
