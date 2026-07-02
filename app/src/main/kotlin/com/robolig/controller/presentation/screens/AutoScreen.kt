@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.components.RailAction
import com.robolig.controller.presentation.components.RailActionStyle
import com.robolig.controller.presentation.components.RobotControlScaffold

@Composable
@Suppress("LongMethod")
fun AutoScreen(
    robotState: RobotState,
    actions: AutoScreenActions,
) {
    RobotControlScaffold(
        robotState = robotState,
        selectedMode = RobotMode.AUTO,
        sideActions =
            listOf(
                RailAction(
                    label = if (robotState.mission.isPaused) "Resume" else "Pause",
                    style = RailActionStyle.TOGGLE,
                    checked = robotState.mission.isPaused,
                    onClick = { actions.onMissionPausedChanged(!robotState.mission.isPaused) },
                ),
                RailAction(label = "Abort", onClick = actions.onAbortMission),
            ),
        frameActions = actions.frameActions,
    ) {
        Surface(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.44f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Autonomous Mission",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                MetricRow(label = "Mission", value = robotState.mission.missionName)
                MetricRow(label = "Task", value = robotState.mission.currentTask)
                MetricRow(
                    label = "Waypoint",
                    value =
                        if (robotState.mission.waypointCount == 0) {
                            robotState.mission.waypointLabel
                        } else {
                            "${robotState.mission.waypointIndex}/${robotState.mission.waypointCount} " +
                                robotState.mission.waypointLabel
                        },
                )
                MetricRow(label = "Robot State", value = robotState.connectionState.displayName)
                LinearProgressIndicator(
                    progress = { robotState.mission.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${robotState.mission.progressPercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
