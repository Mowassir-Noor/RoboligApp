@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.components.ControlJoystick
import com.robolig.controller.presentation.components.RailAction
import com.robolig.controller.presentation.components.RailActionStyle
import com.robolig.controller.presentation.components.RoboligModeButton
import com.robolig.controller.presentation.components.RobotControlScaffold
import com.robolig.controller.presentation.theme.RoboligTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongMethod")
fun GripperScreen(
    robotState: RobotState,
    actions: GripperScreenActions,
) {
    val spacing = RoboligTheme.spacing
    val driveJoystickPadding = spacing.section * 1.4f
    RobotControlScaffold(
        robotState = robotState,
        selectedMode = RobotMode.GRIPPER,
        sideActions =
            listOf(
                RailAction(
                    label = "Precision",
                    style = RailActionStyle.TOGGLE,
                    checked = robotState.arm.precisionModeEnabled,
                    onClick = { actions.onPrecisionModeChanged(!robotState.arm.precisionModeEnabled) },
                ),
            ),
        frameActions = actions.frameActions,
        rightRailContent = {
            RoboligModeButton(
                label = "Open",
                selected = robotState.arm.gripperOpen,
                onClick = { actions.onGripperOpenChanged(true) },
                modifier = Modifier.fillMaxWidth(),
            )
            RoboligModeButton(
                label = "Close",
                selected = !robotState.arm.gripperOpen,
                onClick = { actions.onGripperOpenChanged(false) },
                modifier = Modifier.fillMaxWidth(),
            )
            RoboligModeButton(
                label = "Home",
                selected = robotState.arm.activePreset == "Home",
                onClick = { actions.onPresetActivated("Home") },
                modifier = Modifier.fillMaxWidth(),
            )
            RoboligModeButton(
                label = "Pick",
                selected = robotState.arm.activePreset == "Pick",
                onClick = { actions.onPresetActivated("Pick") },
                modifier = Modifier.fillMaxWidth(),
            )
            RoboligModeButton(
                label = "Place",
                selected = robotState.arm.activePreset == "Place",
                onClick = { actions.onPresetActivated("Place") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        val railClearance = spacing.railWidth + spacing.section * 1.8f
        ModeHeading(
            title = "Gripper Mode",
            subtitle = "Arm left stack, drive lower-left, depth and wrist right",
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = spacing.section),
        )
        Surface(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = railClearance, top = spacing.section * 3.4f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "ARM: Up/Down + Left/Right", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "WRIST: Forward/Back + Rotate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = driveJoystickPadding, bottom = spacing.section * 1.3f),
            verticalArrangement = Arrangement.spacedBy(spacing.compact),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ControlJoystick(
                label = "Arm XY",
                vector = robotState.arm.planarInput,
                onVectorChanged = actions.onPlanarInputChanged,
            )
            ControlJoystick(
                label = "Drive",
                vector = robotState.vehicle.translationInput,
                onVectorChanged = { translationVector ->
                    actions.onDriveInputChanged(translationVector, 0f)
                },
            )
        }
        ControlJoystick(
            label = "Depth / Wrist",
            vector =
                ControlVector(
                    x = robotState.arm.wristRotationInput,
                    y = robotState.arm.depthInput,
                ),
            onVectorChanged = { controlVector ->
                actions.onDepthInputChanged(controlVector.y)
                actions.onWristRotationChanged(controlVector.x)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = driveJoystickPadding, bottom = spacing.section * 1.3f),
        )
    }
}
