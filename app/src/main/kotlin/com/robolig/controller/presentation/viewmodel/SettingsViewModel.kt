package com.robolig.controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robolig.controller.core.LogLevel
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val robotRepository: RobotRepository,
    ) : ViewModel() {
        val robotState: StateFlow<RobotState> = robotRepository.robotState

        fun updateVideoStreamUrl(url: String) {
            robotRepository.videoController.updateStreamUrl(url)
        }

        fun updateLogLevel(logLevel: LogLevel) {
            robotRepository.systemController.updateLogLevel(logLevel)
        }

        fun refreshStatus() {
            viewModelScope.launch {
                robotRepository.systemController.refreshStatus()
            }
        }
    }
