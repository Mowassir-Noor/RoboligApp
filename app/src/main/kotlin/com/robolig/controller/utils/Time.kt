package com.robolig.controller.utils

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

@Singleton
class AndroidMonotonicClock
    @Inject
    constructor() : MonotonicClock {
        override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    }
