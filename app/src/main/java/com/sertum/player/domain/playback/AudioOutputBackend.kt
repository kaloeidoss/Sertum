package com.sertum.player.domain.playback

/** Single contract every output backend implements (PRD ADR-S1). */
interface AudioOutputBackend {
    val capabilities: BackendCapabilities

    fun open(spec: StreamSpec): Result<Unit>
    fun writePcm(frame: ByteArray, offset: Int, length: Int): Result<Unit>
    fun pause(): Result<Unit>
    fun play(): Result<Unit>
    fun flush(): Result<Unit>
    fun stop(): Result<Unit>
    fun release()
    fun onVolumeChanged(volume01: Float)
}

data class StreamSpec(
    val sampleRate: Int,
    val channelCount: Int,
    val bitDepth: Int,
    val isExclusive: Boolean,
)

data class BackendCapabilities(
    val supportsHardwareVolume: Boolean,
    val isExclusive: Boolean,
)
