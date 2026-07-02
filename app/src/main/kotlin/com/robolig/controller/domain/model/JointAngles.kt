package com.robolig.controller.domain.model

import com.robolig.controller.core.ArmConstants
import com.robolig.controller.core.ProtocolConstants

data class JointAngles(
    val shoulderDegrees: Int = ArmConstants.HOME_SHOULDER_DEGREES,
    val elbowDegrees: Int = ArmConstants.HOME_ELBOW_DEGREES,
    val wristPitchDegrees: Int = ArmConstants.HOME_WRIST_PITCH_DEGREES,
    val wristRollDegrees: Int = ArmConstants.DEFAULT_WRIST_ROLL_DEGREES.toInt(),
    val gripperRotationDegrees: Int = ArmConstants.DEFAULT_CLAW_ROTATION_DEGREES.toInt(),
    val gripperCommand: Int = ProtocolConstants.GRIPPER_CLOSED,
)
