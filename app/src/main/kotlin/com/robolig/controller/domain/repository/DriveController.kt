package com.robolig.controller.domain.repository

import com.robolig.controller.domain.model.ControlVector

/**
 * Issues real-time drive commands for the vehicle control loop.
 */
interface DriveController {
    fun updateInput(
        translationInput: ControlVector,
        rotationInput: Float,
    )

    fun setBoostEnabled(enabled: Boolean)

    fun setBrakeActive(active: Boolean)
}
