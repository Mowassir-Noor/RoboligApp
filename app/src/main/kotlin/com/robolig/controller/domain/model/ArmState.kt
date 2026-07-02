package com.robolig.controller.domain.model

data class ArmState(
    val precisionModeEnabled: Boolean = false,
    val locked: Boolean = true,
    val gripperOpen: Boolean = false,
    val activePreset: String? = null,
    val planarInput: ControlVector = ControlVector(),
    val depthInput: Float = 0f,
    val wristRotationInput: Float = 0f,
    val ziplineHeight: Float = 0f,
    val targetPose: ArmPose = ArmPose(),
    val jointAngles: JointAngles = JointAngles(),
    val workspaceClamped: Boolean = false,
)
