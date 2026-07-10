package com.robolig.controller.robot

import com.robolig.controller.communication.CommunicationState
import com.robolig.controller.domain.model.BatteryState
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.CameraStreamStatus
import com.robolig.controller.domain.model.ConnectionState
import com.robolig.controller.domain.model.RobotMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotStateFactoryTest {
    private val robotStateFactory = RobotStateFactory()

    @Test
    fun mergesCommunicationAndVideoStateIntoRobotState() {
        val communicationState =
            CommunicationState(
                connectionState = ConnectionState.CONNECTED,
                currentMode = RobotMode.GRIPPER,
                battery = BatteryState(percentage = 87, isLow = false),
                warnings = listOf("USB permission is pending"),
            )
        val cameraState =
            CameraState(
                streamUrl = "http://10.0.0.5:8080/stream",
                status = CameraStreamStatus.CONFIGURED,
            )

        val robotState =
            robotStateFactory.create(
                communicationState = communicationState,
                cameraState = cameraState,
            )

        assertEquals(ConnectionState.CONNECTED, robotState.connectionState)
        assertEquals(RobotMode.GRIPPER, robotState.currentMode)
        assertEquals(87, robotState.battery.percentage)
        assertEquals("http://10.0.0.5:8080/stream", robotState.camera.streamUrl)
        assertTrue(robotState.warnings.contains("USB permission is pending"))
    }

    @Test
    fun addsVideoWarningWhenStreamIsNotConfigured() {
        val robotState =
            robotStateFactory.create(
                communicationState = CommunicationState(),
                cameraState = CameraState(),
            )

        assertTrue(robotState.warnings.contains("Video stream URL is not configured"))
    }
}
