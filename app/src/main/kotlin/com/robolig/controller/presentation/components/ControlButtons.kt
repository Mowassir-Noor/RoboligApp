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
                .heightIn(min = 56.dp)
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
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
    modifier: Modifier = Modifier,
) {
    RoboligActionButton(
        label = "EMERGENCY",
        modifier = modifier,
        accent = true,
        onClick = onClick,
        description = "Emergency stop",
        emphasis = true,
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
) {
    val backgroundColor =
        when {
            accent && emphasis -> MaterialTheme.colorScheme.primary
            accent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when {
            accent && emphasis -> MaterialTheme.colorScheme.onPrimary
            accent -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
