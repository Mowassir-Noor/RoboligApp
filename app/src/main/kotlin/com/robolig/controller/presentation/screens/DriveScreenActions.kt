package com.robolig.controller.presentation.screens

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.presentation.components.RobotControlFrameActions

data class DriveScreenActions(
    val frameActions: RobotControlFrameActions,
    val onDriveInputChanged: (ControlVector, Float) -> Unit,
    val onBoostChanged: (Boolean) -> Unit,
    val onBrakeChanged: (Boolean) -> Unit,
)
