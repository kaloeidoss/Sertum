package com.sertum.player.audio.backend

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.sertum.player.domain.playback.AudioOutputBackend

/**
 * Routes Media3 audio output between the system AudioTrack provider
 * (speaker / wired / Bluetooth) and the AAudio-exclusive backend (ADR-0001).
 * Switching is never hot: the coordinator stops the player, flips
 * [exclusiveEnabled], and re-prepares so the new output is configured
 * from scratch at the target sample rate (PRD 7.13).
 */
@androidx.annotation.OptIn(UnstableApi::class)
class RoutedAudioOutputProvider(context: Context) :
    androidx.media3.exoplayer.audio.ForwardingAudioOutputProvider(
        androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider.Builder(context).build(),
    ) {

    @Volatile
    var exclusiveBackend: AudioOutputBackend? = null

    @Volatile
    var exclusiveEnabled: Boolean = false

    private val exclusiveOutputs = java.util.concurrent.CopyOnWriteArrayList<BackendAudioOutput>()

    private fun isExclusiveRoute(): Boolean = exclusiveEnabled && exclusiveBackend != null

    override fun getFormatSupport(
        formatConfig: androidx.media3.exoplayer.audio.AudioOutputProvider.FormatConfig,
    ): androidx.media3.exoplayer.audio.AudioOutputProvider.FormatSupport {
        if (!isExclusiveRoute()) return super.getFormatSupport(formatConfig)
        val level = when (formatConfig.format.pcmEncoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_SUPPORTED_DIRECTLY
            else ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_SUPPORTED_WITH_TRANSCODING
        }
        return androidx.media3.exoplayer.audio.AudioOutputProvider.FormatSupport.Builder()
            .setFormatSupportLevel(level)
            .build()
    }

    override fun getOutputConfig(
        formatConfig: androidx.media3.exoplayer.audio.AudioOutputProvider.FormatConfig,
    ): androidx.media3.exoplayer.audio.AudioOutputProvider.OutputConfig {
        if (!isExclusiveRoute()) return super.getOutputConfig(formatConfig)
        val encoding = when (formatConfig.format.pcmEncoding) {
            C.ENCODING_PCM_24BIT -> C.ENCODING_PCM_24BIT
            else -> C.ENCODING_PCM_16BIT
        }
        val channelMask = when (formatConfig.format.channelCount) {
            2 -> android.media.AudioFormat.CHANNEL_OUT_STEREO
            else -> android.media.AudioFormat.CHANNEL_OUT_MONO
        }
        return androidx.media3.exoplayer.audio.AudioOutputProvider.OutputConfig.Builder()
            .setEncoding(encoding)
            .setSampleRate(formatConfig.format.sampleRate)
            .setChannelMask(channelMask)
            .setIsTunneling(false)
            .setIsOffload(false)
            .setBufferSize(formatConfig.preferredBufferSize)
            .setAudioAttributes(formatConfig.audioAttributes)
            .setAudioSessionId(formatConfig.audioSessionId)
            .setVirtualDeviceId(formatConfig.virtualDeviceId)
            .setUsePlaybackParameters(formatConfig.enablePlaybackParameters)
            .build()
    }

    override fun getAudioOutput(
        outputConfig: androidx.media3.exoplayer.audio.AudioOutputProvider.OutputConfig,
    ): androidx.media3.exoplayer.audio.AudioOutput {
        if (!isExclusiveRoute()) return super.getAudioOutput(outputConfig)
        val backend = exclusiveBackend
            ?: throw androidx.media3.exoplayer.audio.AudioOutputProvider.InitializationException(
                IllegalStateException("exclusive backend missing"),
            )
        return BackendAudioOutput(backend, outputConfig).also { exclusiveOutputs.add(it) }
    }

    override fun release() {
        exclusiveOutputs.forEach { it.release() }
        exclusiveOutputs.clear()
        super.release()
    }
}
