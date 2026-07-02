# Robolig Controller

Production-oriented Android controller for the Teknofest Robolig robot.

The project follows the repository specifications in this order:

1. `REQUIREMENTS.md`
2. `PROTOCOL.md`
3. `DESIGN.md`
4. `ARCHITECTURE.md`
5. `TASK.md`

The official UI reference image is `docs/UI/robot app DesignV2_1.png`.

## System Summary

- Platform: Android, minimum API 26
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM + Hilt + Flow/StateFlow
- Robot control transport: USB serial at `115200` baud through Deneyap Mini v2
- Video transport: independent WiFi MJPEG stream
- Packet size: fixed `32` bytes
- Safety: heartbeat, watchdog, auto-stop, emergency stop, packet validation, checksum validation, sequence validation

## Architecture

Dependency direction is kept as:

`UI -> ViewModels -> Repositories -> Communication Layer -> Hardware Managers -> USB / Video -> Robot`

Key packages:

- `app/src/main/kotlin/com/robolig/controller/presentation`: screens, navigation, components, theme
- `app/src/main/kotlin/com/robolig/controller/domain`: immutable models and repository contracts
- `app/src/main/kotlin/com/robolig/controller/data`: repository implementation and controller adapters
- `app/src/main/kotlin/com/robolig/controller/communication`: control loops, queueing, heartbeat, watchdog, inbound/outbound processing
- `app/src/main/kotlin/com/robolig/controller/protocol`: packet model, checksum, encoder, parser, decoder, packet factory
- `app/src/main/kotlin/com/robolig/controller/usb`: USB permission and serial managers
- `app/src/main/kotlin/com/robolig/controller/video`: MJPEG stream manager and decoder
- `app/src/main/kotlin/com/robolig/controller/robot`: state factory and arm kinematics

## Control Modes

- `Drive`: translation joystick, steering joystick, boost, brake, emergency stop
- `Gripper`: Cartesian arm control, wrist rotation, precision mode, presets, gripper open/close
- `Zipline`: drive joystick, manipulator joystick, height slider, grip/release
- `Auto`: mission state, waypoint progress, pause/resume, abort

## Protocol Notes

- Header byte: `0xAA`
- Packet types: vehicle, arm, PTZ, telemetry request/response, emergency stop, heartbeat
- Sequence number: unsigned 8-bit rollover counter
- Timestamp: 3-byte millisecond counter since app start
- Checksum: XOR of bytes `0..30`, written to byte `31`
- Invalid packets are rejected by header, length, checksum, type, sequence, and payload validation

## Build And Verification

Use a writable Gradle cache location when needed:

```bash
env GRADLE_USER_HOME=/tmp/robolig-gradle ./gradlew --no-daemon ktlintFormat detekt testDebugUnitTest assembleDebug
```

Useful targets:

```bash
env GRADLE_USER_HOME=/tmp/robolig-gradle ./gradlew --no-daemon compileDebugKotlin
env GRADLE_USER_HOME=/tmp/robolig-gradle ./gradlew --no-daemon connectedDebugAndroidTest
```

## Device Verification

The current development device is detected through:

```bash
/home/mainframe/Android/Sdk/platform-tools/adb devices -l
```

If `connectedDebugAndroidTest` fails with `INSTALL_FAILED_USER_RESTRICTED` on Xiaomi/MIUI devices, enable the following on the phone:

1. Developer options
2. `USB debugging`
3. `USB debugging (Security settings)` if present
4. `Install via USB`
5. Any install confirmation prompt shown after `adb` starts the APK install

Package verifier was disabled for ADB installs during local testing with:

```bash
/home/mainframe/Android/Sdk/platform-tools/adb shell settings put global package_verifier_enable 0
/home/mainframe/Android/Sdk/platform-tools/adb shell settings put global verifier_verify_adb_installs 0
```

MIUI can still reject installation until its own security prompt is accepted on-device.

## Safety Expectations

- Emergency stop packets must always preempt queued traffic
- USB control failure must leave the robot in a safe stopped state
- Video failure must not block USB control
- USB failure must not silently corrupt or stall video state handling
- All operator-visible UI state must originate from the immutable `RobotState`

## Repository Status

Current local verification completed successfully for:

- `ktlintFormat`
- `detekt`
- `testDebugUnitTest`
- `assembleDebug`

Instrumentation packaging is healthy, but full connected execution remains device-blocked until MIUI allows APK installation over USB.
