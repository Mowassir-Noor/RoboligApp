package com.robolig.controller.robot

import com.robolig.controller.core.ArmConstants
import com.robolig.controller.core.ProtocolConstants
import com.robolig.controller.domain.model.ArmPose
import com.robolig.controller.domain.model.ArmState
import com.robolig.controller.domain.model.JointAngles
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ArmPreset(
    val label: String,
    val pose: ArmPose,
) {
    HOME(
        label = "Home",
        pose =
            ArmPose(
                forwardMeters = ArmConstants.HOME_FORWARD_METERS,
                heightMeters = ArmConstants.HOME_HEIGHT_METERS,
                lateralMeters = ArmConstants.HOME_LATERAL_METERS,
            ),
    ),
    PICK(
        label = "Pick",
        pose = ArmPose(forwardMeters = 0.18f, heightMeters = 0.08f, lateralMeters = 0f),
    ),
    PLACE(
        label = "Place",
        pose = ArmPose(forwardMeters = 0.28f, heightMeters = 0.18f, lateralMeters = 0.08f),
    ),
    ;

    companion object {
        fun fromLabel(label: String?): ArmPreset? = entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}

data class ArmMotionResult(
    val targetPose: ArmPose,
    val jointAngles: JointAngles,
    val workspaceClamped: Boolean,
)

@Singleton
class ArmKinematicsSolver
    @Inject
    constructor() {
        fun resolve(armState: ArmState): ArmMotionResult {
            val translationStep =
                if (armState.precisionModeEnabled) {
                    ArmConstants.PRECISION_TRANSLATION_STEP_METERS
                } else {
                    ArmConstants.NORMAL_TRANSLATION_STEP_METERS
                }
            val rotationStep =
                if (armState.precisionModeEnabled) {
                    ArmConstants.PRECISION_ROTATION_STEP_DEGREES
                } else {
                    ArmConstants.NORMAL_ROTATION_STEP_DEGREES
                }

            val presetPose = ArmPreset.fromLabel(armState.activePreset)?.pose
            val basePose = presetPose ?: armState.targetPose
            val updatedLateralMeters = basePose.lateralMeters + armState.planarInput.x * translationStep
            val targetPose =
                basePose.copy(
                    forwardMeters = basePose.forwardMeters + armState.depthInput * translationStep,
                    heightMeters = basePose.heightMeters + armState.planarInput.y * translationStep,
                    lateralMeters = updatedLateralMeters,
                    wristRollDegrees = basePose.wristRollDegrees + armState.wristRotationInput * rotationStep,
                    clawRotationDegrees = inferClawRotation(updatedLateralMeters),
                )

            val clampedPose = clampPose(targetPose)
            return ArmMotionResult(
                targetPose = clampedPose,
                jointAngles = inverseKinematics(clampedPose, armState.gripperOpen),
                workspaceClamped = clampedPose != targetPose,
            )
        }

        fun forwardKinematics(jointAngles: JointAngles): ArmPose {
            val shoulderRadians =
                Math.toRadians(
                    (jointAngles.shoulderDegrees - ArmConstants.SHOULDER_SERVO_OFFSET_DEGREES).toDouble(),
                )
            val elbowRadians =
                Math.toRadians(
                    (jointAngles.elbowDegrees - ArmConstants.ELBOW_SERVO_OFFSET_DEGREES).toDouble(),
                )
            val reachMeters =
                ArmConstants.SHOULDER_LINK_METERS * cos(shoulderRadians) +
                    ArmConstants.ELBOW_LINK_METERS * cos(shoulderRadians + elbowRadians)
            val heightMeters =
                ArmConstants.SHOULDER_LINK_METERS * sin(shoulderRadians) +
                    ArmConstants.ELBOW_LINK_METERS * sin(shoulderRadians + elbowRadians)
            return clampPose(
                ArmPose(
                    forwardMeters = reachMeters.toFloat(),
                    heightMeters = heightMeters.toFloat(),
                    lateralMeters = ((jointAngles.gripperRotationDegrees - 90) / 90f) * ArmConstants.MAX_LATERAL_METERS,
                    wristRollDegrees = jointAngles.wristRollDegrees.toFloat(),
                    clawRotationDegrees = jointAngles.gripperRotationDegrees.toFloat(),
                ),
            )
        }
    }

private fun clampPose(pose: ArmPose): ArmPose =
    pose.copy(
        forwardMeters = pose.forwardMeters.coerceIn(ArmConstants.MIN_FORWARD_METERS, ArmConstants.MAX_FORWARD_METERS),
        heightMeters = pose.heightMeters.coerceIn(ArmConstants.MIN_HEIGHT_METERS, ArmConstants.MAX_HEIGHT_METERS),
        lateralMeters = pose.lateralMeters.coerceIn(ArmConstants.MIN_LATERAL_METERS, ArmConstants.MAX_LATERAL_METERS),
        wristRollDegrees =
            pose.wristRollDegrees.coerceIn(
                ProtocolConstants.MIN_SERVO_ANGLE.toFloat(),
                ProtocolConstants.MAX_SERVO_ANGLE.toFloat(),
            ),
        clawRotationDegrees =
            pose.clawRotationDegrees.coerceIn(
                ProtocolConstants.MIN_SERVO_ANGLE.toFloat(),
                ProtocolConstants.MAX_SERVO_ANGLE.toFloat(),
            ),
    )

private fun inverseKinematics(
    pose: ArmPose,
    gripperOpen: Boolean,
): JointAngles {
    val planarReachMeters = hypot(pose.forwardMeters, pose.lateralMeters)
    val clampedReachMeters =
        planarReachMeters.coerceIn(
            ArmConstants.MIN_FORWARD_METERS,
            ArmConstants.SHOULDER_LINK_METERS + ArmConstants.ELBOW_LINK_METERS - 0.01f,
        )
    val heightMeters = pose.heightMeters
    val reachSquared = clampedReachMeters * clampedReachMeters + heightMeters * heightMeters
    val elbowCosine =
        (
            (
                reachSquared -
                    ArmConstants.SHOULDER_LINK_METERS * ArmConstants.SHOULDER_LINK_METERS -
                    ArmConstants.ELBOW_LINK_METERS * ArmConstants.ELBOW_LINK_METERS
            ) /
                (2f * ArmConstants.SHOULDER_LINK_METERS * ArmConstants.ELBOW_LINK_METERS)
        ).coerceIn(-1f, 1f)
    val elbowRadians = acos(elbowCosine)
    val shoulderRadians =
        atan2(heightMeters, clampedReachMeters) -
            atan2(
                ArmConstants.ELBOW_LINK_METERS * sin(elbowRadians),
                ArmConstants.SHOULDER_LINK_METERS + ArmConstants.ELBOW_LINK_METERS * cos(elbowRadians),
            )
    val wristPitchDegrees = Math.toDegrees(-(shoulderRadians + elbowRadians).toDouble()).toFloat()
    return JointAngles(
        shoulderDegrees =
            servoClamp(
                Math.toDegrees(shoulderRadians.toDouble()).toFloat() + ArmConstants.SHOULDER_SERVO_OFFSET_DEGREES,
            ),
        elbowDegrees =
            servoClamp(
                Math.toDegrees(elbowRadians.toDouble()).toFloat() + ArmConstants.ELBOW_SERVO_OFFSET_DEGREES,
            ),
        wristPitchDegrees = servoClamp(wristPitchDegrees + ArmConstants.WRIST_PITCH_SERVO_OFFSET_DEGREES),
        wristRollDegrees = servoClamp(pose.wristRollDegrees),
        gripperRotationDegrees = servoClamp(pose.clawRotationDegrees),
        gripperCommand = if (gripperOpen) ProtocolConstants.GRIPPER_OPEN else ProtocolConstants.GRIPPER_CLOSED,
    )
}

private fun inferClawRotation(lateralMeters: Float): Float =
    90f + ((lateralMeters / ArmConstants.MAX_LATERAL_METERS).coerceIn(-1f, 1f) * 45f)

private fun servoClamp(value: Float): Int =
    value.roundToInt().coerceIn(ProtocolConstants.MIN_SERVO_ANGLE, ProtocolConstants.MAX_SERVO_ANGLE)
