package com.sertum.player.audio.backend

import android.media.AudioDeviceInfo
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import com.sertum.player.domain.playback.AudioOutputBackend
import com.sertum.player.domain.playback.StreamSpec
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Media3 [androidx.media3.exoplayer.audio.AudioOutput] backed by an
 * [AudioOutputBackend]. Used for the USB-exclusive AAudio path (ADR-0001):
 * Media3 decodes to PCM, this adapter writes the PCM straight into the
 * native AAudio EXCLUSIVE stream.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class BackendAudioOutput(
    private val backend: AudioOutputBackend,
    private val outputConfig: androidx.media3.exoplayer.audio.AudioOutputProvider.OutputConfig,
) : androidx.media3.exoplayer.audio.AudioOutput {

    private val listeners = CopyOnWriteArrayList<androidx.media3.exoplayer.audio.AudioOutput.Listener>()
    private val sampleRate = outputConfig.sampleRate
    private var playbackParameters = PlaybackParameters.DEFAULT
    private var volume = 1f
    private var released = false

    init {
        val bitDepth = when (outputConfig.encoding) {
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_FLOAT -> 24
            else -> 16
        }
        val channels = Integer.bitCount(outputConfig.channelMask).coerceAtLeast(1)
        backend.open(StreamSpec(sampleRate, channels, bitDepth, isExclusive = true))
            .getOrElse { throw androidx.media3.exoplayer.audio.AudioOutputProvider.InitializationException(it) }
    }

    override fun play() {
        if (!released) backend.play()
    }

    override fun pause() {
        if (!released) backend.pause()
    }

    override fun write(buffer: ByteBuffer, channelCount: Int, presentationTimeUs: Long): Boolean {
        if (released) return false
        if (!buffer.hasRemaining()) return true
        val pcmBytes = when (outputConfig.encoding) {
            C.ENCODING_PCM_FLOAT -> floatToPacked24Le(buffer)
            else -> {
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                bytes
            }
        }
        if (pcmBytes.isEmpty()) return true
        backend.writePcm(pcmBytes, 0, pcmBytes.size).getOrElse {
            throw androidx.media3.exoplayer.audio.AudioOutput.WriteException(
                PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
                true,
            )
        }
        return true
    }

    /**
     * Media3 float PCM is native-endian (little-endian on Android) IEEE-754.
     * Packed 24-bit ints keep 24-bit source material bit-exact because every
     * 24-bit integer is exactly representable in float32.
     */
    private fun floatToPacked24Le(buffer: ByteBuffer): ByteArray {
        val floatView = buffer.asFloatBuffer()
        val out = ByteArray(floatView.remaining() * 3)
        var outIndex = 0
        while (floatView.hasRemaining()) {
            val sample = floatView.get().coerceIn(-1f, 1f)
            val scaled = when {
                sample >= 1f -> MAX_24BIT
                sample <= -1f -> -MAX_24BIT - 1
                else -> (sample * (MAX_24BIT + 1)).toInt()
            }
            out[outIndex++] = (scaled and 0xFF).toByte()
            out[outIndex++] = ((scaled shr 8) and 0xFF).toByte()
            out[outIndex++] = ((scaled shr 16) and 0xFF).toByte()
        }
        return out
    }

    override fun flush() {
        if (!released) backend.flush()
    }

    override fun stop() {
        if (!released) backend.stop()
    }

    override fun release() {
        if (released) return
        released = true
        backend.release()
        listeners.forEach { it.onReleased() }
        listeners.clear()
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        backend.onVolumeChanged(this.volume)
    }

    override fun isOffloadedPlayback(): Boolean = false

    override fun getAudioSessionId(): Int = outputConfig.audioSessionId

    override fun getSampleRate(): Int = sampleRate

    override fun getBufferSizeInFrames(): Long = backend.getBufferSizeInFrames().toLong()

    override fun getPositionUs(): Long = backend.getPositionUs()

    override fun getPlaybackParameters(): PlaybackParameters = playbackParameters

    override fun isStalled(): Boolean = false

    override fun addListener(listener: androidx.media3.exoplayer.audio.AudioOutput.Listener) {
        listeners.addIfAbsent(listener)
    }

    override fun removeListener(listener: androidx.media3.exoplayer.audio.AudioOutput.Listener) {
        listeners.remove(listener)
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        this.playbackParameters = playbackParameters
    }

    override fun setOffloadDelayPadding(delayPaddingFrames: Int, delayPaddingForSpeedChangeFrames: Int) = Unit

    override fun setOffloadEndOfStream() = Unit

    override fun attachAuxEffect(auxEffectId: Int) = Unit

    override fun setAuxEffectSendLevel(sendLevel: Float) = Unit

    override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) = Unit

    companion object {
        private const val MAX_24BIT = 8_388_607
    }
}
