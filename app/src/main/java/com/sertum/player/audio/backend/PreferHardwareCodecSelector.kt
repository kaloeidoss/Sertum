package com.sertum.player.audio.backend

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Prefers hardware decoders when available, keeps every other decoder as a
 * fallback. The Xiaomi 12S has both `c2.qti.alac.sw.decoder` (which rejects
 * some valid ALAC streams) and `c2.qti.alac.hw.decoder`; default Media3
 * ordering picks the broken software decoder first.
 */
@androidx.annotation.OptIn(UnstableApi::class)
object PreferHardwareCodecSelector : MediaCodecSelector {

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        val all = MediaCodecSelector.DEFAULT.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder,
        )
        val hardware = all.filter {
            it.hardwareAccelerated || it.name.contains("hw", ignoreCase = true)
        }
        return hardware + all.filterNot { candidate -> hardware.any { candidate === it } }
    }
}
