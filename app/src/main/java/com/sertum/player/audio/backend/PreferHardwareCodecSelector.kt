package com.sertum.player.audio.backend

import android.media.MediaCodecList
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Builds the decoder candidate list directly from Android's [MediaCodecList]
 * instead of Media3's default helper, then prefers hardware decoders.
 *
 * The Xiaomi 12S exposes `c2.qti.alac.hw.decoder`, but Media3's default
 * selector never returns it, so the broken `c2.qti.alac.sw.decoder` is the
 * only ALAC candidate and rejects valid streams with CodecException
 * 0x80000000. All other decoders remain available as fallbacks.
 */
@androidx.annotation.OptIn(UnstableApi::class)
object PreferHardwareCodecSelector : MediaCodecSelector {

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        val direct = buildList {
            for (codecInfo in MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos) {
                if (codecInfo.isEncoder) continue
                for (supportedType in codecInfo.supportedTypes) {
                    if (!supportedType.equals(mimeType, ignoreCase = true)) continue
                    val capabilities = codecInfo.getCapabilitiesForType(supportedType)
                    add(
                        MediaCodecInfo.newInstance(
                            /* name= */ codecInfo.name,
                            /* mimeType= */ mimeType,
                            /* codecMimeType= */ supportedType,
                            /* capabilities= */ capabilities,
                            /* adaptive= */ false,
                            /* tunneling= */ false,
                            /* secure= */ false,
                            /* hardwareAccelerated= */ codecInfo.isHardwareAccelerated,
                            /* softwareOnly= */ codecInfo.isSoftwareOnly,
                        ),
                    )
                }
            }
        }
        if (direct.isEmpty()) {
            return MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
        }
        val hardware = direct.filter {
            it.hardwareAccelerated || it.name.contains("hw", ignoreCase = true)
        }
        return hardware + direct.filterNot { candidate -> hardware.any { candidate === it } }
    }
}
