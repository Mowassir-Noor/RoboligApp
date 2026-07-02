package com.robolig.controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AutoViewModel
    @Inject
    constructor(
        private val robotRepository: RobotRepository,
    ) : ViewModel() {
        val robotState: StateFlow<RobotState> = robotRepository.robotState

        fun setMissionPaused(paused: Boolean) {
            robotRepository.missionController.setPaused(paused)
        }

        fun abortMission() {
            robotRepository.missionController.abort()
        }
    }
