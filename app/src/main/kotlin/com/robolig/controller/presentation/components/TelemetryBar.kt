@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.theme.RoboligTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryBar(
    robotState: RobotState,
    selectedMode: RobotMode,
    onModeSelected: (RobotMode) -> Unit,
    onEmergencyStop: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = RoboligTheme.spacing
    val barHorizontalPadding = spacing.section
    val barVerticalPadding = spacing.compact + 4.dp
    val clusterSpacing = spacing.item
    val itemSpacing = spacing.compact
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = barHorizontalPadding, vertical = barVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(clusterSpacing),
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(clusterSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                TelemetryItem(label = "Battery", value = robotState.battery.percentage.asDisplayValue("%"))
                TelemetryItem(label = "Connection", value = robotState.connectionState.displayName)
                TelemetryItem(label = "Signal", value = robotState.signal.strengthPercent.asDisplayValue("%"))
                TelemetryItem(label = "Speed", value = robotState.speedMetersPerSecond.asSpeedLabel())
                TelemetryItem(
                    label = "Task",
                    value = robotState.mission.currentTask,
                    modifier = Modifier.widthIn(min = 110.dp, max = 180.dp),
                )
                TelemetryItem(label = "FPS", value = robotState.camera.framesPerSecond.asDisplayValue())
                TelemetryItem(label = "Latency", value = robotState.telemetry.latencyMs.asDisplayValue(" ms"))
            }

            FlowRow(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                RobotMode.entries.forEach { mode ->
                    RoboligModeButton(
                        label = mode.displayName,
                        selected = mode == selectedMode,
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.widthIn(min = 74.dp),
                    )
                }
                WarningChip(
                    message = robotState.warnings.firstOrNull(),
                    modifier = Modifier.widthIn(max = 210.dp),
                )
                EmergencyStopButton(
                    onClick = onEmergencyStop,
                    compact = true,
                )
                CompactTextButton(label = "Settings", onClick = onOpenSettings)
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
        modifier = modifier.widthIn(min = 54.dp).wrapContentWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
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
        modifier =
            Modifier
                .widthIn(min = 74.dp)
                .heightIn(min = 40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
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
