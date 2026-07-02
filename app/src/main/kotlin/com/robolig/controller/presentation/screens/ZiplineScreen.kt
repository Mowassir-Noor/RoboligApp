@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.presentation.components.ControlJoystick
import com.robolig.controller.presentation.components.RailAction
import com.robolig.controller.presentation.components.RobotControlScaffold
import com.robolig.controller.presentation.theme.RoboligTheme

@Composable
@Suppress("LongMethod")
fun ZiplineScreen(
    robotState: RobotState,
    actions: ZiplineScreenActions,
) {
    val spacing = RoboligTheme.spacing
    val railClearance = spacing.railWidth + spacing.section * 1.8f
    RobotControlScaffold(
        robotState = robotState,
        selectedMode = RobotMode.ZIPLINE,
        sideActions =
            listOf(
                RailAction(
                    label = "Grip",
                    checked = !robotState.arm.gripperOpen,
                    onClick = { actions.onGripperOpenChanged(false) },
                ),
                RailAction(
                    label = "Release",
                    checked = robotState.arm.gripperOpen,
                    onClick = { actions.onGripperOpenChanged(true) },
                ),
            ),
        frameActions = actions.frameActions,
    ) {
        ModeHeading(
            title = "Zipline Mode",
            subtitle = "Drive left, manipulator right, height slider",
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = spacing.section),
        )
        ControlJoystick(
            label = "Drive",
            vector = robotState.vehicle.translationInput,
            onVectorChanged = { translationVector ->
                actions.onDriveInputChanged(translationVector, 0f)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = spacing.section * 1.4f, bottom = spacing.section * 1.3f),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = railClearance, bottom = spacing.section * 1.3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.section),
        ) {
            VerticalHeightSlider(
                value = robotState.arm.ziplineHeight,
                onValueChanged = actions.onZiplineHeightChanged,
            )
            ControlJoystick(
                label = "Arm",
                vector = robotState.arm.planarInput,
                onVectorChanged = actions.onPlanarInputChanged,
            )
        }
    }
}

@Composable
private fun VerticalHeightSlider(
    value: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = RoboligTheme.spacing
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.item),
        ) {
            Text(
                text = "Height",
                style = MaterialTheme.typography.labelMedium,
            )
            VerticalSliderTrack(
                value = value,
                onValueChange = onValueChanged,
                modifier =
                    Modifier
                        .height(spacing.joystickSize + spacing.section * 2.2f)
                        .width(52.dp),
            )
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100f).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VerticalSliderTrack(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val thumbRadiusPx = with(density) { 14.dp.toPx() }
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 420f),
        label = "zipline-height",
    )
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier =
            modifier
                .onSizeChanged { layoutSize = it }
                .semantics {
                    contentDescription = "Zipline height"
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                }
                .pointerInput(thumbRadiusPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        onValueChangeState.value(resolveVerticalSliderValue(down.position.y, layoutSize, thumbRadiusPx))
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            onValueChangeState.value(
                                resolveVerticalSliderValue(
                                    positionY = change.position.y,
                                    layoutSize = layoutSize,
                                    thumbRadiusPx = thumbRadiusPx,
                                ),
                            )
                            change.consume()
                        } while (event.changes.any { it.id == pointerId && it.pressed })
                    }
                },
    ) {
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val outline = MaterialTheme.colorScheme.outline
        val primary = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.matchParentSize()) {
            val trackWidth = 10.dp.toPx()
            val centerX = size.width / 2f
            val trackTop = thumbRadiusPx
            val trackBottom = size.height - thumbRadiusPx
            val trackHeight = trackBottom - trackTop
            val thumbCenterY = trackBottom - (trackHeight * animatedValue.coerceIn(0f, 1f))
            val cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)

            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(centerX - trackWidth / 2f, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(centerX - trackWidth / 2f, trackTop),
                size = Size(trackWidth, trackHeight),
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = primary.copy(alpha = 0.28f),
                topLeft = Offset(centerX - trackWidth / 2f, thumbCenterY),
                size = Size(trackWidth, trackBottom - thumbCenterY),
                cornerRadius = cornerRadius,
            )
            drawCircle(
                color = primary.copy(alpha = 0.22f),
                radius = thumbRadiusPx * 1.55f,
                center = Offset(centerX, thumbCenterY),
            )
            drawCircle(
                color = primary,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbCenterY),
            )
        }
    }
}

private fun resolveVerticalSliderValue(
    positionY: Float,
    layoutSize: IntSize,
    thumbRadiusPx: Float,
): Float {
    if (layoutSize.height <= 0) {
        return 0f
    }

    val trackTop = thumbRadiusPx
    val trackBottom = layoutSize.height - thumbRadiusPx
    val clampedY = positionY.coerceIn(trackTop, trackBottom)
    val normalizedY = (clampedY - trackTop) / (trackBottom - trackTop)
    return 1f - normalizedY
}
