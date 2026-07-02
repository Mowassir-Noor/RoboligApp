package com.robolig.controller.domain.model

enum class CameraStreamStatus(val displayName: String) {
    IDLE("Idle"),
    CONFIGURED("Configured"),
    CONNECTING("Connecting"),
    STREAMING("Streaming"),
    ERROR("Error"),
}

data class CameraState(
    val streamUrl: String = "",
    val status: CameraStreamStatus = CameraStreamStatus.IDLE,
    val isStreaming: Boolean = false,
    val latencyMs: Int? = null,
    val framesPerSecond: Int = 0,
    val isMjpegCompatible: Boolean = false,
    val lastError: String? = null,
    val frameBytes: ByteArray? = null,
    val frameSequence: Long = 0L,
    val lastFrameAtMs: Long? = null,
    val reconnectAttempts: Int = 0,
)
