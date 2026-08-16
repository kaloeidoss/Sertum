package com.sertum.player.spike1

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Spike-1: system direct playback path.
 *
 * Plays a 1 kHz sine tone through the Android system audio stack with the
 * lowest-latency flags available from Java AudioTrack. Evidence about whether
 * the platform used a direct/AAudio/MMAP path (and whether any resampling or
 * effects were applied) is collected externally via
 * `dumpsys media.audio_flinger`.
 */
class Spike1AudioEngine {

    companion object {
        private const val TAG = "SertumSpike"
        const val TONE_SECONDS = 3
        val RATES = intArrayOf(44_100, 48_000, 88_200, 96_000, 176_400, 192_000)
    }

    /** Runs the full rate/depth matrix. Returns a summary line for logs. */
    fun runMatrix(onProgress: (String) -> Unit = { Log.i(TAG, it) }): Int {
        var played = 0
        for (rate in RATES) {
            for (bits in intArrayOf(16, 24)) {
                val ok = playTone(rate, bits)
                onProgress("tone rate=$rate bits=$bits ok=$ok")
                if (ok) played++
                Thread.sleep(200)
            }
        }
        return played
    }

    private fun playTone(sampleRate: Int, bitDepth: Int): Boolean {
        if (bitDepth == 24 && android.os.Build.VERSION.SDK_INT < 31) {
            Log.w(TAG, "24-bit packed requires API 31+, skipping")
            return false
        }
        val channel = AudioFormat.CHANNEL_OUT_STEREO
        val encoding = when (bitDepth) {
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            else -> return false
        }
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuffer <= 0) {
            Log.w(TAG, "minBuffer<=0 rate=$sampleRate bits=$bitDepth")
            return false
        }
        val track = try {
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channel)
                    .build(),
                minBuffer * 2,
                AudioTrack.MODE_STREAM,
                0,
            ).apply {
                // API 36 removed setPerformanceMode(); the requested mode is only
                // observable via getPerformanceMode() and audio_flinger dumps.
                setPlaybackParams(
                    playbackParams
                        .setSpeed(1.0f)
                        .setPitch(1.0f),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "create failed rate=$sampleRate bits=$bitDepth: ${e.message}")
            return false
        }

        Log.i(
            TAG,
            "track created rate=$sampleRate bits=$bitDepth enc=$encoding " +
                "bufferFrames=${track.bufferSizeInFrames} " +
                "performanceMode=${track.performanceMode}",
        )

        val frames = sampleRate * TONE_SECONDS
        val samplesPerFrame = 2
        val totalSamples = frames * samplesPerFrame
        val bytesPerSample = if (bitDepth == 16) 2 else 3
        val pcm = ByteArray(totalSamples * bytesPerSample)

        // 1 kHz sine at 20% amplitude (-14 dBFS), interleaved stereo.
        val amplitude = when (bitDepth) {
            16 -> (Short.MAX_VALUE * 0.2).toInt()
            else -> ((1 shl 23) * 0.2).toInt()
        }
        for (i in 0 until frames) {
            val value = (amplitude * kotlin.math.sin(2.0 * Math.PI * 1000.0 * i / sampleRate)).toInt()
            if (bitDepth == 16) {
                val s = value.toShort()
                val off = i * 4
                pcm[off] = (s.toInt() and 0xFF).toByte()
                pcm[off + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                pcm[off + 2] = pcm[off]
                pcm[off + 3] = pcm[off + 1]
            } else {
                val v = value and 0xFFFFFF
                val off = i * 6
                pcm[off] = (v and 0xFF).toByte()
                pcm[off + 1] = ((v shr 8) and 0xFF).toByte()
                pcm[off + 2] = ((v shr 16) and 0xFF).toByte()
                pcm[off + 3] = pcm[off]
                pcm[off + 4] = pcm[off + 1]
                pcm[off + 5] = pcm[off + 2]
            }
        }

        return try {
            track.play()
            var written = 0
            while (written < pcm.size) {
                val n = track.write(pcm, written, pcm.size - written)
                if (n <= 0) break
                written += n
            }
            Thread.sleep(300)
            track.stop()
            track.release()
            true
        } catch (e: Exception) {
            Log.w(TAG, "play failed rate=$sampleRate bits=$bitDepth: ${e.message}")
            try { track.release() } catch (_: Exception) {}
            false
        }
    }
}
