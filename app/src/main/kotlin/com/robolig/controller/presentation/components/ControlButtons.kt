@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RoboligModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoboligActionButton(
        label = label,
        modifier = modifier,
        accent = selected,
        onClick = onClick,
        description = "$label mode",
    )
}

@Composable
fun RoboligToggleButton(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoboligActionButton(
        label = label,
        modifier = modifier,
        accent = checked,
        onClick = { onToggle(!checked) },
        description = label,
    )
}

@Composable
fun RoboligMomentaryButton(
    label: String,
    onPressedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .heightIn(min = 42.dp)
                .pointerInput(label) {
                    detectTapGestures(
                        onPress = {
                            onPressedChanged(true)
                            tryAwaitRelease()
                            onPressedChanged(false)
                        },
                    )
                }
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EmergencyStopButton(
    onClick: () -> Unit,
    onReset: () -> Unit,
    armed: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    RoboligActionButton(
        label = when {
            armed && compact -> "RESET"
            armed -> "E-STOP ARMED"
            compact -> "E-STOP"
            else -> "EMERGENCY"
        },
        modifier = modifier,
        accent = true,
        onClick = if (armed) onReset else onClick,
        description = if (armed) "Reset emergency stop" else "Emergency stop",
        emphasis = true,
        compact = compact,
        filled = armed,
    )
}

@Composable
private fun RoboligActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    description: String = label,
    emphasis: Boolean = false,
    compact: Boolean = true,
    filled: Boolean = false,
) {
    val backgroundColor =
        when {
            accent && emphasis && filled -> MaterialTheme.colorScheme.error
            accent && emphasis -> MaterialTheme.colorScheme.primary
            accent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when {
            accent && emphasis && filled -> MaterialTheme.colorScheme.onError
            accent && emphasis -> MaterialTheme.colorScheme.onPrimary
            accent -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
    val minimumHeight = if (compact) 42.dp else 56.dp
    val cornerRadius = if (compact) 14.dp else 18.dp
    val horizontalPadding = if (compact) 10.dp else 10.dp
    val verticalPadding = if (compact) 8.dp else 12.dp
    val textStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge

    Surface(
        modifier =
            modifier
                .heightIn(min = minimumHeight)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = textStyle,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
