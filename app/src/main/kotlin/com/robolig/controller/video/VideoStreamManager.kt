package com.robolig.controller.video

import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.IoDispatcher
import com.robolig.controller.core.LogTag
import com.robolig.controller.core.VideoConstants
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.CameraStreamStatus
import com.robolig.controller.utils.ControllerPreferences
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoStreamManager
    @Inject
    constructor(
        private val controllerPreferences: ControllerPreferences,
        private val mjpegDecoder: MjpegDecoder,
        private val logger: AppLogger,
        private val clock: MonotonicClock,
        @ApplicationScope private val applicationScope: CoroutineScope,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val cameraStateStore = MutableStateFlow(CameraState())
        private var streamJob: Job? = null

        val cameraState: StateFlow<CameraState> = cameraStateStore.asStateFlow()

        init {
            applicationScope.launch {
                controllerPreferences.videoStreamUrl.collectLatest { streamUrl ->
                    restartStreaming(streamUrl.trim())
                }
            }
        }

        fun updateStreamUrl(url: String) {
            controllerPreferences.updateVideoStreamUrl(url)
            logger.d(LogTag.VIDEO, "Video stream configuration updated")
        }

        private fun restartStreaming(normalizedUrl: String) {
            streamJob?.cancel()
            if (normalizedUrl.isBlank()) {
                cameraStateStore.value =
                    CameraState(
                        streamUrl = "",
                        status = CameraStreamStatus.IDLE,
                        isMjpegCompatible = mjpegDecoder.supports("multipart/x-mixed-replace; boundary=frame"),
                    )
                return
            }

            cameraStateStore.update { currentState ->
                currentState.copy(
                    streamUrl = normalizedUrl,
                    status = CameraStreamStatus.CONFIGURED,
                    isStreaming = false,
                    lastError = null,
                )
            }

            streamJob =
                applicationScope.launch(ioDispatcher) {
                    var reconnectDelayMs = VideoConstants.RECONNECT_DELAY_MS
                    while (isActive) {
                        cameraStateStore.update { currentState ->
                            currentState.copy(
                                status = CameraStreamStatus.CONNECTING,
                                lastError = null,
                            )
                        }
                        try {
                            streamMjpeg(normalizedUrl)
                            reconnectDelayMs = VideoConstants.RECONNECT_DELAY_MS
                        } catch (ioException: IOException) {
                            logger.w(LogTag.VIDEO, "MJPEG stream error: ${ioException.message}")
                            cameraStateStore.update { currentState ->
                                currentState.copy(
                                    status = CameraStreamStatus.ERROR,
                                    isStreaming = false,
                                    lastError = ioException.message ?: "MJPEG stream disconnected",
                                    reconnectAttempts = currentState.reconnectAttempts + 1,
                                )
                            }
                            delay(reconnectDelayMs)
                            reconnectDelayMs =
                                (reconnectDelayMs * 2)
                                    .coerceAtMost(VideoConstants.MAX_RECONNECT_DELAY_MS)
                        }
                    }
                }
        }

        private suspend fun streamMjpeg(streamUrl: String) {
            withContext(ioDispatcher) {
                val frameTimestamps = ArrayDeque<Long>()
                val connection =
                    openConnection(streamUrl).also { httpConnection ->
                        cameraStateStore.update { currentState ->
                            currentState.copy(
                                status = CameraStreamStatus.CONNECTING,
                                lastError = null,
                                isMjpegCompatible = mjpegDecoder.supports(httpConnection.contentType),
                            )
                        }
                    }

                connection.useInputStream { inputStream ->
                    while (isActive) {
                        val frameStartedAtMs = clock.elapsedRealtimeMs()
                        val frameBytes = readNextFrame(inputStream) ?: throw IOException("MJPEG stream ended")
                        val frameCompletedAtMs = clock.elapsedRealtimeMs()
                        val framesPerSecond = calculateFramesPerSecond(frameTimestamps, frameCompletedAtMs)
                        cameraStateStore.update { currentState ->
                            currentState.copy(
                                streamUrl = streamUrl,
                                status = CameraStreamStatus.STREAMING,
                                isStreaming = true,
                                latencyMs = (frameCompletedAtMs - frameStartedAtMs).toInt(),
                                framesPerSecond = framesPerSecond,
                                lastError = null,
                                frameBytes = frameBytes,
                                frameSequence = currentState.frameSequence + 1,
                                lastFrameAtMs = frameCompletedAtMs,
                            )
                        }
                    }
                }
            }
        }
    }

private fun calculateFramesPerSecond(
    frameTimestamps: ArrayDeque<Long>,
    nowMs: Long,
): Int {
    frameTimestamps.addLast(nowMs)
    while (frameTimestamps.isNotEmpty() && nowMs - frameTimestamps.first() > VideoConstants.FRAME_HISTORY_WINDOW_MS) {
        frameTimestamps.removeFirst()
    }
    return frameTimestamps.size
}

private fun openConnection(streamUrl: String): HttpURLConnection =
    (URL(streamUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = VideoConstants.CONNECT_TIMEOUT_MS
        readTimeout = VideoConstants.READ_TIMEOUT_MS
        useCaches = false
        doInput = true
        connect()
    }

private fun HttpURLConnection.useInputStream(block: (BufferedInputStream) -> Unit) {
    inputStream.buffered().use { bufferedInputStream ->
        block(bufferedInputStream)
    }
    disconnect()
}

private fun readNextFrame(inputStream: BufferedInputStream): ByteArray? {
    var previousByte = -1
    var frameStarted = false
    val buffer = ArrayList<Byte>(32_768)

    while (true) {
        val currentByte = inputStream.read()
        if (currentByte == -1) {
            return null
        }

        if (!frameStarted) {
            if (previousByte == JPEG_MARKER_PREFIX && currentByte == JPEG_START_MARKER) {
                frameStarted = true
                buffer.add(JPEG_MARKER_PREFIX.toByte())
                buffer.add(JPEG_START_MARKER.toByte())
            }
        } else {
            buffer.add(currentByte.toByte())
            if (previousByte == JPEG_MARKER_PREFIX && currentByte == JPEG_END_MARKER) {
                return buffer.toByteArray()
            }
        }

        previousByte = currentByte
    }
}

private fun ArrayList<Byte>.toByteArray(): ByteArray =
    ByteArray(size).also { byteArray ->
        forEachIndexed { index, value ->
            byteArray[index] = value
        }
    }

private const val JPEG_MARKER_PREFIX = 0xFF
private const val JPEG_START_MARKER = 0xD8
private const val JPEG_END_MARKER = 0xD9
