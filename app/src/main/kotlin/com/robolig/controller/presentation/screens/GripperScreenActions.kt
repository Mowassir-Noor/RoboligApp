package com.robolig.controller.presentation.screens

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.presentation.components.RobotControlFrameActions

data class GripperScreenActions(
    val frameActions: RobotControlFrameActions,
    val onDriveInputChanged: (ControlVector, Float) -> Unit,
    val onPlanarInputChanged: (ControlVector) -> Unit,
    val onDepthInputChanged: (Float) -> Unit,
    val onWristRotationChanged: (Float) -> Unit,
    val onGripperOpenChanged: (Boolean) -> Unit,
    val onPrecisionModeChanged: (Boolean) -> Unit,
    val onPresetActivated: (String) -> Unit,
)
