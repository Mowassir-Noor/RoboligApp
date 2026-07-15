package com.robolig.controller.robot

import com.robolig.controller.communication.CommunicationState
import com.robolig.controller.domain.model.CameraState
import com.robolig.controller.domain.model.RobotState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RobotStateFactory
    @Inject
    constructor() {
        fun create(
            communicationState: CommunicationState,
            cameraState: CameraState,
            showPacketsOverlay: Boolean = false,
        ): RobotState {
            val warnings =
                buildList {
                    addAll(communicationState.warnings)
                    if (cameraState.streamUrl.isBlank()) {
                        add("Video stream URL is not configured")
                    }
                }
            val errors =
                buildList {
                    addAll(communicationState.errors)
                    cameraState.lastError?.let(::add)
                }

            return RobotState(
                connectionState = communicationState.connectionState,
                currentMode = communicationState.currentMode,
                battery = communicationState.battery,
                signal = communicationState.signal,
                vehicle = communicationState.vehicle,
                arm = communicationState.arm,
                camera = cameraState,
                mission = communicationState.mission,
                telemetry = communicationState.telemetry,
                safety = communicationState.safety,
                diagnostics = communicationState.diagnostics,
                warnings = warnings,
                errors = errors,
                showPacketsOverlay = showPacketsOverlay,
            )
        }
    }
