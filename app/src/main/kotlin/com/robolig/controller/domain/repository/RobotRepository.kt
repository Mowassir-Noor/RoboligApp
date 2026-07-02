package com.robolig.controller.domain.repository

import com.robolig.controller.domain.model.RobotState
import kotlinx.coroutines.flow.StateFlow

/**
 * Single repository entry point for the controller application.
 */
interface RobotRepository {
    val robotState: StateFlow<RobotState>
    val driveController: DriveController
    val armController: ArmController
    val missionController: MissionController
    val systemController: SystemController
    val videoController: VideoController
}
