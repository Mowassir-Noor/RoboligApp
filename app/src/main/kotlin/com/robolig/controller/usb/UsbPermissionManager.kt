package com.robolig.controller.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbPermissionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val pendingRequestState = MutableStateFlow<Set<Int>>(emptySet())
        private val permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action != ACTION_USB_PERMISSION) {
                        return
                    }

                    val device =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                    pendingRequestState.value = pendingRequestState.value - (device?.deviceId ?: return)
                }
            }

        val pendingRequests: StateFlow<Set<Int>> = pendingRequestState.asStateFlow()

        init {
            val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(permissionReceiver, permissionFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(permissionReceiver, permissionFilter)
            }
        }

        fun hasPermissionForAnyAttachedDevice(): Boolean {
            val usbManager = context.getSystemService(UsbManager::class.java) ?: return false
            return usbManager.deviceList.values.any { device -> usbManager.hasPermission(device) }
        }

        fun hasPermission(device: UsbDevice): Boolean {
            val usbManager = context.getSystemService(UsbManager::class.java) ?: return false
            return usbManager.hasPermission(device)
        }

        fun isPermissionRequested(device: UsbDevice): Boolean = pendingRequestState.value.contains(device.deviceId)

        fun requestPermission(device: UsbDevice): Boolean {
            val usbManager = context.getSystemService(UsbManager::class.java) ?: return false
            val shouldRequest =
                !usbManager.hasPermission(device) &&
                    !pendingRequestState.value.contains(device.deviceId)

            if (shouldRequest) {
                pendingRequestState.value = pendingRequestState.value + device.deviceId
                usbManager.requestPermission(device, buildPermissionIntent(context))
            }
            return shouldRequest
        }
    }

private fun buildPermissionIntent(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        0,
        Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE,
    )

private const val ACTION_USB_PERMISSION = "com.robolig.controller.USB_PERMISSION"
