# PROTOCOL.md

# Robolig Communication Protocol

Version: 1.0

---

# Overview

This document defines the complete communication protocol between the Android Controller and the robot.

Communication path

```
Android App
    │
USB Serial
    │
Deneyap Mini v2
    │
NRF24
    │
Robot
```

The Deneyap Mini **must not process packets**.

It acts only as a transparent bridge.

Every byte received from USB is immediately transmitted through NRF24.

Every byte received from NRF24 is immediately forwarded over USB.

---

# Design Goals

The protocol is designed for

- Low latency
- High reliability
- Small packet size
- Fast parsing
- Simple implementation
- Error detection
- Future extensibility

---

# Communication Rates

| Communication | Frequency |
|--------------|-----------|
| Vehicle Control | 60 Hz |
| Arm Control | 30 Hz |
| PTZ Control | 30 Hz |
| Telemetry Request | 10 Hz |
| Heartbeat | Every 500 ms |

---

# Packet Size

Every packet is

```
32 bytes
```

No packet is ever larger.

No packet is ever smaller.

This keeps compatibility with NRF24.

---

# Packet Structure

| Byte | Name | Size |
|------|------|------|
|0|Header|1|
|1|Packet Type|1|
|2|Sequence Number|1|
|3|Flags|1|
|4-27|Payload|24|
|28-30|Timestamp|3|
|31|Checksum|1|

---

# Header

Always

```
0xAA
```

Reject every packet with an invalid header.

---

# Packet Types

| Type | Value |
|------|-------|
|Vehicle Control|0x01|
|Arm Control|0x02|
|PTZ Control|0x03|
|Telemetry Request|0x04|
|Telemetry Response|0x05|
|Emergency Stop|0x0E|
|Heartbeat|0x0F|

Unknown packet types must be ignored.

---

# Sequence Number

8-bit unsigned integer.

Increment for every outgoing packet.

```
0
1
2
...
255
0
```

Used to

- detect packet loss
- detect duplicates
- measure latency

---

# Flags

Bit layout

```
Bit 0

Emergency Stop

Bit 1

Precision Mode

Bit 2

Arm Locked

Bit 3

Vehicle Locked

Bit 4

Auto Mode

Bit 5

Reserved

Bit 6

Reserved

Bit 7

Reserved
```

---

# Timestamp

3-byte unsigned integer.

Milliseconds since application startup.

Wraparound is acceptable.

Used for

Latency

Synchronization

Debugging

---

# Checksum

Checksum is XOR.

Algorithm

```
checksum = byte0

XOR byte1

XOR byte2

...

XOR byte30
```

Checksum is stored in byte 31.

Reject invalid packets immediately.

---

# Vehicle Control Packet

Packet Type

```
0x01
```

Payload

| Byte | Description |
|------|-------------|
|4|Move X|
|5|Move Y|
|6|Rotation|
|7|Throttle|
|8|Brake|
|9|Boost|
|10-27|Reserved|

Values

```
-127

...

0

...

127
```

---

# Arm Control Packet

Packet Type

```
0x02
```

Payload

| Byte | Description |
|------|-------------|
|4|Shoulder|
|5|Elbow|
|6|Wrist Pitch|
|7|Wrist Roll|
|8|Gripper Rotation|
|9|Gripper|
|10-27|Reserved|

Servo values

```
0

...

180
```

Gripper

```
0

Closed

255

Open
```

---

# PTZ Packet

Packet Type

```
0x03
```

Payload

| Byte | Description |
|------|-------------|
|4|Pan|
|5|Tilt|
|6|Zoom|
|7-27|Reserved|

---

# Telemetry Request

Packet Type

```
0x04
```

Payload unused.

Robot immediately responds.

---

# Telemetry Response

Packet Type

```
0x05
```

Payload

| Byte | Description |
|------|-------------|
|4|Battery|
|5|Signal|
|6|Current Speed|
|7|Temperature|
|8|Current Mode|
|9|Error Code|
|10|Motor Current|
|11|Arm Current|
|12|CPU Load|
|13-27|Reserved|

---

# Emergency Stop

Packet Type

```
0x0E
```

Immediately

Stop motors

Disable arm motion

Disable autonomous tasks

Robot must acknowledge.

---

# Heartbeat

Packet Type

```
0x0F
```

Contains no payload.

Robot responds immediately.

---

# Robot Modes

| Mode | Value |
|------|-------|
|Drive|0|
|Gripper|1|
|Zipline|2|
|Auto|3|

---

# Connection State Machine

```
Disconnected

↓

USB Connected

↓

Serial Open

↓

Heartbeat Running

↓

Connected
```

Disconnect

↓

Reconnect

↓

Heartbeat

↓

Connected

---

# Watchdog

If no valid packet received within

```
2000 ms
```

Robot

Stops

Locks movement

Waits for reconnect

---

# Packet Validation

Incoming packets are checked in this order

Header

↓

Length

↓

Checksum

↓

Packet Type

↓

Sequence

↓

Payload

Only then

Update RobotState

---

# RobotState Update Flow

```
USB

↓

Packet Decoder

↓

Checksum

↓

Parser

↓

Repository

↓

RobotState

↓

ViewModel

↓

Compose UI
```

---

# Control Pipeline

```
Joystick

↓

ViewModel

↓

Repository

↓

Packet Builder

↓

USB Queue

↓

Serial

↓

NRF24

↓

Robot
```

---

# Queue Rules

Outgoing queue

FIFO

Incoming queue

FIFO

Emergency Stop

Highest priority

Heartbeat

Second priority

Telemetry

Lowest priority

---

# Error Codes

| Code | Description |
|------|-------------|
|0|No Error|
|1|Low Battery|
|2|Motor Fault|
|3|Arm Fault|
|4|Communication Fault|
|5|Emergency Stop|
|6|Watchdog Triggered|
|7|Unknown Error|

---

# Safety Rules

If USB disconnects

Immediately stop robot.

If heartbeat timeout

Immediately stop robot.

If checksum fails

Discard packet.

If packet type invalid

Discard packet.

If header invalid

Discard packet.

If packet size invalid

Discard packet.

---

# Future Reserved Packet Types

| Type | Purpose |
|------|---------|
|0x10|Firmware Update|
|0x11|Calibration|
|0x12|Debug|
|0x13|Configuration|
|0x14|Camera Control|
|0x15|Path Upload|
|0x16|Mission Download|
|0x17|Sensor Stream|

---

# Version Compatibility

Protocol Version

```
1.0
```

Future versions should remain backward compatible whenever possible.

Reserved bytes should never be repurposed without incrementing the protocol version.
