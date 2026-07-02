package com.robolig.controller.communication

import com.robolig.controller.protocol.Packet
import com.robolig.controller.protocol.PacketFlags
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.PacketType
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CommandQueueTest {
    private class FakeClock : MonotonicClock {
        private var currentTimeMs = 0L

        override fun elapsedRealtimeMs(): Long {
            currentTimeMs += 1L
            return currentTimeMs
        }
    }

    @Test
    fun prioritizesEmergencyStopThenHeartbeatThenTelemetry() =
        runBlocking {
            val commandQueue = CommandQueue(clock = FakeClock())

            commandQueue.enqueue(
                packet = packet(PacketType.TELEMETRY_REQUEST, sequenceNumber = 1),
                priority = PacketPriority.TELEMETRY,
                description = "Telemetry refresh",
            )
            commandQueue.enqueue(
                packet = packet(PacketType.HEARTBEAT, sequenceNumber = 2),
                priority = PacketPriority.HEARTBEAT,
                description = "Heartbeat",
            )
            commandQueue.enqueue(
                packet = packet(PacketType.EMERGENCY_STOP, sequenceNumber = 3),
                priority = PacketPriority.EMERGENCY_STOP,
                description = "Emergency stop",
            )

            assertEquals(3, commandQueue.queueDepth.value)
            assertEquals(PacketType.EMERGENCY_STOP, commandQueue.poll()?.packetType)
            assertEquals(PacketType.HEARTBEAT, commandQueue.poll()?.packetType)

            val telemetryCommand = commandQueue.poll()
            assertNotNull(telemetryCommand)
            assertEquals(PacketType.TELEMETRY_REQUEST, telemetryCommand?.packetType)
            assertEquals(0, commandQueue.queueDepth.value)
        }

    private fun packet(
        packetType: PacketType,
        sequenceNumber: Int,
    ): Packet =
        Packet(
            type = packetType,
            sequenceNumber = sequenceNumber,
            flags = PacketFlags(),
        )
}
