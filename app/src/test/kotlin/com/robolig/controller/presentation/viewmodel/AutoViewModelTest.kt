package com.robolig.controller.presentation.viewmodel

import com.robolig.controller.testing.FakeRobotRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoViewModelTest {
    @Test
    fun pausesAndAbortsMissionState() {
        val repository = FakeRobotRepository()
        val viewModel = AutoViewModel(repository)

        viewModel.setMissionPaused(true)
        assertTrue(repository.robotState.value.mission.isPaused)
        assertEquals("Mission paused", repository.robotState.value.mission.currentTask)

        viewModel.abortMission()

        assertFalse(repository.robotState.value.mission.isPaused)
        assertEquals("Mission aborted", repository.robotState.value.mission.currentTask)
        assertEquals(0, repository.robotState.value.mission.progressPercent)
    }
}
