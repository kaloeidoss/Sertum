package com.sertum.player.spike1

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.concurrent.thread

/**
 * Spike-1 entry point. Launch with:
 *   adb shell am start -n com.sertum.player/com.sertum.player.spike1.Spike1Activity
 *
 * It auto-runs the full sample-rate/bit-depth matrix once and writes
 * results to logcat under the "SertumSpike" tag.
 */
class Spike1Activity : ComponentActivity() {

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = TextView(this).apply {
            text = "Spike-1 running: see logcat tag SertumSpike"
            setPadding(48, 48, 48, 48)
        }
        setContentView(label)

        if (started) return
        started = true
        thread {
            Log.i("SertumSpike", "=== Spike-1 matrix start ===")
            val engine = Spike1AudioEngine()
            val played = engine.runMatrix()
            Log.i("SertumSpike", "=== Spike-1 matrix done played=$played ===")
            runOnUiThread {
                label.text = "Spike-1 done: $played/12 tones played"
            }
        }
    }
}
