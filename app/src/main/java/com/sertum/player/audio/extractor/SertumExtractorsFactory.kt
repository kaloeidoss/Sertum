package com.sertum.player.audio.extractor

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory

/**
 * Media3 tries extractors in order. AIFF has no native Media3 extractor, so
 * the self-written [AiffExtractor] is probed first (sniffing FORM....AIFF),
 * then the standard set handles FLAC/WAV/MP4-ALAC/MP3/etc.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class SertumExtractorsFactory(
    private val delegate: ExtractorsFactory = DefaultExtractorsFactory(),
) : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> =
        arrayOf(AiffExtractor(), *delegate.createExtractors())

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>,
    ): Array<Extractor> = arrayOf(AiffExtractor(), *delegate.createExtractors(uri, responseHeaders))
}
