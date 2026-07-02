package com.robolig.controller.communication

import com.robolig.controller.protocol.Packet
import com.robolig.controller.protocol.PacketPriority
import com.robolig.controller.protocol.PacketType
import com.robolig.controller.utils.MonotonicClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicLong

data class QueuedPacket(
    val sequenceId: Long,
    val packet: Packet,
    val priority: PacketPriority,
    val description: String,
    val queuedAtMs: Long,
) {
    val packetType: PacketType
        get() = packet.type
}

class CommandQueue(
    private val clock: MonotonicClock,
) {
    private val queueMutex = Mutex()
    private val sequenceGenerator = AtomicLong(0L)
    private val queue =
        PriorityQueue(
            compareBy<QueuedPacket>({ it.priority.order }, { it.sequenceId }),
        )
    private val queueDepthState = MutableStateFlow(0)

    val queueDepth: StateFlow<Int> = queueDepthState.asStateFlow()

    suspend fun enqueue(
        packet: Packet,
        priority: PacketPriority,
        description: String,
    ): QueuedPacket =
        queueMutex.withLock {
            val queuedPacket =
                QueuedPacket(
                    sequenceId = sequenceGenerator.getAndIncrement(),
                    packet = packet,
                    priority = priority,
                    description = description,
                    queuedAtMs = clock.elapsedRealtimeMs(),
                )

            queue.add(queuedPacket)
            queueDepthState.value = queue.size
            queuedPacket
        }

    suspend fun poll(): QueuedPacket? =
        queueMutex.withLock {
            queue.poll().also { queueDepthState.value = queue.size }
        }

    suspend fun snapshot(): List<QueuedPacket> =
        queueMutex.withLock {
            queue.toList().sortedWith(compareBy({ it.priority.order }, { it.sequenceId }))
        }

    suspend fun clear() {
        queueMutex.withLock {
            queue.clear()
            queueDepthState.value = 0
        }
    }
}
