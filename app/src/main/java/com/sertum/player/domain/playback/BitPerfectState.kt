package com.sertum.player.domain.playback

enum class BitPerfectState { INTACT, DEGRADED, NOT_APPLICABLE }

/**
 * PRD 7.3 volume policy:
 * - hardware volume: always bit-perfect intact.
 * - software volume disabled: locked at 100%, intact.
 * - software volume enabled: 100% intact, below 100% degraded.
 */
data class VolumePolicy(
    val hardwareVolume: Boolean,
    val softwareVolumeEnabled: Boolean,
    val softwareVolume01: Float,
) {
    init {
        require(softwareVolume01 in 0f..1f) { "softwareVolume01 must be in [0,1]" }
    }

    val bitPerfectState: BitPerfectState
        get() = when {
            hardwareVolume -> BitPerfectState.INTACT
            !softwareVolumeEnabled -> BitPerfectState.INTACT
            softwareVolume01 >= 1.0f -> BitPerfectState.INTACT
            else -> BitPerfectState.DEGRADED
        }
}
