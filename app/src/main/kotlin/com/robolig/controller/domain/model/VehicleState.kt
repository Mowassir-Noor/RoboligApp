package com.robolig.controller.domain.model

data class VehicleState(
    val speedMetersPerSecond: Float = 0f,
    val speedPercent: Int = 0,
    val throttlePercent: Int = 0,
    val brakePercent: Int = 0,
    val boostEnabled: Boolean = false,
    val locked: Boolean = true,
    val translationInput: ControlVector = ControlVector(),
    val rotationInput: Float = 0f,
)
