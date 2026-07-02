package com.robolig.controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val robotRepository: RobotRepository,
    ) : ViewModel() {
        val robotState: StateFlow<RobotState> = robotRepository.robotState

        init {
            refreshStatus()
        }

        fun refreshStatus() {
            viewModelScope.launch {
                robotRepository.systemController.refreshStatus()
            }
        }

        fun setRobotMode(mode: RobotMode) {
            robotRepository.systemController.setRobotMode(mode)
        }

        fun updateVideoStreamUrl(url: String) {
            robotRepository.videoController.updateStreamUrl(url)
        }

        fun triggerEmergencyStop() {
            viewModelScope.launch {
                robotRepository.systemController.emergencyStop()
            }
        }
    }
