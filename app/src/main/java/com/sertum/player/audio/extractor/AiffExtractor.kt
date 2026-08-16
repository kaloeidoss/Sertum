package com.sertum.player.audio.extractor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import java.io.EOFException

/**
 * Minimal AIFF/AIFC PCM extractor for Media3.
 *
 * Supports FORM AIFF/AIFC containers with COMM + SSND chunks, 16/24/32-bit
 * big-endian PCM. Seeking is not implemented for the M2 slice (Unseekable);
 * the player can still decode and play the stream.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class AiffExtractor : Extractor {

    companion object {
        private const val STATE_HEADER = 0
        private const val STATE_CHUNKS = 1
        private const val STATE_DATA = 2

        fun sniffFormat(input: ExtractorInput): Boolean {
            val header = ByteArray(12)
            return try {
                input.peekFully(header, 0, 12, false)
                header[0] == 'F'.code.toByte() &&
                    header[1] == 'O'.code.toByte() &&
                    header[2] == 'R'.code.toByte() &&
                    header[3] == 'M'.code.toByte() &&
                    header[8] == 'A'.code.toByte() &&
                    header[9] == 'I'.code.toByte() &&
                    header[10] == 'F'.code.toByte() &&
                    (header[11] == 'F'.code.toByte() || header[11] == 'C'.code.toByte())
            } catch (e: Exception) {
                false
            }
        }
    }

    private var state = STATE_HEADER
    private var output: ExtractorOutput? = null
    private var trackOutput: TrackOutput? = null
    private var frameBytes = 0
    private var dataBytesRemaining = 0L
    private var framesFed = 0L
    private var sampleRate = 1

    override fun sniff(input: ExtractorInput): Boolean = sniffFormat(input)

    override fun init(output: ExtractorOutput) {
        this.output = output
    }

    override fun read(input: ExtractorInput, positionHolder: PositionHolder): Int {
        return when (state) {
            STATE_HEADER -> readHeader(input)
            STATE_CHUNKS -> readChunks(input)
            STATE_DATA -> readData(input)
            else -> Extractor.RESULT_END_OF_INPUT
        }
    }

    private fun readHeader(input: ExtractorInput): Int {
        val header = ByteArray(12)
        if (!input.readFully(header, 0, 12, false)) return Extractor.RESULT_END_OF_INPUT
        state = STATE_CHUNKS
        return Extractor.RESULT_CONTINUE
    }

    private fun readChunks(input: ExtractorInput): Int {
        val chunkHeader = ByteArray(8)
        if (!input.readFully(chunkHeader, 0, 8, false)) return Extractor.RESULT_END_OF_INPUT
        val chunkId = String(chunkHeader, 0, 4, Charsets.US_ASCII)
        val chunkSize = readU32(chunkHeader, 4)

        when (chunkId) {
            "COMM" -> {
                val comm = ByteArray(18)
                if (!input.readFully(comm, 0, 18, false)) return Extractor.RESULT_END_OF_INPUT
                val channelCount = readU16(comm, 0)
                val frameCount = readU32(comm, 2)
                val bitsPerSample = readU16(comm, 6)
                sampleRate = readExtended80(comm, 8).toInt().coerceAtLeast(1)
                frameBytes = channelCount * (bitsPerSample / 8)
                val pcmEncoding = when (bitsPerSample) {
                    16 -> C.ENCODING_PCM_16BIT
                    24 -> C.ENCODING_PCM_24BIT
                    32 -> C.ENCODING_PCM_32BIT
                    else -> return Extractor.RESULT_END_OF_INPUT
                }
                val out = output ?: return Extractor.RESULT_END_OF_INPUT
                trackOutput = out.track(0, C.TRACK_TYPE_AUDIO).apply {
                    format(
                        Format.Builder()
                            .setSampleMimeType(MimeTypes.AUDIO_RAW)
                            .setSampleRate(sampleRate)
                            .setChannelCount(channelCount)
                            .setPcmEncoding(pcmEncoding)
                            .build(),
                    )
                }
                val durationUs = frameCount * 1_000_000L / sampleRate
                out.seekMap(SeekMap.Unseekable(durationUs))
            }

            "SSND" -> {
                val ssnd = ByteArray(8)
                if (!input.readFully(ssnd, 0, 8, false)) return Extractor.RESULT_END_OF_INPUT
                dataBytesRemaining = chunkSize - 8L
                state = STATE_DATA
            }

            else -> {
                val toSkip = chunkSize + (chunkSize and 1L)
                if (toSkip > Int.MAX_VALUE) return Extractor.RESULT_END_OF_INPUT
                input.skipFully(toSkip.toInt())
            }
        }
        return Extractor.RESULT_CONTINUE
    }

    private fun readData(input: ExtractorInput): Int {
        val out = trackOutput ?: return Extractor.RESULT_END_OF_INPUT
        if (dataBytesRemaining <= 0) return Extractor.RESULT_END_OF_INPUT

        val block = 4096.coerceAtMost(dataBytesRemaining.toInt())
        val read = out.sampleData(input, block, false)
        if (read <= 0) return Extractor.RESULT_END_OF_INPUT
        dataBytesRemaining -= read

        val frames = read / frameBytes
        framesFed += frames
        out.sampleMetadata(
            framesFed * 1_000_000L / sampleRate,
            C.BUFFER_FLAG_KEY_FRAME,
            read,
            0,
            null,
        )
        return if (dataBytesRemaining <= 0) Extractor.RESULT_END_OF_INPUT else Extractor.RESULT_CONTINUE
    }

    override fun seek(position: Long, timeUs: Long) {
        // Unseekable for the M2 slice.
    }

    override fun release() {
        trackOutput = null
        output = null
    }

    private fun readU16(b: ByteArray, offset: Int): Int =
        ((b[offset].toInt() and 0xFF) shl 8) or (b[offset + 1].toInt() and 0xFF)

    private fun readU32(b: ByteArray, offset: Int): Long =
        ((b[offset].toLong() and 0xFF) shl 24) or
            ((b[offset + 1].toLong() and 0xFF) shl 16) or
            ((b[offset + 2].toLong() and 0xFF) shl 8) or
            (b[offset + 3].toLong() and 0xFF)

    /** 80-bit IEEE 754 extended float, big-endian, as used by AIFF COMM. */
    private fun readExtended80(b: ByteArray, offset: Int): Double {
        val sign = if ((b[offset].toInt() and 0x80) != 0) -1.0 else 1.0
        val exponent = ((b[offset].toInt() and 0x7F) shl 8) or (b[offset + 1].toInt() and 0xFF)
        var mantissa = 0L
        for (i in 2..9) {
            mantissa = (mantissa shl 8) or (b[offset + i].toLong() and 0xFF)
        }
        if (exponent == 0 && mantissa == 0L) return 0.0
        val integerPart = (mantissa ushr 63) and 1L
        val fraction = (mantissa and 0x7FFFFFFFFFFFFFFFL) / 9223372036854775808.0
        return sign * (integerPart + fraction) * Math.pow(2.0, (exponent - 16383).toDouble())
    }
}
