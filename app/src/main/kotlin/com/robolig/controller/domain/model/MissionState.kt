package com.robolig.controller.domain.model

data class MissionState(
    val missionName: String = "Manual Control",
    val currentTask: String = "Awaiting operator input",
    val progressPercent: Int = 0,
    val waypointLabel: String = "N/A",
    val waypointIndex: Int = 0,
    val waypointCount: Int = 0,
    val isPaused: Boolean = false,
)
