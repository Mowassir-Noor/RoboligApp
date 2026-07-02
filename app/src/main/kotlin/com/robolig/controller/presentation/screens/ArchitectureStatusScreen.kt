@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robolig.controller.core.AppConstants
import com.robolig.controller.core.asDisplayValue
import com.robolig.controller.core.asFallback
import com.robolig.controller.core.asSpeedLabel
import com.robolig.controller.domain.model.RobotState

@Composable
fun ArchitectureStatusScreen(
    robotState: RobotState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = AppConstants.APPLICATION_NAME,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Project architecture online",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        ControlGraphCard(robotState = robotState)
        RobotStateCard(robotState = robotState)
        VideoCard(robotState = robotState)
        MessageCard(title = "Warnings", messages = robotState.warnings)
        MessageCard(title = "Errors", messages = robotState.errors)
    }
}

@Composable
private fun ControlGraphCard(robotState: RobotState) {
    StatusCard(title = "Control Graph") {
        StatusRow(label = "Connection", value = robotState.connectionState.displayName)
        StatusRow(label = "Mode", value = robotState.currentMode.displayName)
        StatusRow(label = "Speed", value = robotState.speedMetersPerSecond.asSpeedLabel())
        StatusRow(label = "Task", value = robotState.mission.currentTask)
    }
}

@Composable
private fun RobotStateCard(robotState: RobotState) {
    StatusCard(title = "Robot State") {
        StatusRow(label = "Battery", value = robotState.battery.percentage.asDisplayValue("%"))
        StatusRow(label = "Signal", value = robotState.signal.strengthPercent.asDisplayValue("%"))
        StatusRow(label = "Mission Progress", value = robotState.mission.progressPercent.asDisplayValue("%"))
        StatusRow(label = "Latency", value = robotState.telemetry.latencyMs.asDisplayValue(" ms"))
    }
}

@Composable
private fun VideoCard(robotState: RobotState) {
    StatusCard(title = "Video") {
        StatusRow(label = "Status", value = robotState.camera.status.displayName)
        StatusRow(label = "Stream URL", value = robotState.camera.streamUrl.asFallback())
        StatusRow(label = "FPS", value = robotState.camera.framesPerSecond.asDisplayValue())
        StatusRow(
            label = "MJPEG",
            value = if (robotState.camera.isMjpegCompatible) "Supported" else "Unavailable",
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    messages: List<String>,
) {
    StatusCard(title = title) {
        Text(
            text = if (messages.isEmpty()) "None" else messages.joinToString(separator = "\n"),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            content()
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
