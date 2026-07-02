package com.robolig.controller.domain.repository

/**
 * Controls autonomous mission execution state.
 */
interface MissionController {
    fun setPaused(paused: Boolean)

    fun abort()
}
