# Architecture

## Robolig Controller System Architecture

Version: 1.0

---

# Purpose

This document describes the software architecture of the Robolig Android Controller.

The goal is to create a modular, production-grade, low-latency robot controller capable of controlling the Teknofest Robolig robot in real time.

The application must remain maintainable, scalable, testable, and reliable.

---

# Design Principles

The application follows the following principles:

- Clean Architecture
- MVVM
- SOLID
- Dependency Injection
- Reactive Programming
- Single Source of Truth
- Unidirectional Data Flow
- Modular Components
- Thread Safety
- High Performance

---

# High Level Architecture

```
                    Android Application
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
        UI             ViewModels         Services
         │                  │                  │
         └──────────────┬───┘                  │
                        ▼                      ▼
                  Repository Layer      Communication Layer
                        │                      │
                        └──────────────┬───────┘
                                       ▼
                               Robot State Manager
                                       │
              ┌────────────────────────┼─────────────────────────┐
              ▼                        ▼                         ▼
        USB Serial              Video Manager            Safety Manager
              │                        │                         │
              ▼                        ▼                         ▼
      Deneyap Mini              WiFi MJPEG              Watchdog Logic
              │
              ▼
            NRF24
              │
              ▼
            Robot
```

---

# Architectural Layers

## Presentation Layer

Responsible only for displaying data.

Contains

- Screens
- Components
- Navigation
- Themes
- Animations

Never contains business logic.

Uses

Jetpack Compose

StateFlow

ViewModels

---

## ViewModel Layer

Responsible for

- UI state
- Commands
- Screen logic
- Validation
- Calling repositories

Never accesses USB directly.

Never accesses networking directly.

Everything goes through repositories.

---

## Repository Layer

Acts as the single interface between UI and communication.

Responsible for

- Robot commands
- Telemetry
- Video state
- Robot state
- Packet generation
- Packet parsing

Every ViewModel talks only to repositories.

---

## Communication Layer

Responsible for

USB communication.

Video communication.

Packet protocol.

Heartbeat.

Reconnect logic.

Queue management.

No UI logic exists here.

---

## Hardware Layer

Contains

USB Manager

Packet Encoder

Packet Decoder

Checksum

Serial Communication

Everything related to hardware remains isolated.

---

# Package Structure

```
com.robolig.controller

├── app
│
├── core
│
├── di
│
├── domain
│
├── data
│
├── communication
│
├── robot
│
├── video
│
├── usb
│
├── protocol
│
├── presentation
│
├── service
│
├── utils
│
└── testing
```

---

# Detailed Structure

```
com.robolig.controller

app/
    RobotApplication.kt

core/
    Constants.kt
    Dispatchers.kt
    Logger.kt
    Extensions.kt

di/
    AppModule.kt
    RepositoryModule.kt
    CommunicationModule.kt
    NetworkModule.kt

domain/

    model/

        RobotState.kt
        BatteryState.kt
        SignalState.kt
        VehicleState.kt
        ArmState.kt
        CameraState.kt
        MissionState.kt
        Telemetry.kt

    repository/

        RobotRepository.kt

data/

    repository/

        RobotRepositoryImpl.kt

communication/

    CommunicationManager.kt
    CommandQueue.kt
    PacketScheduler.kt
    HeartbeatManager.kt
    ConnectionMonitor.kt

usb/

    UsbSerialManager.kt
    UsbPermissionManager.kt
    UsbConnection.kt

video/

    VideoStreamManager.kt
    MjpegDecoder.kt
    VideoStatistics.kt

protocol/

    Packet.kt
    PacketBuilder.kt
    PacketParser.kt
    Checksum.kt
    PacketEncoder.kt
    PacketDecoder.kt

presentation/

    screens/

        DriveScreen.kt
        GripperScreen.kt
        ZiplineScreen.kt
        AutoScreen.kt

    components/

        CameraView.kt
        Joystick.kt
        TelemetryBar.kt
        SidePanel.kt
        BatteryWidget.kt
        SignalWidget.kt

    navigation/

        NavigationGraph.kt

    viewmodel/

        MainViewModel.kt
        DriveViewModel.kt
        ArmViewModel.kt
        AutoViewModel.kt

service/

    RobotControlService.kt
    VideoService.kt

utils/

    Time.kt
    Math.kt
    Preferences.kt

testing/

    FakeRepository.kt
```

---

# Dependency Flow

```
Compose UI

↓

ViewModel

↓

Repository

↓

Communication Layer

↓

USB / WiFi

↓

Robot
```

The dependency direction must never be violated.

---

# State Management

There is only one source of truth.

```
RobotState
```

Every screen observes RobotState.

RobotState contains

```
Connection

Battery

Signal

Speed

Current Mode

Camera

Manipulator

Vehicle

Mission

Warnings

Errors

Telemetry
```

RobotState is immutable.

Updates create new copies.

---

# Communication Architecture

```
ViewModel

↓

Repository

↓

Packet Builder

↓

USB Queue

↓

USB Serial

↓

Deneyap Mini

↓

NRF24

↓

Robot
```

Incoming packets

```
Robot

↓

NRF24

↓

Deneyap

↓

USB

↓

Packet Parser

↓

Repository

↓

RobotState

↓

UI
```

---

# Video Architecture

Video is independent.

```
Robot Camera

↓

WiFi

↓

MJPEG Stream

↓

Video Manager

↓

Compose UI
```

Loss of video must never stop robot control.

Loss of USB must never stop video.

Both systems remain independent.

---

# Control Scheduler

The application contains multiple independent schedulers.

Vehicle

60 Hz

Arm

30 Hz

Telemetry

10 Hz

Heartbeat

500 ms

Each scheduler runs in its own coroutine.

---

# Packet Queue

Outgoing packets are placed into a queue.

```
UI

↓

Repository

↓

Packet Queue

↓

USB Writer
```

Incoming packets

```
USB Reader

↓

Parser

↓

RobotState
```

Queues are thread-safe.

---

# Safety Manager

Responsible for

Emergency Stop

Watchdog

Heartbeat

Reconnect

Auto Stop

Timeout Detection

Packet Validation

Low Battery

Signal Loss

If heartbeat fails

```
Stop Robot

Notify User

Reconnect
```

---

# Navigation

The application has four primary destinations.

```
Drive

Gripper

Zipline

Auto
```

Navigation must preserve RobotState.

Changing screens must never interrupt communication.

---

# Dependency Injection

Hilt manages

Repositories

Communication

USB

Video

Managers

Services

ViewModels

No manual dependency creation.

---

# Background Services

RobotControlService

Maintains communication even if UI changes.

VideoService

Maintains video decoding.

---

# Threading Model

Main Thread

Compose

ViewModels

Default Dispatcher

Packet Encoding

Packet Parsing

Math

Inverse Kinematics

IO Dispatcher

USB

Video

File Operations

No blocking calls on Main.

---

# Error Handling

Every hardware failure should

Log

Recover

Notify UI

Retry if possible

Never crash.

---

# Logging

Separate log tags.

```
USB

VIDEO

PACKET

IK

UI

VIEWMODEL

REPOSITORY

SAFETY
```

Support debug logging.

Disable verbose logs in release builds.

---

# Performance Targets

UI

60 FPS

Vehicle Control

60 Hz

Manipulator

30 Hz

Packet Decode

< 1 ms

USB Latency

< 10 ms

Memory

No excessive allocations

GC pauses should be minimized.

---

# Testing Strategy

Unit Tests

Repository

Protocol

Checksum

Packet Parser

Packet Builder

ViewModels

Communication

Integration Tests

USB

Video

RobotState

Navigation

---

# Future Expansion

The architecture should support future additions without major refactoring.

Possible future modules include:

- Autonomous navigation
- ROS 2 bridge
- SLAM visualization
- LiDAR integration
- Multi-camera support
- Voice commands
- Remote internet control
- Cloud telemetry
- Mission scripting
- AI-assisted manipulation
- Multiple robot profiles

All new functionality should be added as independent modules while preserving the existing architecture.
