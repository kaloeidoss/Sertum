package com.sertum.player.spike2

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Spike-2: read-only feasibility probe for taking over the USB DAC.
 *
 * It enumerates USB devices, asks for permission on the audio device,
 * claims interface 0, and probes SET_INTERFACE (alt-setting) control
 * transfers. It never sends audio data.
 *
 * Launch with:
 *   adb shell am start -n com.sertum.player/com.sertum.player.spike2.Spike2Activity
 */
class Spike2Activity : ComponentActivity() {

    companion object {
        private const val TAG = "SertumSpike"
        private const val ACTION_USB_PERMISSION = "com.sertum.player.USB_PERMISSION"
        private const val USB_VID_DAWN = 0x2F44 // Moondrop VID; probing is name-based anyway
    }

    private lateinit var usbManager: UsbManager
    private lateinit var label: TextView
    private var permissionRequested = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            Log.i(TAG, "usb permission granted=$granted device=$device")
            if (granted && device != null) {
                probe(device)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerPermissionReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(permissionReceiver, filter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        label = TextView(this).apply { setPadding(48, 48, 48, 48) }
        setContentView(label)
        registerPermissionReceiver()
        enumerate()
    }

    private fun enumerate() {
        val devices = usbManager.deviceList.values
        Log.i(TAG, "=== Spike-2 enumerate ${devices.size} usb devices ===")
        devices.forEach { d ->
            Log.i(
                TAG,
                "usb device vid=${d.vendorId.toString(16)} pid=${d.productId.toString(16)} " +
                    "name=${d.productName} interfaces=${d.interfaceCount}",
            )
        }
        val audio = devices.firstOrNull {
            (it.productName?.contains("Dawn", ignoreCase = true) == true) ||
                it.vendorId == USB_VID_DAWN
        } ?: devices.firstOrNull { it.interfaceCount > 0 }
        if (audio == null) {
            Log.w(TAG, "no candidate USB audio device found")
            label.text = "No candidate USB device found"
            return
        }
        Log.i(TAG, "candidate device=$audio")
        if (!usbManager.hasPermission(audio)) {
            if (permissionRequested) return
            permissionRequested = true
            val pi = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(packageName),
                PendingIntent.FLAG_MUTABLE,
            )
            usbManager.requestPermission(audio, pi)
            return
        }
        probe(audio)
    }

    private fun probe(device: UsbDevice) {
        Log.i(TAG, "=== Spike-2 probe device=${device.productName} ===")
        val conn: UsbDeviceConnection = try {
            usbManager.openDevice(device) ?: run {
                Log.w(TAG, "openDevice returned null")
                label.text = "openDevice failed"
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "openDevice threw: ${e.message}")
            label.text = "openDevice threw"
            return
        }

        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            Log.i(
                TAG,
                "interface $i id=${intf.id} class=${intf.interfaceClass} " +
                    "subclass=${intf.interfaceSubclass} protocol=${intf.interfaceProtocol} " +
                    "endpoints=${intf.endpointCount}",
            )
            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                Log.i(
                    TAG,
                    "  ep${ep.endpointNumber} dir=${ep.direction} type=${ep.type} " +
                        "attr=${ep.attributes} max=${ep.maxPacketSize}",
                )
            }
        }

        // Claim every interface in turn (release immediately) and probe
        // GET_INTERFACE / SET_INTERFACE where the device supports it.
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            val claimed = try {
                conn.claimInterface(intf, true)
            } catch (e: Exception) {
                Log.w(TAG, "claimInterface($i) threw: ${e.message}")
                false
            }
            Log.i(TAG, "claimInterface($i) force=true -> $claimed")
            if (!claimed) continue

            val current = ByteArray(1)
            val get = conn.controlTransfer(
                UsbConstants.USB_DIR_IN or UsbConstants.USB_TYPE_CLASS,
                10, 0, i, current, 1, 2000,
            )
            Log.i(TAG, "GET_INTERFACE($i) -> $get current=${current[0]}")

            // Audio streaming interface on this DAC is interface 2 (5 alts);
            // probe a small alt range regardless.
            val altMax = if (i == 2) 5 else 3
            for (alt in 0..altMax) {
                val result = conn.controlTransfer(
                    UsbConstants.USB_TYPE_CLASS or UsbConstants.USB_DIR_OUT,
                    11, alt, i, null, 0, 2000,
                )
                Log.i(TAG, "SET_INTERFACE($i) alt=$alt -> $result (negative means failure)")
            }
            try { conn.releaseInterface(intf) } catch (_: Exception) {}
        }
        conn.close()
        label.text = "Spike-2 done; see logcat SertumSpike"
        Log.i(TAG, "=== Spike-2 probe done ===")
    }

    override fun onDestroy() {
        unregisterReceiver(permissionReceiver)
        super.onDestroy()
    }
}
