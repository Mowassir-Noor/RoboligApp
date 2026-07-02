package com.robolig.controller.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.LogTag
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RobotControlService : Service() {
    @Inject
    lateinit var logger: AppLogger

    override fun onCreate() {
        super.onCreate()
        logger.d(LogTag.SAFETY, "RobotControlService created")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
