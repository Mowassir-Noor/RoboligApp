@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.theme.RoboligTheme

enum class RailActionStyle {
    STANDARD,
    TOGGLE,
    MOMENTARY,
}

data class RailAction(
    val label: String,
    val style: RailActionStyle = RailActionStyle.STANDARD,
    val checked: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val onPressedChanged: ((Boolean) -> Unit)? = null,
)

data class RobotControlFrameActions(
    val onModeSelected: (RobotMode) -> Unit,
    val onEmergencyStop: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenAbout: () -> Unit,
)

@Composable
@Suppress("LongMethod")
fun RobotControlScaffold(
    robotState: RobotState,
    selectedMode: RobotMode,
    sideActions: List<RailAction>,
    frameActions: RobotControlFrameActions,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    val spacing = RoboligTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(horizontal = spacing.page, vertical = spacing.page),
        verticalArrangement = Arrangement.spacedBy(spacing.section),
    ) {
        TelemetryBar(
            robotState = robotState,
            onOpenSettings = frameActions.onOpenSettings,
            onOpenAbout = frameActions.onOpenAbout,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            CameraView(
                cameraState = robotState.camera,
                modifier = Modifier.fillMaxSize(),
            )
            overlayContent()

            RailContainer(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = spacing.section, top = spacing.section, bottom = spacing.section)
                        .width(spacing.railWidth),
            ) {
                EmergencyStopButton(
                    onClick = frameActions.onEmergencyStop,
                    modifier = Modifier.fillMaxWidth(),
                )
                sideActions.forEach { action ->
                    when (action.style) {
                        RailActionStyle.STANDARD ->
                            RoboligModeButton(
                                label = action.label,
                                selected = action.checked,
                                onClick = action.onClick ?: {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                        RailActionStyle.TOGGLE ->
                            RoboligToggleButton(
                                label = action.label,
                                checked = action.checked,
                                onToggle = { _ -> action.onClick?.invoke() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        RailActionStyle.MOMENTARY ->
                            RoboligMomentaryButton(
                                label = action.label,
                                onPressedChanged = action.onPressedChanged ?: {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                    }
                }
            }

            RailContainer(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = spacing.section, top = spacing.section, bottom = spacing.section)
                        .width(spacing.railWidth),
            ) {
                RobotMode.entries.forEach { mode ->
                    RoboligModeButton(
                        label = mode.displayName,
                        selected = mode == selectedMode,
                        onClick = { frameActions.onModeSelected(mode) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RailContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
