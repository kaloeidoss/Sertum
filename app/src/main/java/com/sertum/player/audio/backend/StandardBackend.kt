package com.sertum.player.audio.backend

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.sertum.player.domain.playback.AudioOutputBackend
import com.sertum.player.domain.playback.BackendCapabilities
import com.sertum.player.domain.playback.StreamSpec
import java.nio.ByteBuffer

/**
 * System audio path (speaker / wired / Bluetooth).
 * Bit-perfect is NOT promised on this path (PRD 7.2).
 */
class StandardBackend : AudioOutputBackend {

    override val capabilities = BackendCapabilities(supportsHardwareVolume = false, isExclusive = false)

    private var sink: DefaultAudioSink? = null

    override fun open(spec: StreamSpec): Result<Unit> = runCatching {
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

    override fun writePcm(frame: ByteArray, offset: Int, length: Int): Result<Unit> = runCatching {
        val current = sink ?: error("backend not open")
        current.handleBuffer(ByteBuffer.wrap(frame, offset, length), 0, 1)
    }

    override fun pause(): Result<Unit> = runCatching { sink?.pause() ?: Unit }

    override fun play(): Result<Unit> = runCatching { sink?.play() ?: Unit }

    override fun flush(): Result<Unit> = runCatching { sink?.flush() ?: Unit }

    override fun stop(): Result<Unit> = runCatching { sink?.reset() ?: Unit }

    override fun release() {
        sink?.reset()
        sink = null
    }

    override fun onVolumeChanged(volume01: Float) {
        // System volume is applied by AudioTrack itself on the standard path.
    }
}
