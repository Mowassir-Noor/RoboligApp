@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robolig.controller.core.asDisplayValue
import com.robolig.controller.core.asSpeedLabel
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.theme.RoboligTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryBar(
    robotState: RobotState,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = RoboligTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.page, vertical = spacing.panel),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.section),
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.section),
                verticalArrangement = Arrangement.spacedBy(spacing.compact),
            ) {
                TelemetryItem(label = "Battery", value = robotState.battery.percentage.asDisplayValue("%"))
                TelemetryItem(label = "Connection", value = robotState.connectionState.displayName)
                TelemetryItem(label = "Signal", value = robotState.signal.strengthPercent.asDisplayValue("%"))
                TelemetryItem(label = "Mode", value = robotState.currentMode.displayName)
                TelemetryItem(label = "Speed", value = robotState.speedMetersPerSecond.asSpeedLabel())
                TelemetryItem(
                    label = "Task",
                    value = robotState.mission.currentTask,
                    modifier = Modifier.widthIn(min = 132.dp, max = 220.dp),
                )
                TelemetryItem(label = "FPS", value = robotState.camera.framesPerSecond.asDisplayValue())
                TelemetryItem(label = "Latency", value = robotState.telemetry.latencyMs.asDisplayValue(" ms"))
            }

            WarningChip(
                message = robotState.warnings.firstOrNull(),
                modifier = Modifier.widthIn(max = 260.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.compact)) {
                CompactTextButton(label = "Settings", onClick = onOpenSettings)
                CompactTextButton(label = "About", onClick = onOpenAbout)
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.wrapContentWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WarningChip(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrBlank()) {
        return
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactTextButton(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 88.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
