
# Robolig Controller UI Specification

## Overview

This document defines the visual appearance of the Robolig Controller.

The attached image **DesignV2_1.png** is the single source of truth for the UI.

The implementation must closely match the provided design.

Do not redesign the interface.

Small improvements for spacing, responsiveness and accessibility are allowed.

---

# Design Philosophy

The application should resemble a professional industrial robot controller.

It should feel similar to software used for

- industrial manipulators
- military UGVs
- autonomous ground vehicles
- robotic arms
- warehouse robots

Avoid colorful consumer-style interfaces.

The UI should be functional, minimalistic and information-dense.

---

# Theme

Background

Very dark

Approximately

#111111

Panels

Dark gray

Rounded corners

Thin borders

Accent

Soft red

Approximately

#FF6B6B

Text

White

Secondary text

Light gray

No gradients.

No glassmorphism.

No neumorphism.

---

# Display

Landscape only.

Target devices

Android Tablets

10"

11"

12"

Support

1280×800

1920×1080

2560×1600

Maintain proportions.

Never stretch controls.

---

# Layout

Every screen consists of

Top telemetry bar

Large live camera

Floating controls

Left status panel

Right mode panel

Bottom joysticks

The camera occupies most of the display.

The operator should never lose sight of the camera while controlling the robot.

---

# Top Telemetry Bar

Always visible.

Contains

Battery

Connection

Signal

Robot Mode

Current Speed

Current Task

FPS

Latency

Robot Warnings

---

# Left Status Panel

Small vertical buttons.

Examples

Emergency Stop

Lights

Horn

Reset

These buttons should always remain visible.

---

# Right Mode Panel

Vertical buttons.

Driving

Gripper

Zipline

Auto

The active mode is highlighted.

Mode switching should animate smoothly.

---

# Camera

The camera occupies approximately 70% of the screen.

Video should support

MJPEG

Loading indicator

Reconnect

Error state

Latency indicator

Fullscreen scaling

---

# Driving Mode

Left joystick

Vehicle translation

Forward

Backward

Left

Right

Right joystick

Steering

Rotation

Movement should feel immediate.

---

# Gripper Mode

The robot uses an anthropomorphic arm.

The operator controls the gripper position.

Never expose servo angles.

Use Cartesian control.

Left joystick

Move gripper

Up

Down

Left

Right

Right joystick

Forward

Backward

Rotate wrist

Buttons

Open gripper

Close gripper

Home

Pick

Place

Precision mode

---

# Zipline Mode

Camera

Driving joystick

Manipulator joystick

Vertical slider

Grip Zipline

Release Zipline

Status indicators

---

# Auto Mode

Mission status

Waypoint progress

Camera

Telemetry

Robot state

Mission controls

---

# Animations

Smooth

Fast

Professional

Use Compose animations.

Joystick movement

Button press

Mode transitions

Telemetry updates

Battery updates

Signal updates

Avoid flashy effects.

---

# Accessibility

Large touch targets.

Readable fonts.

High contrast.

Responsive layout.
