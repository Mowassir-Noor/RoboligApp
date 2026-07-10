package com.robolig.controller.communication

import com.robolig.controller.protocol.Packet
import com.robolig.controller.protocol.PacketFlags
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.PacketType
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandQueueSignalTest {
    private class FakeClock : MonotonicClock {
        private var nowMs = 0L

        override fun elapsedRealtimeMs(): Long = nowMs
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

    @Test
    fun packetAvailableSignalIsDeliveredAfterEnqueue() =
        runBlocking {
            val commandQueue = CommandQueue(clock = FakeClock())

            val signalDeferred = commandQueue.packetAvailableSignal()

            commandQueue.enqueue(
                packet = packet(PacketType.HEARTBEAT, sequenceNumber = 1),
                priority = PacketPriority.HEARTBEAT,
                description = "Heartbeat",
            )

            val received = signalDeferred.tryReceive()
            assertNotNull(received.getOrNull())
            assertTrue(received.isSuccess)
            assertEquals(1, commandQueue.queueDepth.value)
        }

    @Test
    fun multipleEnqueuesAreCoalescedByConflatedChannel() =
        runBlocking {
            val commandQueue = CommandQueue(clock = FakeClock())
            val signalDeferred = commandQueue.packetAvailableSignal()

            commandQueue.enqueue(packet(PacketType.HEARTBEAT, 1), PacketPriority.HEARTBEAT, "h1")
            commandQueue.enqueue(packet(PacketType.HEARTBEAT, 2), PacketPriority.HEARTBEAT, "h2")
            commandQueue.enqueue(packet(PacketType.HEARTBEAT, 3), PacketPriority.HEARTBEAT, "h3")

            assertEquals(3, commandQueue.queueDepth.value)

            val firstSignal = signalDeferred.tryReceive()
            val secondSignal = signalDeferred.tryReceive()

            assertTrue(firstSignal.isSuccess)
            assertTrue(secondSignal.isFailure)
        }
}
