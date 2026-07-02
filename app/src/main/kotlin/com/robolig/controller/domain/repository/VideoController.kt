package com.robolig.controller.domain.repository

/**
 * Manages the independently streamed MJPEG video channel.
 */
interface VideoController {
    fun updateStreamUrl(url: String)
}
