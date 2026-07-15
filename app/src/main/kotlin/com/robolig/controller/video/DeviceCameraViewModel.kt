package com.robolig.controller.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.robolig.controller.utils.ControllerPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DeviceCameraViewModel
    @Inject
    constructor(
        private val deviceCameraManager: DeviceCameraManager,
        controllerPreferences: ControllerPreferences,
    ) : ViewModel() {
        val useDeviceCamera: StateFlow<Boolean> = controllerPreferences.useDeviceCamera
        val isBound: StateFlow<Boolean> = deviceCameraManager.isBound
        val frameBytes: StateFlow<ByteArray?> = deviceCameraManager.frameBytes

        fun bind(lifecycleOwner: LifecycleOwner) {
            deviceCameraManager.start(lifecycleOwner)
        }

        fun unbind() {
            deviceCameraManager.stop()
        }

        override fun onCleared() {
            super.onCleared()
            deviceCameraManager.stop()
        }
    }
