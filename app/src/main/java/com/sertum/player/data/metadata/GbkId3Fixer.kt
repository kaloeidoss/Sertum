package com.sertum.player.data.metadata

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * ID3v2.3 legacy text decoding:
 * - encoding byte 0: Latin-1 per spec, but Chinese files in the wild are
 *   often GBK/GB18030, so we upgrade to GB18030 when it yields CJK text.
 * - encoding byte 1/2: UTF-16 with BOM (LE/BE).
 * - encoding byte 3: UTF-8.
 */
object GbkId3Fixer {

    private val GB18030: Charset = Charset.forName("GB18030")

    fun decodeText(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return when (bytes[0].toInt() and 0xFF) {
            0 -> fixLatin1(bytes.copyOfRange(1, bytes.size))
            1 -> decodeUtf16(bytes, 1, littleEndian = false)
            2 -> decodeUtf16(bytes, 1, littleEndian = true)
            3 -> String(bytes, 1, bytes.size - 1, StandardCharsets.UTF_8)
            else -> fixLatin1(bytes)
        }
    }

    /**
     * Heuristic for strings that were already decoded as Latin-1 and now
     * contain mojibake: decode the same bytes as GB18030 when it yields CJK.
     */
    fun looksLikeGbkMojibake(text: String): Boolean =
        text.any { it.code in 0x80..0xFF } &&
            tryDecodeGbk(text.toByteArray(StandardCharsets.ISO_8859_1)).any { it.code in 0x4E00..0x9FFF }

    fun fixMojibake(text: String): String {
        val gbk = tryDecodeGbk(text.toByteArray(StandardCharsets.ISO_8859_1))
        return if (gbk.any { it.code in 0x4E00..0x9FFF }) gbk else text
    }

    private fun fixLatin1(raw: ByteArray): String {
        if (raw.isEmpty()) return ""
        val gbk = tryDecodeGbk(raw)
        return if (gbk.any { it.code in 0x4E00..0x9FFF }) gbk else String(raw, StandardCharsets.ISO_8859_1)
    }

    private fun tryDecodeGbk(raw: ByteArray): String = try {
        String(raw, GB18030)
    } catch (e: Exception) {
        ""
    }

    private fun decodeUtf16(bytes: ByteArray, offset: Int, littleEndian: Boolean): String {
        val len = bytes.size - offset
        if (len < 2) return ""
        var start = offset
        val end = bytes.size
        val b0 = bytes[start].toInt() and 0xFF
        val b1 = bytes[start + 1].toInt() and 0xFF
        if ((b0 == 0xFF && b1 == 0xFE) || (b0 == 0xFE && b1 == 0xFF)) {
            start += 2
        }
        val decoded = String(
            bytes,
            start,
            end - start,
            if (littleEndian) StandardCharsets.UTF_16LE else StandardCharsets.UTF_16BE,
        )
        // Defensive: some writers keep a literal U+FEFF after the BOM bytes.
        return if (decoded.isNotEmpty() && decoded[0] == '\uFEFF') decoded.substring(1) else decoded
    }
}
