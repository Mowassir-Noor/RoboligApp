package com.robolig.controller.di

import com.robolig.controller.data.repository.ArmControllerImpl
import com.robolig.controller.data.repository.DriveControllerImpl
import com.robolig.controller.data.repository.MissionControllerImpl
import com.robolig.controller.data.repository.RobotRepositoryImpl
import com.robolig.controller.data.repository.SystemControllerImpl
import com.robolig.controller.data.repository.VideoControllerImpl
import com.robolig.controller.domain.repository.ArmController
import com.robolig.controller.domain.repository.DriveController
import com.robolig.controller.domain.repository.MissionController
import com.robolig.controller.domain.repository.RobotRepository
import com.robolig.controller.domain.repository.SystemController
import com.robolig.controller.domain.repository.VideoController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRobotRepository(robotRepositoryImpl: RobotRepositoryImpl): RobotRepository

    @Binds
    abstract fun bindDriveController(driveControllerImpl: DriveControllerImpl): DriveController

    @Binds
    abstract fun bindArmController(armControllerImpl: ArmControllerImpl): ArmController

    @Binds
    abstract fun bindMissionController(missionControllerImpl: MissionControllerImpl): MissionController

    @Binds
    abstract fun bindSystemController(systemControllerImpl: SystemControllerImpl): SystemController

    @Binds
    abstract fun bindVideoController(videoControllerImpl: VideoControllerImpl): VideoController
}
