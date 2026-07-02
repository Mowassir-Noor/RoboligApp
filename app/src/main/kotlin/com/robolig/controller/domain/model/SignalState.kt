package com.robolig.controller.domain.model

data class SignalState(
    val strengthPercent: Int? = null,
    val warning: Boolean = false,
)
