package com.robolig.controller.domain.model

import com.robolig.controller.core.ArmConstants

data class ArmPose(
    val forwardMeters: Float = ArmConstants.HOME_FORWARD_METERS,
    val heightMeters: Float = ArmConstants.HOME_HEIGHT_METERS,
    val lateralMeters: Float = ArmConstants.HOME_LATERAL_METERS,
    val wristRollDegrees: Float = ArmConstants.DEFAULT_WRIST_ROLL_DEGREES,
    val clawRotationDegrees: Float = ArmConstants.DEFAULT_CLAW_ROTATION_DEGREES,
)
