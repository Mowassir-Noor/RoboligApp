package com.robolig.controller.presentation.screens

import com.robolig.controller.presentation.components.RobotControlFrameActions

data class AutoScreenActions(
    val frameActions: RobotControlFrameActions,
    val onMissionPausedChanged: (Boolean) -> Unit,
    val onAbortMission: () -> Unit,
)
