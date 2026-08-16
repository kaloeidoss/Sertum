package com.sertum.player.audio.backend

import android.util.Log
import com.sertum.player.domain.playback.AudioOutputBackend
import com.sertum.player.domain.playback.BackendCapabilities
import com.sertum.player.domain.playback.StreamSpec

/** Observed native stream parameters, used for evidence and UI badges. */
data class AaudioStreamInfo(
    val actualRate: Int = 0,
    val actualFormat: Int = 0,
    val sharingMode: Int = 0,
    val deviceId: Int = 0,
    val performanceMode: Int = 0,
    val framesPerBurst: Int = 0,
) {
    val isExclusive: Boolean get() = sharingMode == 1
    val is24Bit: Boolean get() = actualFormat == 3
}

/**
 * USB-exclusive backend built on native AAudio EXCLUSIVE streams (ADR-0001).
 * The native layer requests EXCLUSIVE and falls back to SHARED only when the
 * device refuses; the resulting sharing mode is surfaced in [streamInfo].
 */
class AaudioExclusiveBackend : AudioOutputBackend {

    companion object {
        private const val TAG = "SertumAudio"
        private const val EXCLUSIVE = 1
    }

    private var handle = 0L
    private var spec: StreamSpec? = null

    override var capabilities = BackendCapabilities(supportsHardwareVolume = false, isExclusive = false)
        private set

    var streamInfo = AaudioStreamInfo()
        private set

    override fun open(spec: StreamSpec): Result<Unit> {
        close()
        this.spec = spec
        val opened = AaudioNative.nativeOpen(spec.sampleRate, spec.channelCount, spec.bitDepth)
        if (opened == 0L) return Result.failure(IllegalStateException("AAudio open failed"))
        handle = opened
        streamInfo = AaudioStreamInfo(
            actualRate = AaudioNative.nativeGetActualRate(handle),
            actualFormat = AaudioNative.nativeGetActualFormat(handle),
            sharingMode = AaudioNative.nativeGetSharingMode(handle),
            deviceId = AaudioNative.nativeGetDeviceId(handle),
            performanceMode = AaudioNative.nativeGetPerformanceMode(handle),
            framesPerBurst = AaudioNative.nativeGetFramesPerBurst(handle),
        )
        capabilities = BackendCapabilities(
            supportsHardwareVolume = false,
            isExclusive = streamInfo.isExclusive,
        )
        Log.i(
            TAG,
            "opened spec=$spec actual=$streamInfo exclusive=${streamInfo.isExclusive}",
        )
        return Result.success(Unit)
    }

    override fun writePcm(frame: ByteArray, offset: Int, length: Int): Result<Unit> {
        val written = AaudioNative.nativeWrite(handle, frame, offset, length)
        val bytesPerFrame = if (streamInfo.is24Bit) 6 else 4
        val expected = length / bytesPerFrame
        return if (written == expected) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("AAudio write frames=$written expected=$expected"))
        }
    }

    override fun pause(): Result<Unit> =
        if (AaudioNative.nativePause(handle)) Result.success(Unit)
        else Result.failure(IllegalStateException("AAudio pause failed"))

    override fun play(): Result<Unit> =
        if (AaudioNative.nativeStart(handle)) Result.success(Unit)
        else Result.failure(IllegalStateException("AAudio start failed"))

    override fun flush(): Result<Unit> =
        if (AaudioNative.nativeFlush(handle)) Result.success(Unit)
        else Result.failure(IllegalStateException("AAudio flush failed"))

    override fun stop(): Result<Unit> =
        if (AaudioNative.nativeStop(handle)) Result.success(Unit)
        else Result.failure(IllegalStateException("AAudio stop failed"))

    override fun release() = close()

    override fun onVolumeChanged(volume01: Float) {
        // Software volume is intentionally not applied on this path (PRD 7.3).
    }

    private fun close() {
        if (handle != 0L) {
            AaudioNative.nativeClose(handle)
            handle = 0L
        }
        spec = null
    }
}
