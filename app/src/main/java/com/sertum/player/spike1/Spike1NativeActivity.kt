package com.sertum.player.spike1

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.concurrent.thread

/**
 * Native AAudio/MMAP spike entry point.
 * Launch with:
 *   adb shell am start -n com.sertum.player/com.sertum.player.spike1.Spike1NativeActivity
 */
class Spike1NativeActivity : ComponentActivity() {

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = TextView(this).apply {
            text = "Spike-1 native running: see logcat tag SertumSpike"
            setPadding(48, 48, 48, 48)
        }
        setContentView(label)

        if (started) return
        started = true
        thread {
            Log.i("SertumSpike", "=== Spike-1 native AAudio start ===")
            val played = Spike1NativeEngine.runMatrix()
            Log.i("SertumSpike", "=== Spike-1 native AAudio done played=$played ===")
            runOnUiThread {
                label.text = "Spike-1 native done: $played/6 rates played"
            }
        }
    }
}
