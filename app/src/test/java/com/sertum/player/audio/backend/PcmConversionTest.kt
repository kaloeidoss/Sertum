package com.sertum.player.audio.backend

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmConversionTest {

    private fun floats(vararg values: Float): ByteArray =
        ByteBuffer.allocate(values.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { values.forEach { putFloat(it) } }
            .array()

    private fun packed24(vararg ints: Int): ByteArray {
        val out = ByteArray(ints.size * 3)
        var i = 0
        for (v in ints) {
            out[i++] = (v and 0xFF).toByte()
            out[i++] = ((v shr 8) and 0xFF).toByte()
            out[i++] = ((v shr 16) and 0xFF).toByte()
        }
        return out
    }

    @Test
    fun `silence stays silence`() {
        assertThat(PcmConversion.float32ToPacked24Le(floats(0f, -0f))).isEqualTo(packed24(0, 0))
    }

    @Test
    fun `full scale positive and negative clip to 24-bit bounds`() {
        assertThat(PcmConversion.float32ToPacked24Le(floats(1f))).isEqualTo(packed24(8_388_607))
        assertThat(PcmConversion.float32ToPacked24Le(floats(-1f))).isEqualTo(packed24(-8_388_608))
    }

    @Test
    fun `values beyond unit range are clamped`() {
        assertThat(PcmConversion.float32ToPacked24Le(floats(2.5f))).isEqualTo(packed24(8_388_607))
        assertThat(PcmConversion.float32ToPacked24Le(floats(-2.5f))).isEqualTo(packed24(-8_388_608))
    }

    @Test
    fun `exact powers and midscale map to exact 24-bit values`() {
        assertThat(PcmConversion.float32ToPacked24Le(floats(0.5f))).isEqualTo(packed24(4_194_304))
        assertThat(PcmConversion.float32ToPacked24Le(floats(0.25f))).isEqualTo(packed24(2_097_152))
        assertThat(PcmConversion.float32ToPacked24Le(floats(-0.5f))).isEqualTo(packed24(-4_194_304))
    }

    @Test
    fun `round trip is bit-exact for representative 24-bit integers`() {
        val representatives = listOf(
            0, 1, -1, 2, -2,
            8_388_607, -8_388_608,
            4_194_303, -4_194_303,
            1_234_567, -1_234_567,
            7_654_321, -7_654_321,
        )
        for (value in representatives) {
            val floatValue = value / (8_388_608.0f)
            val converted = PcmConversion.float32ToPacked24Le(floats(floatValue))
            assertThat(converted).isEqualTo(packed24(value))
        }
    }

    @Test
    fun `multiple samples keep channel order`() {
        val inBytes = floats(1f, -1f, 0.5f)
        assertThat(PcmConversion.float32ToPacked24Le(inBytes))
            .isEqualTo(packed24(8_388_607, -8_388_608, 4_194_304))
    }
}
