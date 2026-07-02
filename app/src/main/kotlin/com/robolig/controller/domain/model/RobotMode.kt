package com.robolig.controller.domain.model

enum class RobotMode(
    val displayName: String,
    val route: String,
) {
    DRIVE("Drive", "drive"),
    GRIPPER("Gripper", "gripper"),
    ZIPLINE("Zipline", "zipline"),
    AUTO("Auto", "auto"),
}
