# TASKS.md

# Robolig Controller Development Roadmap

Version: 1.0

---

# Goal

Develop a production-grade Android application capable of controlling the Teknofest Robolig robot safely, reliably, and with low latency.

Each milestone should produce a working application.

Never skip tests.

Never introduce TODO placeholders.

Every completed task must build successfully before moving to the next milestone.

---

# Phase 1 — Project Foundation

## Task 1 — Project Initialization

### Objectives

- Create Android project
- Configure Gradle
- Configure Kotlin
- Configure Jetpack Compose
- Configure Hilt
- Configure Material 3
- Configure Navigation Compose
- Configure Coroutines
- Configure ktlint
- Configure Detekt
- Configure Proguard

### Acceptance Criteria

- Project builds
- Application launches
- CI passes

---

## Task 2 — Project Architecture

Create package structure

```
app/

core/

di/

domain/

data/

presentation/

communication/

usb/

video/

protocol/

repository/

service/

utils/
```

Acceptance Criteria

- Clean architecture established
- Dependency graph complete

---

## Task 3 — Theme System

Implement

- Dark theme
- Color palette
- Typography
- Shapes
- Spacing
- Responsive sizing

Acceptance Criteria

- Matches DESIGN.md

---

# Phase 2 — Navigation

---

## Task 4 — Navigation

Create

Drive

Gripper

Zipline

Auto

Settings

About

Acceptance Criteria

- Smooth transitions
- State preserved

---

## Task 5 — Main Layout

Create

Top telemetry

Side panels

Mode selector

Floating controls

Acceptance Criteria

- Matches design image

---

# Phase 3 — UI Components

---

## Task 6 — Camera Component

Create

CameraView

Features

- Loading
- Error
- Latency
- FPS
- Auto reconnect

Acceptance Criteria

- Reusable component

---

## Task 7 — Joystick Component

Implement

Deadzone

Sensitivity

Multi-touch

Return to center

Angle

Magnitude

Animation

Acceptance Criteria

- Smooth
- Accurate
- Reusable

---

## Task 8 — Buttons

Create reusable buttons

Emergency

Preset

Toggle

Momentary

Mode

Acceptance Criteria

Consistent styling

---

## Task 9 — Telemetry Widgets

Create

Battery

Signal

Connection

Latency

Warnings

Temperature

FPS

Acceptance Criteria

Reactive updates

---

# Phase 4 — Communication

---

## Task 10 — Packet Library

Implement

Packet

PacketBuilder

PacketParser

Checksum

Encoder

Decoder

Acceptance Criteria

100% protocol compliant

---

## Task 11 — USB Manager

Implement

Permission handling

Connect

Disconnect

Reconnect

Write queue

Read queue

Timeout detection

Statistics

Acceptance Criteria

Reliable connection

---

## Task 12 — Communication Manager

Create

Command queue

Packet scheduler

Heartbeat

Watchdog

Reconnect

Acceptance Criteria

Thread-safe

---

# Phase 5 — Video

---

## Task 13 — MJPEG Client

Implement

Streaming

Reconnect

Latency

Statistics

Frame timing

Acceptance Criteria

Stable stream

---

## Task 14 — Video Overlay

Overlay

Telemetry

Joystick

Buttons

Warnings

Acceptance Criteria

No dropped frames

---

# Phase 6 — Repository Layer

---

## Task 15 — Robot Repository

Create

Command API

Telemetry API

State API

Video API

Acceptance Criteria

Single source of truth

---

## Task 16 — Robot State

Create immutable RobotState

Contains

Vehicle

Arm

Battery

Signal

Telemetry

Mission

Video

Warnings

Acceptance Criteria

Uses StateFlow

---

# Phase 7 — Driving Mode

---

## Task 17 — Vehicle Control

Implement

Movement joystick

Steering joystick

Throttle

Brake

Boost

Acceptance Criteria

60Hz updates

---

## Task 18 — Emergency Stop

Implement

Emergency button

Confirmation

Hardware acknowledgement

Acceptance Criteria

Highest packet priority

---

# Phase 8 — Arm Control

---

## Task 19 — Cartesian Control

Implement

Move Up

Move Down

Move Left

Move Right

Forward

Backward

Acceptance Criteria

Smooth movement

---

## Task 20 — Inverse Kinematics

Implement

Forward Kinematics

Inverse Kinematics

Workspace limits

Joint limits

Acceptance Criteria

Stable solutions

---

## Task 21 — Gripper

Implement

Open

Close

Speed

Force

Acceptance Criteria

Reliable operation

---

## Task 22 — Presets

Implement

Home

Pick

Place

Acceptance Criteria

Single-click execution

---

# Phase 9 — Zipline Mode

---

## Task 23 — Zipline Controls

Implement

Driving

Manipulator

Slider

Grip

Release

Acceptance Criteria

Matches design

---

# Phase 10 — Autonomous Mode

---

## Task 24 — Mission Screen

Implement

Mission status

Waypoint

Robot state

Task progress

Acceptance Criteria

Fully reactive

---

# Phase 11 — Safety

---

## Task 25 — Watchdog

Implement

Heartbeat

Timeout

Auto stop

Reconnect

Acceptance Criteria

Robot always stops safely

---

## Task 26 — Packet Validation

Implement

Checksum

Header

Sequence

Length

Packet type

Acceptance Criteria

Reject invalid packets

---

## Task 27 — Error Handling

Implement

Communication errors

USB errors

Video errors

Robot errors

Acceptance Criteria

No crashes

---

# Phase 12 — Performance

---

## Task 28 — Optimization

Optimize

Compose recomposition

Memory

Packet allocation

Coroutines

USB

Acceptance Criteria

60 FPS

---

## Task 29 — Logging

Implement

USB logs

Packet logs

Video logs

Robot logs

Acceptance Criteria

Configurable log levels

---

# Phase 13 — Testing

---

## Task 30 — Unit Tests

Write tests

Protocol

Checksum

Repository

ViewModel

RobotState

Acceptance Criteria

High coverage

---

## Task 31 — Integration Tests

Test

USB

Video

Navigation

Packet flow

Acceptance Criteria

Stable

---

## Task 32 — Stress Testing

Simulate

Packet loss

Disconnects

High latency

Invalid packets

Acceptance Criteria

Application recovers automatically

---

# Phase 14 — Final Polish

---

## Task 33 — UI Polish

Improve

Animations

Spacing

Accessibility

Transitions

Acceptance Criteria

Matches DESIGN.md

---

## Task 34 — Documentation

Complete

README

Architecture

Protocol

Comments

Acceptance Criteria

Complete documentation

---

## Task 35 — Production Release

Verify

✓ Builds

✓ No TODOs

✓ No placeholder code

✓ No crashes

✓ Fully documented

✓ Performance targets met

✓ UI matches DesignV2_1.png

✓ Protocol matches PROTOCOL.md

✓ Architecture matches ARCHITECTURE.md

✓ Safe robot operation

---

# Final Acceptance Criteria

The project is considered complete only when:

- Every milestone is completed.
- Every feature in REQUIREMENTS.md is implemented.
- The UI closely matches DESIGN.md and DesignV2_1.png.
- USB communication is stable.
- Video streaming is reliable.
- The robot can be driven continuously with low latency.
- The robotic arm supports Cartesian control with inverse kinematics.
- Safety systems (heartbeat, watchdog, emergency stop) function correctly.
- The project builds without warnings or placeholder implementations.
- All tests pass.
- The application is suitable for use during the Teknofest Robolig competition.
