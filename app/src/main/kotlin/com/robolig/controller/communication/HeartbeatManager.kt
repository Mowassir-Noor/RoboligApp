package com.robolig.controller.communication

import com.robolig.controller.core.ControlLoopConstants
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class HeartbeatStatus(
    val intervalMs: Long = ControlLoopConstants.HEARTBEAT_INTERVAL_MS,
    val lastSentAtMs: Long? = null,
    val lastReceivedAtMs: Long? = null,
    val isHealthy: Boolean = false,
)

@Singleton
class HeartbeatManager
    @Inject
    constructor(
        private val clock: MonotonicClock,
    ) {
        private val heartbeatState = MutableStateFlow(HeartbeatStatus())

        val status: StateFlow<HeartbeatStatus> = heartbeatState.asStateFlow()

        fun markSent() {
            val now = clock.elapsedRealtimeMs()
            val currentState = heartbeatState.value
            val isHealthy =
                currentState.lastReceivedAtMs?.let { lastReceivedAt ->
                    now - lastReceivedAt <= currentState.intervalMs * 2
                } ?: false

            heartbeatState.value = currentState.copy(lastSentAtMs = now, isHealthy = isHealthy)
        }

        fun markReceived() {
            val now = clock.elapsedRealtimeMs()
            heartbeatState.value =
                heartbeatState.value.copy(
                    lastReceivedAtMs = now,
                    isHealthy = true,
                )
        }

        fun reset() {
            heartbeatState.value = HeartbeatStatus()
        }
    }
