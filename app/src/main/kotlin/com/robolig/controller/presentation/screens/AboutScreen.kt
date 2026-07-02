@file:Suppress("FunctionName")

package com.robolig.controller.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robolig.controller.core.AppConstants
import com.robolig.controller.presentation.components.RoboligModeButton
import com.robolig.controller.presentation.theme.RoboligTheme

@Composable
fun AboutScreen(onBackToDrive: () -> Unit) {
    val spacing = RoboligTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.page, vertical = spacing.page),
        verticalArrangement = Arrangement.spacedBy(spacing.section),
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = AppConstants.APPLICATION_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Protocol ${AppConstants.PROTOCOL_VERSION}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text =
                        "Landscape tablet controller for Teknofest Robolig. The app maintains a " +
                            "single immutable RobotState, keeps USB control independent from MJPEG " +
                            "video, validates every inbound packet, and prioritizes emergency stop " +
                            "above every other command.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text =
                        "Architecture: UI -> ViewModel -> Repository -> Communication -> Hardware " +
                            "Managers -> USB / Video -> Robot",
                    style = MaterialTheme.typography.bodyLarge,
                )
                RoboligModeButton(label = "Back", selected = false, onClick = onBackToDrive)
            }
        }
    }
}
