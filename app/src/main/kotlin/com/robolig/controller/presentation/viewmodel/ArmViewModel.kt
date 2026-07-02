package com.robolig.controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotState
import com.robolig.controller.domain.repository.RobotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ArmViewModel
    @Inject
    constructor(
        private val robotRepository: RobotRepository,
    ) : ViewModel() {
        val robotState: StateFlow<RobotState> = robotRepository.robotState

        fun updatePlanarInput(planarInput: ControlVector) {
            robotRepository.armController.updatePlanarInput(planarInput)
        }

        fun updateDepthInput(depthInput: Float) {
            robotRepository.armController.updateDepthInput(depthInput)
        }

        fun updateWristRotation(wristRotationInput: Float) {
            robotRepository.armController.updateWristRotation(wristRotationInput)
        }

        fun setGripperOpen(open: Boolean) {
            robotRepository.armController.setGripperOpen(open)
        }

        fun setPrecisionMode(enabled: Boolean) {
            robotRepository.armController.setPrecisionMode(enabled)
        }

        fun activatePreset(presetLabel: String) {
            robotRepository.armController.activatePreset(presetLabel)
        }

        fun updateZiplineHeight(height: Float) {
            robotRepository.armController.updateZiplineHeight(height)
        }
    }
