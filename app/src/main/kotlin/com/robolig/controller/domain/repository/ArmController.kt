package com.robolig.controller.domain.repository

import com.robolig.controller.domain.model.ControlVector

/**
 * Sends manipulator and zipline commands while keeping Cartesian control in the app layer.
 */
interface ArmController {
    fun updatePlanarInput(planarInput: ControlVector)

    fun updateDepthInput(depthInput: Float)

    fun updateWristRotation(wristRotationInput: Float)

    fun setGripperOpen(open: Boolean)

    fun setPrecisionMode(enabled: Boolean)

    fun activatePreset(presetLabel: String)

    fun updateZiplineHeight(height: Float)
}
