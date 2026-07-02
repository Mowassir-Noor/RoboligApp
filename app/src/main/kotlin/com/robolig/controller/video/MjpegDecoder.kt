package com.robolig.controller.video

import javax.inject.Inject
import javax.inject.Singleton

interface MjpegDecoder {
    fun supports(contentType: String?): Boolean
}

@Singleton
class BoundaryAwareMjpegDecoder
    @Inject
    constructor() : MjpegDecoder {
        override fun supports(contentType: String?): Boolean =
            contentType?.contains("multipart/x-mixed-replace", ignoreCase = true) == true
    }
