package com.sertum.player.audio.backend

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
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
        // Direct support only for raw PCM families the AAudio backend
        // consumes. Compressed formats carry pcmEncoding metadata too
        // (e.g. FLAC stream info), so sampleMimeType must be audio/raw —
        // everything else must go through MediaCodec decoding first.
        val isRawPcm = MimeTypes.AUDIO_RAW == formatConfig.format.sampleMimeType
        val level = when {
            !isRawPcm ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_UNSUPPORTED
            formatConfig.format.pcmEncoding == C.ENCODING_PCM_16BIT ||
                formatConfig.format.pcmEncoding == C.ENCODING_PCM_24BIT ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_SUPPORTED_DIRECTLY
            // Float is produced by Media3's own processing pipeline for
            // 24/32-bit PCM. Returning WITH_TRANSCODING here prevents the
            // renderer from forcing decoders into float output, so 16-bit
            // sources keep their native depth; the float pipeline still
            // reaches getOutputConfig and is packed back to 24-bit.
            formatConfig.format.pcmEncoding == C.ENCODING_PCM_FLOAT ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_SUPPORTED_WITH_TRANSCODING
            else ->
                androidx.media3.exoplayer.audio.AudioOutputProvider.FORMAT_UNSUPPORTED
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
            C.ENCODING_PCM_FLOAT -> C.ENCODING_PCM_FLOAT
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
