package com.robolig.controller.data.repository

import com.robolig.controller.communication.CommunicationManager
import com.robolig.controller.communication.CommunicationState
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.ArmController
import com.robolig.controller.domain.repository.DriveController
import com.robolig.controller.domain.repository.MissionController
import com.robolig.controller.domain.repository.RobotRepository
import com.robolig.controller.domain.repository.SystemController
import com.robolig.controller.domain.repository.VideoController
import com.robolig.controller.robot.RobotStateFactory
import com.robolig.controller.video.VideoStreamManager
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
        robotStateFactory: RobotStateFactory,
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
            ) { communicationState: CommunicationState, cameraState: CameraState ->
                robotStateFactory.create(
                    communicationState = communicationState,
                    cameraState = cameraState,
                )
            }.stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = robotStateFactory.create(CommunicationState(), CameraState()),
            )
    }
