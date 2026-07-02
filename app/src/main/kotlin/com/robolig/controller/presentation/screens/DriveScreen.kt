@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.components.ControlJoystick
import com.robolig.controller.presentation.components.JoystickAxisMode
import com.robolig.controller.presentation.components.RailAction
import com.robolig.controller.presentation.components.RailActionStyle
import com.robolig.controller.presentation.components.RobotControlScaffold
import com.robolig.controller.presentation.theme.RoboligTheme

@Composable
@Suppress("LongMethod")
fun DriveScreen(
    robotState: RobotState,
    actions: DriveScreenActions,
) {
    val spacing = RoboligTheme.spacing
    RobotControlScaffold(
        robotState = robotState,
        selectedMode = RobotMode.DRIVE,
        sideActions =
            listOf(
                RailAction(
                    label = "Boost",
                    style = RailActionStyle.TOGGLE,
                    checked = robotState.vehicle.boostEnabled,
                    onClick = { actions.onBoostChanged(!robotState.vehicle.boostEnabled) },
                ),
                RailAction(
                    label = "Brake",
                    style = RailActionStyle.MOMENTARY,
                    onPressedChanged = actions.onBrakeChanged,
                ),
            ),
        frameActions = actions.frameActions,
    ) {
        ModeHeading(
            title = "Driving Mode",
            subtitle = "Translation left, steering right",
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = spacing.section),
        )
        ControlJoystick(
            label = "Translation",
            vector = robotState.vehicle.translationInput,
            onVectorChanged = { translationVector ->
                actions.onDriveInputChanged(translationVector, robotState.vehicle.rotationInput)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = spacing.section * 1.4f, bottom = spacing.section * 1.3f),
        )
        ControlJoystick(
            label = "Steering",
            vector = ControlVector(x = robotState.vehicle.rotationInput),
            axisMode = JoystickAxisMode.HORIZONTAL_ONLY,
            onVectorChanged = { rotationVector ->
                actions.onDriveInputChanged(robotState.vehicle.translationInput, rotationVector.x)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.section * 1.4f, bottom = spacing.section * 1.3f),
        )
    }
}

@Composable
internal fun ModeHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoboligTheme.spacing.compact),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
