@file:Suppress("FunctionName")

package com.robolig.controller.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.CameraStreamStatus
import com.robolig.controller.domain.model.CubeDetection

@Composable
fun CameraView(
    cameraState: CameraState,
    modifier: Modifier = Modifier,
) {
    val frameBitmap =
        remember(cameraState.frameSequence) {
            cameraState.frameBytes?.let { frameBytes ->
                BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)?.asImageBitmap()
            }
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            if (frameBitmap != null) {
                Image(
                    bitmap = frameBitmap,
                    contentDescription = "Robot camera feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (cameraState.cubeDetections.isNotEmpty()) {
                    CubeDetectionOverlay(
                        detections = cameraState.cubeDetections,
                        imageWidth = frameBitmap.width,
                        imageHeight = frameBitmap.height,
                    )
                }
            } else {
                CameraPlaceholder(cameraState = cameraState)
            }

            Box(
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                CameraOverlay(cameraState = cameraState)
            }
        }
    }
}

@Composable
private fun CameraPlaceholder(cameraState: CameraState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (cameraState.status == CameraStreamStatus.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text =
                when (cameraState.status) {
                    CameraStreamStatus.IDLE -> "Camera URL not configured"
                    CameraStreamStatus.CONFIGURED -> "Awaiting MJPEG stream"
                    CameraStreamStatus.CONNECTING -> "Connecting to MJPEG stream"
                    CameraStreamStatus.STREAMING -> "Video warming up"
                    CameraStreamStatus.ERROR -> cameraState.lastError ?: "Video stream error"
                },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (cameraState.streamUrl.isNotBlank()) {
            Text(
                text = cameraState.streamUrl,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CameraOverlay(cameraState: CameraState) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        CameraBadge(label = cameraState.status.displayName)
        CameraBadge(label = "${cameraState.framesPerSecond} FPS")
        CameraBadge(label = "${cameraState.latencyMs ?: 0} ms")
        val detections = cameraState.cubeDetections
        if (detections.isNotEmpty()) {
            CameraBadge(label = "${detections.size} cube${if (detections.size == 1) "" else "s"}")
        }
        if (cameraState.reconnectAttempts > 0) {
            CameraBadge(label = "Retry ${cameraState.reconnectAttempts}")
        }
    }
}

@Composable
private fun CubeDetectionOverlay(
    detections: List<CubeDetection>,
    imageWidth: Int,
    imageHeight: Int,
) {
    if (imageWidth <= 0 || imageHeight <= 0) return
    val boxColor = MaterialTheme.colorScheme.tertiary
    val crosshairColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = Modifier.fillMaxSize()) {
        detections.forEach { detection ->
            val left = (detection.x.toFloat() / imageWidth) * size.width
            val top = (detection.y.toFloat() / imageHeight) * size.height
            val right = (detection.right.toFloat() / imageWidth) * size.width
            val bottom = (detection.bottom.toFloat() / imageHeight) * size.height
            drawRect(
                color = boxColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 4f, pathEffect = PathEffect.cornerPathEffect(8f)),
            )
            val cx = (detection.centroidX / imageWidth) * size.width
            val cy = (detection.centroidY / imageHeight) * size.height
            val arm = 14f
            drawLine(
                color = crosshairColor,
                start = androidx.compose.ui.geometry.Offset(cx - arm, cy),
                end = androidx.compose.ui.geometry.Offset(cx + arm, cy),
                strokeWidth = 3f,
            )
            drawLine(
                color = crosshairColor,
                start = androidx.compose.ui.geometry.Offset(cx, cy - arm),
                end = androidx.compose.ui.geometry.Offset(cx, cy + arm),
                strokeWidth = 3f,
            )
            drawCircle(
                color = crosshairColor.copy(alpha = 0.35f),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                radius = 5f,
            )
        }
    }
}

@Composable
private fun CameraBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
