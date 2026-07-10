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
                    )
                return
            }

            cameraStateStore.update { currentState ->
                currentState.copy(
                    streamUrl = normalizedUrl,
                    status = CameraStreamStatus.CONFIGURED,
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
                    openConnection(streamUrl).also {
                        cameraStateStore.update { currentState ->
                            currentState.copy(
                                status = CameraStreamStatus.CONNECTING,
                                lastError = null,
                            )
                        }
                    }

                connection.useInputStream { inputStream ->
                    var scratchBuffer = ByteArray(VideoConstants.INITIAL_FRAME_BUFFER_BYTES)
                    while (isActive) {
                        val frameStartedAtMs = clock.elapsedRealtimeMs()
                        val frameRead = readNextFrame(inputStream, scratchBuffer)
                        scratchBuffer = frameRead.buffer
                        if (frameRead.size == 0) {
                            throw IOException("MJPEG stream ended")
                        }
                        val frameBytes = ByteArray(frameRead.size).also { destination ->
                            System.arraycopy(scratchBuffer, 0, destination, 0, frameRead.size)
                        }
                        val frameCompletedAtMs = clock.elapsedRealtimeMs()
                        val framesPerSecond = calculateFramesPerSecond(frameTimestamps, frameCompletedAtMs)
                        cameraStateStore.update { currentState ->
                            currentState.copy(
                                streamUrl = streamUrl,
                                status = CameraStreamStatus.STREAMING,
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

private data class FrameRead(
    val buffer: ByteArray,
    val size: Int,
)

private fun readNextFrame(
    inputStream: BufferedInputStream,
    initialBuffer: ByteArray,
): FrameRead {
    var previousByte = -1
    var frameStarted = false
    var frameSize = 0
    var buffer = initialBuffer

    while (true) {
        val currentByte = inputStream.read()
        if (currentByte == -1) {
            return FrameRead(buffer = buffer, size = 0)
        }

        if (frameStarted && frameSize >= buffer.size) {
            buffer = buffer.copyOf(buffer.size * 2)
        }

        if (!frameStarted) {
            if (previousByte == JPEG_MARKER_PREFIX && currentByte == JPEG_START_MARKER) {
                frameStarted = true
                buffer[frameSize++] = JPEG_MARKER_PREFIX.toByte()
                buffer[frameSize++] = JPEG_START_MARKER.toByte()
            }
        } else {
            buffer[frameSize++] = currentByte.toByte()
            if (previousByte == JPEG_MARKER_PREFIX && currentByte == JPEG_END_MARKER) {
                return FrameRead(buffer = buffer, size = frameSize)
            }
        }

        previousByte = currentByte
    }
}

private const val JPEG_MARKER_PREFIX = 0xFF
private const val JPEG_START_MARKER = 0xD8
private const val JPEG_END_MARKER = 0xD9
