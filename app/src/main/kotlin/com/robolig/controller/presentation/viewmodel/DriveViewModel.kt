package com.robolig.controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DriveViewModel
    @Inject
    constructor(
        private val robotRepository: RobotRepository,
    ) : ViewModel() {
        val robotState: StateFlow<RobotState> = robotRepository.robotState

        fun updateDriveInput(
            translationInput: ControlVector,
            rotationInput: Float,
        ) {
            robotRepository.driveController.updateInput(
                translationInput = translationInput,
                rotationInput = rotationInput,
            )
        }

        fun setBoostEnabled(enabled: Boolean) {
            robotRepository.driveController.setBoostEnabled(enabled)
        }

        fun setBrakeActive(active: Boolean) {
            robotRepository.driveController.setBrakeActive(active)
        }
    }
