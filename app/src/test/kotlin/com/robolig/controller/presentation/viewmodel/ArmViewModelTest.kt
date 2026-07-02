package com.robolig.controller.presentation.viewmodel

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.testing.FakeRobotRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmViewModelTest {
    @Test
    fun updatesManipulatorInputsAndPresetState() {
        val repository = FakeRobotRepository()
        val viewModel = ArmViewModel(repository)

        viewModel.updatePlanarInput(ControlVector(x = 0.25f, y = 0.75f))
        viewModel.updateDepthInput(0.4f)
        viewModel.updateWristRotation(-0.5f)
        viewModel.setGripperOpen(true)
        viewModel.setPrecisionMode(true)
        viewModel.activatePreset("Pick")
        viewModel.updateZiplineHeight(0.8f)

        val armState = repository.robotState.value.arm
        assertEquals(ControlVector(x = 0.25f, y = 0.75f), armState.planarInput)
        assertEquals(0.4f, armState.depthInput)
        assertEquals(-0.5f, armState.wristRotationInput)
        assertTrue(armState.gripperOpen)
        assertTrue(armState.precisionModeEnabled)
        assertEquals("Pick", armState.activePreset)
        assertEquals(0.8f, armState.ziplineHeight)
    }
}
