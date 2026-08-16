package com.sertum.player.audio.backend

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Float PCM (native-endian IEEE-754) to packed 24-bit little-endian PCM.
 * Every 24-bit integer is exactly representable in float32, so 24-bit source
 * material survives the conversion bit-exactly.
 */
object PcmConversion {

    fun float32ToPacked24Le(floatPcm: ByteArray): ByteArray {
        val floatView = ByteBuffer.wrap(floatPcm).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val out = ByteArray(floatView.remaining() * 3)
        var outIndex = 0
        while (floatView.hasRemaining()) {
            val sample = floatView.get().coerceIn(-1f, 1f)
            val scaled = when {
                sample >= 1f -> MAX_24BIT
                sample <= -1f -> -MAX_24BIT - 1
                else -> (sample * (MAX_24BIT + 1)).toInt()
            }
            out[outIndex++] = (scaled and 0xFF).toByte()
            out[outIndex++] = ((scaled shr 8) and 0xFF).toByte()
            out[outIndex++] = ((scaled shr 16) and 0xFF).toByte()
        }
        return out
    }

    fun float32ToPacked16Le(floatPcm: ByteArray): ByteArray {
        val floatView = ByteBuffer.wrap(floatPcm).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val out = ByteArray(floatView.remaining() * 2)
        var outIndex = 0
        while (floatView.hasRemaining()) {
            val sample = floatView.get().coerceIn(-1f, 1f)
            val scaled = when {
                sample >= 1f -> MAX_16BIT
                sample <= -1f -> -MAX_16BIT - 1
                else -> (sample * (MAX_16BIT + 1)).toInt()
            }
            out[outIndex++] = (scaled and 0xFF).toByte()
            out[outIndex++] = ((scaled shr 8) and 0xFF).toByte()
        }
        return out
    }

    private const val MAX_16BIT = 32_767
    private const val MAX_24BIT = 8_388_607
}
