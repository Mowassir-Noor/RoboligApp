@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.presentation.theme.RoboligTheme

@Composable
@Suppress("LongMethod")
fun ControlJoystick(
    label: String,
    vector: ControlVector,
    onVectorChanged: (ControlVector) -> Unit,
    modifier: Modifier = Modifier,
    axisMode: JoystickAxisMode = JoystickAxisMode.TWO_DIMENSIONAL,
    deadzone: Float = 0.12f,
) {
    val spacing = RoboligTheme.spacing
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val animatedX by animateFloatAsState(
        targetValue = vector.x,
        animationSpec = spring(stiffness = 420f),
        label = "joystick-x",
    )
    val animatedY by animateFloatAsState(
        targetValue = vector.y,
        animationSpec = spring(stiffness = 420f),
        label = "joystick-y",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier =
                Modifier
                    .size(spacing.joystickSize)
                    .onSizeChanged { layoutSize = it }
                    .pointerInput(axisMode, deadzone) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            onVectorChanged(
                                resolveVector(
                                    position = down.position,
                                    size = layoutSize,
                                    axisMode = axisMode,
                                    deadzone = deadzone,
                                ),
                            )
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                onVectorChanged(
                                    resolveVector(
                                        position = change.position,
                                        size = layoutSize,
                                        axisMode = axisMode,
                                        deadzone = deadzone,
                                    ),
                                )
                                change.consume()
                            } while (event.changes.any { it.id == pointerId && it.pressed })
                            onVectorChanged(ControlVector())
                        }
                    },
        ) {
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val outline = MaterialTheme.colorScheme.outline
            val primary = MaterialTheme.colorScheme.primary

            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
            ) {
                val center = Offset(x = size.width / 2f, y = size.height / 2f)
                val outerRadius = size.minDimension / 2f * 0.84f
                val knobRadius = outerRadius * 0.28f
                drawCircle(
                    color = surfaceVariant,
                    radius = outerRadius,
                    center = center,
                )
                drawCircle(
                    color = outline,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 3f),
                )
                drawLine(
                    color = outline,
                    start = Offset(center.x - outerRadius, center.y),
                    end = Offset(center.x + outerRadius, center.y),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = outline,
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y + outerRadius),
                    strokeWidth = 1f,
                )

                val knobOffset =
                    Offset(
                        x = center.x + animatedX * outerRadius * 0.62f,
                        y = center.y - animatedY * outerRadius * 0.62f,
                    )
                drawCircle(
                    color = primary.copy(alpha = 0.22f),
                    radius = knobRadius * 1.5f,
                    center = knobOffset,
                )
                drawCircle(
                    color = primary,
                    radius = knobRadius,
                    center = knobOffset,
                )
            }
        }
    }
}

private fun resolveVector(
    position: Offset,
    size: IntSize,
    axisMode: JoystickAxisMode,
    deadzone: Float,
): ControlVector {
    if (size == IntSize.Zero) {
        return ControlVector()
    }

    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = minOf(size.width, size.height) / 2f
    val normalizedX = ((position.x - centerX) / radius).coerceIn(-1f, 1f)
    val normalizedY = ((centerY - position.y) / radius).coerceIn(-1f, 1f)
    val resolvedVector =
        when (axisMode) {
            JoystickAxisMode.TWO_DIMENSIONAL -> ControlVector(x = normalizedX, y = normalizedY)
            JoystickAxisMode.VERTICAL_ONLY -> ControlVector(x = 0f, y = normalizedY)
            JoystickAxisMode.HORIZONTAL_ONLY -> ControlVector(x = normalizedX, y = 0f)
        }.clamp()

    return if (resolvedVector.magnitude < deadzone) {
        ControlVector()
    } else {
        resolvedVector
    }
}
