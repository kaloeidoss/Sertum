package com.sertum.player.audio.backend

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.sertum.player.domain.playback.AudioOutputBackend
import com.sertum.player.domain.playback.BackendCapabilities
import com.sertum.player.domain.playback.StreamSpec
import java.nio.ByteBuffer

/**
 * System audio path (speaker / wired / Bluetooth).
 * Bit-perfect is NOT promised on this path (PRD 7.2).
 */
@androidx.annotation.OptIn(UnstableApi::class)
class StandardBackend : AudioOutputBackend {

    override val capabilities = BackendCapabilities(supportsHardwareVolume = false, isExclusive = false)

    private var sink: DefaultAudioSink? = null
    private var spec: StreamSpec? = null

    override fun open(spec: StreamSpec): Result<Unit> = runCatching {
        this.spec = spec
        val built = DefaultAudioSink.Builder()
            .setEnableFloatOutput(true)
            .build()
        val encoding = if (spec.bitDepth >= 24) C.ENCODING_PCM_24BIT else C.ENCODING_PCM_16BIT
        val format = Format.Builder()
            .setSampleRate(spec.sampleRate)
            .setChannelCount(spec.channelCount)
            .setPcmEncoding(encoding)
            .build()
        built.configure(format, 0, intArrayOf(spec.channelCount))
        sink = built
    }

    override fun writePcm(frame: ByteArray, offset: Int, length: Int): Result<Int> = runCatching {
        val current = sink ?: error("backend not open")
        val bytesPerFrame = when (spec?.bitDepth ?: 16) {
            24 -> 6
            else -> 4
        }
        val frames = length / bytesPerFrame
        if (frames <= 0) return@runCatching 0
        val consumed = current.handleBuffer(ByteBuffer.wrap(frame, offset, frames * bytesPerFrame), 0, 1)
        if (consumed) frames else 0
    }

    override fun pause(): Result<Unit> = runCatching { sink?.pause() ?: Unit }

    override fun play(): Result<Unit> = runCatching { sink?.play() ?: Unit }

    override fun flush(): Result<Unit> = runCatching { sink?.flush() ?: Unit }

    override fun stop(): Result<Unit> = runCatching { sink?.reset() ?: Unit }

    override fun release() {
        sink?.reset()
        sink = null
        spec = null
    }

    override fun onVolumeChanged(volume01: Float) {
        // System volume is applied by AudioTrack itself on the standard path.
    }

    override fun getPositionUs(): Long = sink?.getCurrentPositionUs(false) ?: 0L
}
