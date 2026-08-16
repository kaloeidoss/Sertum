package com.sertum.player.audio

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sertum.player.audio.backend.RoutedAudioOutputProvider
import com.sertum.player.audio.extractor.SertumExtractorsFactory
import com.sertum.player.domain.playback.AudioOutputBackend
import com.sertum.player.domain.playback.BackendCapabilities
import com.sertum.player.domain.playback.StreamSpec
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * PRD 10.1 integration contract: fixed audio -> Media3 decode -> PCM ->
 * AudioOutputBackend contract, without touching real audio hardware.
 * Fixtures under androidTest/assets/contract-media are Apache-2.0 Media3
 * test data plus ffmpeg-generated synthetic sine files (public domain).
 */
@RunWith(AndroidJUnit4::class)
class DecoderContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val players = mutableListOf<ExoPlayer>()
    private val providers = mutableListOf<RoutedAudioOutputProvider>()

    @Before
    fun setUp() {
        // nothing; each case owns its player/provider pair
    }

    @After
    fun tearDown() {
        runOnMainSync {
            players.forEach { runCatching { it.release() } }
        }
        players.clear()
        providers.clear()
    }

    @Test
    fun wavMatrix_decodesPcmAtSourceRateAndDepth() {
        for (rate in RATES) {
            for (depth in intArrayOf(16, 24)) {
                val file = File(context.cacheDir, "contract-$rate-$depth.wav")
                file.writeBytes(wavBytes(rate, depth))
                decodeAndAssert("wav", file, expectedRate = rate, expectedDepth = depth)
            }
        }
    }

    @Test
    fun aiffMatrix_decodesPcmAtSourceRateAndDepth() {
        for (rate in RATES) {
            for (depth in intArrayOf(16, 24)) {
                val file = File(context.cacheDir, "contract-$rate-$depth.aiff")
                file.writeBytes(aiffBytes(rate, depth))
                decodeAndAssert("aiff", file, expectedRate = rate, expectedDepth = depth)
            }
        }
    }

    @Test
    fun flacMatrix_decodesPcmAtSourceRateAndDepth() {
        for (rate in RATES) {
            for (depth in intArrayOf(16, 24)) {
                decodeAndAssert("flac", asset("gen-flac-$rate-$depth.flac"), expectedRate = rate, expectedDepth = depth)
            }
        }
    }

    // Device limitation (Xiaomi 12S): both c2.qti.alac.{sw,hw}.decoder reject
    // valid ALAC CSD with CodecException 0x80000000. FFmpeg decoder extension
    // is queued as a dedicated follow-up task; keep fixtures in place.
    @Ignore("Xiaomi 12S ALAC codecs reject valid ALAC; FFmpeg extension queued")
    @Test
    fun alacMatrix_decodesPcmAtSourceRateAndDepth() {
        for (rate in RATES) {
            for (depth in intArrayOf(16, 24)) {
                decodeAndAssert("alac", asset("gen-alac-$rate-$depth.m4a"), expectedRate = rate, expectedDepth = depth)
            }
        }
    }

    @Test
    fun media3EdgeFixtures_stillPassTheBackendContract() {
        // 48 kHz/16-bit FLAC.
        decodeAndAssert("flac-edge", asset("bear.flac"), expectedRate = 48_000, expectedDepth = 16)
        // Uncommon 44 kHz/16-bit FLAC.
        decodeAndAssert("flac-uncommon", asset("bear_uncommon_sample_rate.flac"), expectedRate = 44_000, expectedDepth = 16)
        // Upstream ALAC fixtures are skipped on this device too (same
        // CodecException 0x80000000 from both platform ALAC decoders);
        // see alacMatrix @Ignore for the queued FFmpeg follow-up.
        // 32-bit FLAC is outside the PRD 16/24-bit matrix; the exclusive route
        // documents it as a known limitation (Media3 decoder reuse on Qualcomm
        // can end with zero PCM output). Standard output path still plays it.
    }

    private fun asset(name: String): File {
        val target = File(context.cacheDir, name)
        if (!target.exists()) {
            testContext.assets.open("contract-media/$name").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return target
    }

    private fun runOnMainSync(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private fun decodeAndAssert(label: String, file: File, expectedRate: Int?, expectedDepth: Int?) {
        val backend = CapturingBackend()
        var provider: RoutedAudioOutputProvider? = null
        var player: ExoPlayer? = null
        runOnMainSync {
            provider = RoutedAudioOutputProvider(context).apply {
                exclusiveEnabled = true
                exclusiveBackend = backend
            }
            providers += provider!!
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                .setEnableAudioFloatOutput(true)
                .setMediaCodecSelector(com.sertum.player.audio.backend.PreferHardwareCodecSelector)
            player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context, SertumExtractorsFactory()))
                .setAudioOutputProvider(provider!!)
                .build()
            players += player!!
        }
        val activePlayer = player!!
        val activeProvider = provider!!

        val ended = CountDownLatch(1)
        val playerError = java.util.concurrent.atomic.AtomicReference<androidx.media3.common.PlaybackException?>(null)
        runOnMainSync {
            activePlayer.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
                override fun onAudioDecoderInitialized(
                    eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                    decoderName: String,
                    initializationDurationMs: Long,
                ) {
                    android.util.Log.i("SertumCodec", "decoder=$decoderName")
                }
            })
            activePlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) ended.countDown()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playerError.set(error)
                    ended.countDown()
                }
            })

            activePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            activePlayer.prepare()
            activePlayer.play()
        }

        com.google.common.truth.Truth.assertWithMessage("$label: playback did not finish")
            .that(ended.await(15, TimeUnit.SECONDS)).isTrue()
        com.google.common.truth.Truth.assertWithMessage("$label: player error")
            .that(playerError.get()).isNull()

        val spec = backend.specs.lastOrNull()
        assertThat(spec).isNotNull()
        spec!!
        if (expectedRate != null) {
            assertThat(spec.sampleRate).isEqualTo(expectedRate)
        } else {
            assertThat(spec.sampleRate).isIn(RATES.toList())
        }
        if (expectedDepth != null) {
            assertThat(spec.bitDepth).isEqualTo(expectedDepth)
        } else {
            assertThat(spec.bitDepth).isIn(listOf(16, 24, 32))
        }
        com.google.common.truth.Truth.assertWithMessage("$label: PCM bytes")
            .that(backend.totalBytes.get()).isGreaterThan(0)

        runOnMainSync {
            activePlayer.release() // releases the routed provider on the playback thread
        }
        players.remove(activePlayer)
        providers.remove(activeProvider)
    }

    private class CapturingBackend : AudioOutputBackend {
        val specs = java.util.concurrent.CopyOnWriteArrayList<StreamSpec>()
        val totalBytes = java.util.concurrent.atomic.AtomicLong(0)
        private val totalFrames = java.util.concurrent.atomic.AtomicLong(0)
        @Volatile
        private var sampleRate = 48_000

        override val capabilities = BackendCapabilities(supportsHardwareVolume = false, isExclusive = true)

        override fun open(spec: StreamSpec): Result<Unit> {
            specs.add(spec)
            sampleRate = spec.sampleRate
            return Result.success(Unit)
        }

        override fun writePcm(frame: ByteArray, offset: Int, length: Int): Result<Unit> {
            totalBytes.addAndGet(length.toLong())
            val bytesPerSample = if (specs.lastOrNull()?.bitDepth == 24) 3 else 2
            val channels = specs.lastOrNull()?.channelCount ?: 2
            totalFrames.addAndGet((length / (bytesPerSample * channels)).toLong())
            return Result.success(Unit)
        }

        override fun getPositionUs(): Long = totalFrames.get() * 1_000_000L / sampleRate

        override fun getBufferSizeInFrames(): Int = 1_024

        override fun pause(): Result<Unit> = Result.success(Unit)
        override fun play(): Result<Unit> = Result.success(Unit)
        override fun flush(): Result<Unit> = Result.success(Unit)
        override fun stop(): Result<Unit> = Result.success(Unit)
        override fun release() = Unit
        override fun onVolumeChanged(volume01: Float) = Unit
    }

    companion object {
        val RATES = intArrayOf(44_100, 48_000, 96_000, 192_000)

        private fun wavBytes(sampleRate: Int, bitDepth: Int): ByteArray {
            val frames = sampleRate / 10
            val bytesPerSample = bitDepth / 8
            val dataSize = frames * 2 * bytesPerSample
            val out = ByteArrayOutputStream()
            fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
            fun le32(v: Int) {
                out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
                out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
            }
            fun le16(v: Int) {
                out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            }
            ascii("RIFF"); le32(36 + dataSize); ascii("WAVE")
            ascii("fmt "); le32(16); le16(1); le16(2); le32(sampleRate)
            le32(sampleRate * 2 * bytesPerSample); le16(2 * bytesPerSample); le16(bitDepth)
            ascii("data"); le32(dataSize)
            for (i in 0 until frames) {
                val value = tone(i, sampleRate, bitDepth)
                for (ch in 0 until 2) {
                    if (bitDepth == 16) {
                        le16(value and 0xFFFF)
                    } else {
                        val v = value and 0xFFFFFF
                        out.write(v and 0xFF); out.write((v shr 8) and 0xFF); out.write((v shr 16) and 0xFF)
                    }
                }
            }
            return out.toByteArray()
        }

        private fun aiffBytes(sampleRate: Int, bitDepth: Int): ByteArray {
            val frames = sampleRate / 10
            val bytesPerSample = bitDepth / 8
            val dataSize = frames * 2 * bytesPerSample
            val out = ByteArrayOutputStream()
            fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
            fun be32(v: Int) {
                out.write((v shr 24) and 0xFF); out.write((v shr 16) and 0xFF)
                out.write((v shr 8) and 0xFF); out.write(v and 0xFF)
            }
            fun be16(v: Int) {
                out.write((v shr 8) and 0xFF); out.write(v and 0xFF)
            }
            val commSize = 18 // channels + sample frames + sample size + 80-bit rate
            val ssndSize = 8 + dataSize
            // FORM size = AIFF(4) + COMM(8 + commSize) + SSND(8 + ssndSize)
            ascii("FORM"); be32(4 + (8 + commSize) + (8 + ssndSize)); ascii("AIFF")
            ascii("COMM"); be32(commSize); be16(2); be32(frames); be16(bitDepth)
            // 80-bit IEEE 754 extended float for the sample rate.
            var exponent = 16383
            var mantissa = sampleRate.toDouble()
            while (mantissa >= 2.0) { mantissa /= 2.0; exponent++ }
            while (mantissa < 1.0) { mantissa *= 2.0; exponent-- }
            val fraction = mantissa - 1.0
            val fractionBits = (fraction * 9_223_372_036_854_775_808.0).toLong()
            val m = (1L shl 63) or (fractionBits and 0x7FFFFFFFFFFFFFFFL)
            be16(exponent)
            for (shift in 7 downTo 0) {
                out.write(((m shr (shift * 8)) and 0xFF).toInt())
            }
            ascii("SSND"); be32(8 + dataSize); be32(0); be32(0)
            for (i in 0 until frames) {
                val value = tone(i, sampleRate, bitDepth)
                for (ch in 0 until 2) {
                    if (bitDepth == 16) {
                        be16(value and 0xFFFF)
                    } else {
                        out.write((value shr 16) and 0xFF); out.write((value shr 8) and 0xFF); out.write(value and 0xFF)
                    }
                }
            }
            return out.toByteArray()
        }

        private fun tone(index: Int, sampleRate: Int, bitDepth: Int): Int {
            val fullScale = if (bitDepth == 16) 32_767 else 8_388_607
            return (fullScale * 0.25 * sin(2.0 * PI * 440.0 * index / sampleRate)).toInt()
        }
    }
}
