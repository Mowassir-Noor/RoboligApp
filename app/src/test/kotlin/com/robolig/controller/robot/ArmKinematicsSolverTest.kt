package com.robolig.controller.robot

import com.robolig.controller.core.ArmConstants
import com.robolig.controller.core.ProtocolConstants
import com.robolig.controller.domain.model.ArmPose
import com.robolig.controller.domain.model.ArmState
import com.robolig.controller.domain.model.ControlVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmKinematicsSolverTest {
    private val solver = ArmKinematicsSolver()

    @Test
    fun resolvesHomePresetWithoutWorkspaceClamp() {
        val result = solver.resolve(ArmState(activePreset = ArmPreset.HOME.label))

        assertFalse(result.workspaceClamped)
        assertEquals(ArmConstants.HOME_FORWARD_METERS, result.targetPose.forwardMeters)
        assertEquals(ArmConstants.HOME_HEIGHT_METERS, result.targetPose.heightMeters)
        assertTrue(
            result.jointAngles.shoulderDegrees in ProtocolConstants.MIN_SERVO_ANGLE..ProtocolConstants.MAX_SERVO_ANGLE,
        )
        assertTrue(
            result.jointAngles.elbowDegrees in ProtocolConstants.MIN_SERVO_ANGLE..ProtocolConstants.MAX_SERVO_ANGLE,
        )
    }

    @Test
    fun clampsTargetPoseToWorkspaceLimits() {
        val result =
            solver.resolve(
                ArmState(
                    targetPose =
                        ArmPose(
                            forwardMeters = ArmConstants.MAX_FORWARD_METERS,
                            heightMeters = ArmConstants.MAX_HEIGHT_METERS,
                            lateralMeters = ArmConstants.MAX_LATERAL_METERS,
                        ),
                    planarInput = ControlVector(x = 1f, y = 1f),
                    depthInput = 1f,
                    wristRotationInput = 1f,
                ),
            )

        assertTrue(result.workspaceClamped)
        assertEquals(ArmConstants.MAX_FORWARD_METERS, result.targetPose.forwardMeters)
        assertEquals(ArmConstants.MAX_HEIGHT_METERS, result.targetPose.heightMeters)
        assertEquals(ArmConstants.MAX_LATERAL_METERS, result.targetPose.lateralMeters)
    }

    @Test
    fun forwardKinematicsProducesPoseWithinConfiguredBounds() {
        val pose = solver.forwardKinematics(ArmState().jointAngles)

        assertTrue(pose.forwardMeters in ArmConstants.MIN_FORWARD_METERS..ArmConstants.MAX_FORWARD_METERS)
        assertTrue(pose.heightMeters in ArmConstants.MIN_HEIGHT_METERS..ArmConstants.MAX_HEIGHT_METERS)
        assertTrue(pose.lateralMeters in ArmConstants.MIN_LATERAL_METERS..ArmConstants.MAX_LATERAL_METERS)
    }
}
