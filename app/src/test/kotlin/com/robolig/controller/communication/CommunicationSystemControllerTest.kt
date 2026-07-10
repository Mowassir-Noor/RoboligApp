package com.robolig.controller.communication

import com.robolig.controller.testing.FakeRobotRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationSystemControllerTest {
    @Test
    fun emergencyStopAppendsErrorMessage() =
        runBlocking {
            val repository = FakeRobotRepository()

            repository.systemController.emergencyStop()

            val state = repository.robotState.value
            assertTrue(state.errors.any { it.contains("Emergency stop requested") })
            assertEquals(100, state.vehicle.brakePercent)
        }

    @Test
    fun resetEmergencyStopClearsLatchedSafetyFlag() =
        runBlocking {
            val repository = FakeRobotRepository()

            repository.systemController.emergencyStop()
            assertEquals(100, repository.robotState.value.vehicle.brakePercent)

            repository.systemController.resetEmergencyStop()

            val state = repository.robotState.value
            assertFalse(state.safety.emergencyStopLatched)
            assertEquals(0, state.vehicle.brakePercent)
            assertFalse(state.vehicle.locked)
        }

    @Test
    fun resetEmergencyStopOnHealthyStateIsANoOp() =
        runBlocking {
            val repository = FakeRobotRepository()
            val initialErrors = repository.robotState.value.errors

            repository.systemController.resetEmergencyStop()

            assertEquals(initialErrors, repository.robotState.value.errors)
        }
}
