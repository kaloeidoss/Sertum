package com.sertum.player.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * PRD 7.13: watches USB audio attach/detach (AudioManager device callback
 * plus UsbManager broadcasts as belt-and-braces) and Bluetooth output
 * presence. Coordinates the reaction; never shows system notifications here.
 */
class UsbHotplugController(
    context: Context,
    private val onUsbAttached: () -> Unit,
    private val onUsbDetached: () -> Unit,
    private val onBluetoothChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var usbPresent = false
    private var bluetoothPresent = false
    private var closed = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            update(addedDevices.toList(), present = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            update(removedDevices.toList(), present = false)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> onAttached()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> onDetached()
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(appContext, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        scanInitialDevices()
    }

    private fun scanInitialDevices() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        update(devices.toList(), present = true)
    }

    private fun update(devices: List<AudioDeviceInfo>, present: Boolean) {
        if (closed) return
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    if (present && !usbPresent) {
                        usbPresent = true
                        onUsbAttached()
                    } else if (!present && usbPresent) {
                        usbPresent = false
                        onUsbDetached()
                    }
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                -> {
                    val newValue = present
                    if (newValue != bluetoothPresent) {
                        bluetoothPresent = newValue
                        onBluetoothChanged(newValue)
                    }
                }
            }
        }
    }

    private fun onAttached() {
        if (!usbPresent) {
            usbPresent = true
            onUsbAttached()
        }
    }

    private fun onDetached() {
        if (usbPresent) {
            usbPresent = false
            onUsbDetached()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        try {
            appContext.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // not registered
        }
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
}
