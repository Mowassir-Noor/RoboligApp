package com.robolig.controller.presentation.screens

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.presentation.components.RobotControlFrameActions

data class ZiplineScreenActions(
    val frameActions: RobotControlFrameActions,
    val onDriveInputChanged: (ControlVector, Float) -> Unit,
    val onPlanarInputChanged: (ControlVector) -> Unit,
    val onGripperOpenChanged: (Boolean) -> Unit,
    val onZiplineHeightChanged: (Float) -> Unit,
)
