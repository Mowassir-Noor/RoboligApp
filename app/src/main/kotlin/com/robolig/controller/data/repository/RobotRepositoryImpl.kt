package com.robolig.controller.data.repository

import com.robolig.controller.communication.CommunicationManager
import com.robolig.controller.communication.CommunicationState
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.CameraStreamStatus
import com.robolig.controller.domain.model.CubeDetection
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.ArmController
import com.robolig.controller.domain.repository.DriveController
import com.robolig.controller.domain.repository.MissionController
import com.robolig.controller.domain.repository.RobotRepository
import com.robolig.controller.domain.repository.SystemController
import com.robolig.controller.domain.repository.VideoController
import com.robolig.controller.robot.RobotStateFactory
import com.robolig.controller.utils.ControllerPreferences
import com.robolig.controller.video.DeviceCameraManager
import com.robolig.controller.video.VideoStreamManager
import com.robolig.controller.vision.CubeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RobotRepositoryImpl
    @Inject
    constructor(
        communicationManager: CommunicationManager,
        videoStreamManager: VideoStreamManager,
        deviceCameraManager: DeviceCameraManager,
        cubeDetector: CubeDetector,
        robotStateFactory: RobotStateFactory,
        controllerPreferences: ControllerPreferences,
        override val driveController: DriveController,
        override val armController: ArmController,
        override val missionController: MissionController,
        override val systemController: SystemController,
        override val videoController: VideoController,
        @ApplicationScope applicationScope: CoroutineScope,
    ) : RobotRepository {
        override val robotState: StateFlow<RobotState> =
            combine(
                communicationManager.state,
                videoStreamManager.cameraState,
                deviceCameraManager.frameBytes,
                cubeDetector.detections,
                controllerPreferences.showPacketsOverlay,
                controllerPreferences.useDeviceCamera,
            ) { values: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val communicationState = values[0] as CommunicationState
                @Suppress("UNCHECKED_CAST")
                val mjpegState = values[1] as CameraState
                @Suppress("UNCHECKED_CAST")
                val deviceFrame = values[2] as ByteArray?
                @Suppress("UNCHECKED_CAST")
                val cubeDetections = values[3] as List<CubeDetection>
                @Suppress("UNCHECKED_CAST")
                val showPacketsOverlay = values[4] as Boolean
                @Suppress("UNCHECKED_CAST")
                val useDeviceCamera = values[5] as Boolean
                val mergedCamera = mergeCameraState(
                    mjpegState = mjpegState,
                    deviceFrame = deviceFrame,
                    cubeDetections = cubeDetections,
                    useDeviceCamera = useDeviceCamera,
                )
                robotStateFactory.create(
                    communicationState = communicationState,
                    cameraState = mergedCamera,
                    showPacketsOverlay = showPacketsOverlay,
                    useDeviceCamera = useDeviceCamera,
                    cubeDetectionEnabled = controllerPreferences.cubeDetectionEnabled.value,
                )
            }.stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    robotStateFactory.create(
                        communicationState = CommunicationState(),
                        cameraState = CameraState(),
                    ),
            )
    }

private var deviceFrameSequence: Long = 0L

private fun mergeCameraState(
    mjpegState: CameraState,
    deviceFrame: ByteArray?,
    cubeDetections: List<CubeDetection>,
    useDeviceCamera: Boolean,
): CameraState {
    val withDetections = mjpegState.copy(cubeDetections = cubeDetections)
    if (!useDeviceCamera) {
        return withDetections
    }
    if (deviceFrame == null) {
        return withDetections.copy(
            status = if (mjpegState.frameBytes != null) mjpegState.status else CameraStreamStatus.IDLE,
            lastError = null,
        )
    }
    deviceFrameSequence++
    return withDetections.copy(
        frameBytes = deviceFrame,
        frameSequence = deviceFrameSequence,
        lastFrameAtMs = System.currentTimeMillis(),
        status = CameraStreamStatus.STREAMING,
        lastError = null,
    )
}
