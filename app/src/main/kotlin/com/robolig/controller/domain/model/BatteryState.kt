package com.robolig.controller.domain.model

data class BatteryState(
    val percentage: Int? = null,
    val isLow: Boolean = false,
    val isCharging: Boolean = false,
)
