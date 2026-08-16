package com.sertum.player.data.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.charset.Charset

class GbkId3FixerTest {

    private val gb = Charset.forName("GB18030")

    @Test
    fun `encoding 0 with GBK bytes decodes to Chinese`() {
        val text = "花环播放器"
        val payload = byteArrayOf(0) + text.toByteArray(gb)
        assertThat(GbkId3Fixer.decodeText(payload)).isEqualTo(text)
    }

    @Test
    fun `encoding 0 with pure ASCII stays ASCII`() {
        val payload = byteArrayOf(0) + "Sertum".toByteArray(Charsets.US_ASCII)
        assertThat(GbkId3Fixer.decodeText(payload)).isEqualTo("Sertum")
    }

    @Test
    fun `encoding 3 UTF-8 decodes`() {
        val payload = byteArrayOf(3) + "花环播放器".toByteArray(Charsets.UTF_8)
        assertThat(GbkId3Fixer.decodeText(payload)).isEqualTo("花环播放器")
    }

    @Test
    fun `encoding 1 UTF-16 BE with BOM decodes`() {
        val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val payload = byteArrayOf(1) + bom + "花环".toByteArray(Charsets.UTF_16BE)
        assertThat(GbkId3Fixer.decodeText(payload)).isEqualTo("花环")
    }

    @Test
    fun `mojibake heuristic upgrades GBK-looking Latin-1 text`() {
        val mojibake = String("花环播放器".toByteArray(gb), Charsets.ISO_8859_1)
        assertThat(GbkId3Fixer.looksLikeGbkMojibake(mojibake)).isTrue()
        assertThat(GbkId3Fixer.fixMojibake(mojibake)).isEqualTo("花环播放器")
    }

    @Test
    fun `plain accented Latin-1 is not rewritten as GBK`() {
        val cafe = "café"
        assertThat(GbkId3Fixer.looksLikeGbkMojibake(cafe)).isFalse()
        assertThat(GbkId3Fixer.fixMojibake(cafe)).isEqualTo(cafe)
    }
}
