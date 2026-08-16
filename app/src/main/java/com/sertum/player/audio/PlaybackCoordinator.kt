package com.sertum.player.audio

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.sertum.player.domain.playback.QueueEngine
import com.sertum.player.domain.playback.RepeatMode
import com.sertum.player.domain.playback.ResumePositionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Current decoded audio parameters, used by the UI and for rate-switch evidence. */
data class AudioStreamInfo(
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val channelCount: Int = 0,
)

/**
 * Wires QueueEngine + Media3 + resume positions together.
 * Gapless: consecutive MediaItems are handed to Media3 in one `setMediaSources`
 * call; Media3 applies its built-in gapless transition for FLAC/ALAC/WAV.
 * Sample-rate switching is delegated to the active AudioSink backend:
 * Media3 reconfigures the sink with the next track's Format, and the backend
 * re-opens its native stream at the new rate before playback continues.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackCoordinator(
    private val engine: PlayerEngine,
    private val resumeStore: ResumePositionStore,
) {

    private val _audioInfo = MutableStateFlow(AudioStreamInfo())
    val audioInfo: StateFlow<AudioStreamInfo> = _audioInfo.asStateFlow()

    private val queue = QueueEngine()
    private var currentUri: Uri? = null
    private var currentTrackId: Long = 0L

    init {
        engine.player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    currentUri = mediaItem.localConfiguration?.uri
                }
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    queue.next()
                }
            }
        })
        engine.player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                audioFormat: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                _audioInfo.value = AudioStreamInfo(
                    sampleRate = audioFormat.sampleRate,
                    bitDepth = when (audioFormat.pcmEncoding) {
                        C.ENCODING_PCM_24BIT -> 24
                        C.ENCODING_PCM_32BIT -> 32
                        else -> 16
                    },
                    channelCount = audioFormat.channelCount,
                )
            }
        })
    }

    fun playTracks(trackIds: List<Long>, uris: List<Uri>, startIndex: Int) {
        require(trackIds.size == uris.size) { "trackIds and uris must have the same size" }
        if (trackIds.isEmpty()) return
        queue.setQueue(trackIds, startIndex)
        currentTrackId = trackIds[startIndex]
        val resume = resumeStore.get(currentTrackId) ?: 0L
        engine.player.setMediaItems(
            uris.map { MediaItem.fromUri(it) },
            startIndex,
            resume,
        )
        engine.player.prepare()
        engine.player.play()
    }

    fun saveResumePosition() {
        if (queue.size == 0) return
        resumeStore.put(currentTrackId, engine.player.currentPosition.coerceAtLeast(0L))
    }

    fun pause() {
        saveResumePosition()
        engine.player.pause()
    }

    fun release() {
        saveResumePosition()
        engine.release()
    }

    fun skipToNext() {
        if (queue.next() == null) {
            engine.player.seekToNextMediaItem()
        } else {
            engine.player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        queue.previous()
        engine.player.seekToPreviousMediaItem()
    }

    fun setRepeat(mode: RepeatMode) {
        queue.setRepeat(mode)
        engine.player.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }
}
