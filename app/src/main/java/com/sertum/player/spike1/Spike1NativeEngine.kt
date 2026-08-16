package com.sertum.player.spike1

/** JNI bridge for the native AAudio/MMAP spike (libsertumspike). */
object Spike1NativeEngine {
    init {
        System.loadLibrary("sertumspike")
    }

    external fun runMatrix(): Int
}
