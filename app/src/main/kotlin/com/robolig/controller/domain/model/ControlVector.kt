package com.robolig.controller.domain.model

import kotlin.math.atan2
import kotlin.math.hypot

data class ControlVector(
    val x: Float = 0f,
    val y: Float = 0f,
) {
    val magnitude: Float
        get() = hypot(x, y).coerceIn(0f, 1f)

    val angleDegrees: Float
        get() = if (magnitude == 0f) 0f else Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()

    fun clamp(): ControlVector =
        if (magnitude <= 1f) {
            this
        } else {
            val scale = 1f / magnitude
            copy(x = x * scale, y = y * scale)
        }
}
