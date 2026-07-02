package com.robolig.controller.video

data class VideoStatistics(
    val framesPerSecond: Int = 0,
    val latencyMs: Int? = null,
    val lastFrameAtMs: Long? = null,
    val reconnectAttempts: Int = 0,
)
