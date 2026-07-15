package com.robolig.controller.data.repository

import com.robolig.controller.communication.CommunicationArmController
import com.robolig.controller.communication.CommunicationDriveController
import com.robolig.controller.communication.CommunicationMissionController
import com.robolig.controller.communication.CommunicationSystemController
import com.robolig.controller.core.LogLevel
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.repository.ArmController
import com.robolig.controller.domain.repository.DriveController
import com.robolig.controller.domain.repository.MissionController
import com.robolig.controller.domain.repository.SystemController
import com.robolig.controller.domain.repository.VideoController
import com.robolig.controller.video.VideoStreamManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveControllerImpl
    @Inject
    constructor(
        private val communicationDriveController: CommunicationDriveController,
    ) : DriveController {
        override fun updateInput(
            translationInput: ControlVector,
            rotationInput: Float,
        ) {
            communicationDriveController.updateInput(translationInput = translationInput, rotationInput = rotationInput)
        }

        override fun setBoostEnabled(enabled: Boolean) {
            communicationDriveController.setBoostEnabled(enabled)
        }

        override fun setBrakeActive(active: Boolean) {
            communicationDriveController.setBrakeActive(active)
        }
    }

@Singleton
class ArmControllerImpl
    @Inject
    constructor(
        private val communicationArmController: CommunicationArmController,
    ) : ArmController {
        override fun updatePlanarInput(planarInput: ControlVector) {
            communicationArmController.updatePlanarInput(planarInput)
        }

        override fun updateDepthInput(depthInput: Float) {
            communicationArmController.updateDepthInput(depthInput)
        }

        override fun updateWristRotation(wristRotationInput: Float) {
            communicationArmController.updateWristRotation(wristRotationInput)
        }

        override fun setGripperOpen(open: Boolean) {
            communicationArmController.setGripperOpen(open)
        }

        override fun setPrecisionMode(enabled: Boolean) {
            communicationArmController.setPrecisionMode(enabled)
        }

        override fun activatePreset(presetLabel: String) {
            communicationArmController.activatePreset(presetLabel)
        }

        override fun updateZiplineHeight(height: Float) {
            communicationArmController.updateZiplineHeight(height)
        }
    }

@Singleton
class MissionControllerImpl
    @Inject
    constructor(
        private val communicationMissionController: CommunicationMissionController,
    ) : MissionController {
        override fun setPaused(paused: Boolean) {
            communicationMissionController.setPaused(paused)
        }

        override fun abort() {
            communicationMissionController.abort()
        }
    }

@Singleton
class SystemControllerImpl
    @Inject
    constructor(
        private val communicationSystemController: CommunicationSystemController,
    ) : SystemController {
        override suspend fun refreshStatus() {
            communicationSystemController.refreshStatus()
        }

        override fun setRobotMode(mode: RobotMode) {
            communicationSystemController.setRobotMode(mode)
        }

        override fun updateLogLevel(level: LogLevel) {
            communicationSystemController.updateLogLevel(level)
        }

        override fun updateShowPacketsOverlay(enabled: Boolean) {
            communicationSystemController.updateShowPacketsOverlay(enabled)
        }

        override fun updateUseDeviceCamera(enabled: Boolean) {
            communicationSystemController.updateUseDeviceCamera(enabled)
        }

        override fun updateCubeDetectionEnabled(enabled: Boolean) {
            communicationSystemController.updateCubeDetectionEnabled(enabled)
        }

        override suspend fun emergencyStop() {
            communicationSystemController.emergencyStop()
        }

        override suspend fun resetEmergencyStop() {
            communicationSystemController.resetEmergencyStop()
        }
    }

@Singleton
class VideoControllerImpl
    @Inject
    constructor(
        private val videoStreamManager: VideoStreamManager,
    ) : VideoController {
        override fun updateStreamUrl(url: String) {
            videoStreamManager.updateStreamUrl(url)
        }
    }
