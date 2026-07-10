package com.robolig.controller.communication

import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatManagerTest {
    private class FakeClock(
        initialNowMs: Long = 0L,
    ) : MonotonicClock {
        var nowMs: Long = initialNowMs

        override fun elapsedRealtimeMs(): Long = nowMs
    }

    @Test
    fun markReceivedSetsHealthyImmediately() =
        runBlocking {
            val clock = FakeClock()
            val manager = HeartbeatManager(clock)

            manager.markReceived()

            val status = manager.status.value
            assertEquals(clock.nowMs, status.lastReceivedAtMs)
            assertTrue(status.isHealthy)
        }

    @Test
    fun markReceivedBeforeAnySendIsHealthy() =
        runBlocking {
            val clock = FakeClock()
            val manager = HeartbeatManager(clock)

            manager.markReceived()

            assertTrue(manager.status.value.isHealthy)
        }

    @Test
    fun resetClearsAllState() =
        runBlocking {
            val clock = FakeClock()
            val manager = HeartbeatManager(clock)

            manager.markReceived()
            clock.nowMs = 1_000L
            manager.markSent()

            manager.reset()

            val status = manager.status.value
            assertNull(status.lastReceivedAtMs)
            assertNull(status.lastSentAtMs)
            assertFalse(status.isHealthy)
        }

    @Test
    fun markSentKeepsHealthIfRecentReceiveInsideTwoIntervals() =
        runBlocking {
            val clock = FakeClock()
            val manager = HeartbeatManager(clock)

            manager.markReceived()
            clock.nowMs = 600L
            manager.markSent()

            assertTrue(manager.status.value.isHealthy)
        }

    @Test
    fun markSentDoesNotRecoverHealthWhenNoReceiveHasHappened() =
        runBlocking {
            val clock = FakeClock()
            val manager = HeartbeatManager(clock)

            clock.nowMs = 1_000L
            manager.markSent()

            assertFalse(manager.status.value.isHealthy)
        }
}
