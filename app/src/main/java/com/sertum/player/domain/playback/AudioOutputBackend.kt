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

    /** Playback-head position in microseconds; backends without a head return 0. */
    fun getPositionUs(): Long = 0L

    /** Backend buffer size in frames; 0 means "not applicable / unknown". */
    fun getBufferSizeInFrames(): Int = 0
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
