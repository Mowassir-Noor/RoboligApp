
# Robolig Controller Requirements

## Objective

Develop a production-grade Android application for controlling a Teknofest Robolig robot.

The application communicates with a Deneyap Mini v2 over USB.

The Deneyap acts only as a transparent bridge.

Communication between the bridge and robot uses NRF24.

Video is streamed independently using WiFi.

---

# Technology

Language

Kotlin

UI

Jetpack Compose

Architecture

MVVM

Dependency Injection

Hilt

Concurrency

Coroutines

Flow

Navigation

Navigation Compose

Minimum Android

API 26

---

# Communication

USB

115200 baud

Binary protocol

Auto reconnect

Permission handling

Packet queue

Heartbeat

Watchdog

Statistics

WiFi

MJPEG

Reconnect

Latency monitoring

Configurable URL

---

# Packet Protocol

Exactly

32 bytes

Header

Packet Type

Sequence

Flags

Payload

Timestamp

Checksum

Checksum

XOR

Reject invalid packets.

---

# Control Frequencies

Vehicle

60 Hz

Manipulator

30 Hz

Telemetry

10 Hz

Heartbeat

500 ms

---

# Robot Modes

Driving

Gripper

Zipline

Auto

---

# Driving

Movement joystick

Steering joystick

Emergency stop

Boost

Brake

Realtime telemetry

---

# Gripper

Cartesian control

6 DOF manipulator

Inverse Kinematics

The application computes joint angles.

The robot receives only joint commands.

Controls

Up

Down

Left

Right

Forward

Backward

Open

Close

Rotate Wrist

Presets

Home

Pick

Place

---

# Zipline

Driving

Manipulator

Height slider

Grip

Release

Realtime telemetry

---

# Auto

Mission display

Waypoint display

Robot state

Task progress

Pause

Resume

Abort

---

# Safety

Emergency stop

Connection timeout

Robot watchdog

Auto stop

Packet validation

Checksum validation

Sequence validation

Low battery warnings

Signal warnings

---

# Robot State

Maintain a single observable RobotState.

Include

Battery

Connection

Signal

Speed

Manipulator state

Vehicle state

Mission state

Telemetry

Video

Warnings

Errors

Use StateFlow.

---

# Performance

Target

60 FPS

Low latency

Low allocations

Reusable buffers

No UI blocking

No busy waiting

---

# Logging

Structured logging.

Communication logs.

Connection logs.

Packet logs.

Error logs.

---

# Testing

Unit tests

Protocol tests

Checksum tests

Repository tests

Communication tests

ViewModel tests

---

# Code Quality

No duplicated code.

No TODOs.

No placeholder implementations.

Proper documentation.

Clean architecture.

Reusable components.

Thread-safe.

Production-ready.

---

# Success Criteria

The final application should be capable of controlling a real Teknofest robot during competition.

The interface should match DesignV2_1.png.

The application should be reliable enough for continuous field operation.

Every feature should be fully implemented.

The project should build without errors.

No mocked implementations.

No unfinished code.
