package com.robolig.controller.presentation.viewmodel

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.testing.FakeRobotRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveViewModelTest {
    @Test
    fun updatesDriveInputsAndAuxiliaryControls() {
        val repository = FakeRobotRepository()
        val viewModel = DriveViewModel(repository)

        viewModel.updateDriveInput(ControlVector(x = 0.6f, y = -0.4f), rotationInput = 0.2f)
        viewModel.setBoostEnabled(true)
        viewModel.setBrakeActive(true)

        val vehicleState = repository.robotState.value.vehicle
        assertEquals(ControlVector(x = 0.6f, y = -0.4f), vehicleState.translationInput)
        assertEquals(0.2f, vehicleState.rotationInput)
        assertTrue(vehicleState.boostEnabled)
        assertEquals(100, vehicleState.brakePercent)
    }
}
