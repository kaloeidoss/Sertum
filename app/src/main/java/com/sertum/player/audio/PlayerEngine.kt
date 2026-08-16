package com.sertum.player.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes as M3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sertum.player.audio.backend.RoutedAudioOutputProvider

/**
 * Owns the Media3 player. Audio output is routed by [router]:
 * system AudioTrack by default, AAudio EXCLUSIVE when the user selects
 * USB-exclusive mode (ADR-0001).
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerEngine(context: Context) {

    val router = RoutedAudioOutputProvider(context.applicationContext)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context))
        .setAudioOutputProvider(router)
        .setAudioAttributes(
            M3AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    fun playUri(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
        router.release()
    }
}
