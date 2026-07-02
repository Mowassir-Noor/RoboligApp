package com.robolig.controller.domain.model

data class Telemetry(
    val temperatureCelsius: Int? = null,
    val cpuLoadPercent: Int? = null,
    val motorCurrentAmps: Float? = null,
    val armCurrentAmps: Float? = null,
    val latencyMs: Int? = null,
    val errorCode: Int? = null,
)
