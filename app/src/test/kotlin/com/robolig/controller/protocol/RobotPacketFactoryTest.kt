package com.robolig.controller.protocol

import com.robolig.controller.domain.model.ControlVector
import com.robolig.controller.domain.model.RobotMode
import com.robolig.controller.domain.model.SafetyState
import com.robolig.controller.domain.model.VehicleState
import com.robolig.controller.utils.MonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotPacketFactoryTest {
    private class FakeClock(
        private val nowMs: Long,
    ) : MonotonicClock {
        override fun elapsedRealtimeMs(): Long = nowMs
    }

    @Test
    fun buildsVehicleControlPacketWithScaledPayloadAndFlags() {
        val packetFactory = RobotPacketFactory(FakeClock(nowMs = 0x0012_3456), PacketSequenceGenerator())
        val vehicleState =
            VehicleState(
                translationInput = ControlVector(x = 0.5f, y = -1f),
                rotationInput = 0.25f,
                throttlePercent = 60,
                brakePercent = 10,
                boostEnabled = true,
                locked = true,
            )

        val packet =
            packetFactory.createVehicleControlPacket(
                vehicleState = vehicleState,
                robotMode = RobotMode.DRIVE,
                safetyState = SafetyState(),
            )
        val payload = VehicleControlPayload.fromPayload(packet.payload)

        assertEquals(PacketType.VEHICLE_CONTROL, packet.type)
        assertEquals(0, packet.sequenceNumber)
        assertEquals(0x0012_3456, packet.timestampMillis)
        assertEquals(64, payload.moveX)
        assertEquals(-127, payload.moveY)
        assertEquals(32, payload.rotation)
        assertEquals(76, payload.throttle)
        assertEquals(13, payload.brake)
        assertEquals(127, payload.boost)
        assertTrue(packet.flags.vehicleLocked)
        assertEquals(false, packet.flags.emergencyStop)
    }

    @Test
    fun buildsEmergencyStopPacketWithHighestPriorityFlags() {
        val packetFactory = RobotPacketFactory(FakeClock(nowMs = 512L), PacketSequenceGenerator())

        val packet = packetFactory.createEmergencyStopPacket(robotMode = RobotMode.AUTO)

        assertEquals(PacketType.EMERGENCY_STOP, packet.type)
        assertTrue(packet.flags.emergencyStop)
        assertTrue(packet.flags.armLocked)
        assertTrue(packet.flags.vehicleLocked)
        assertTrue(packet.flags.autoMode)
    }
}
