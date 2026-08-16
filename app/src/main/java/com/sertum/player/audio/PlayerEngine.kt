package com.sertum.player.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes as M3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Owns the Media3 player for the standard system output path.
 * The USB-exclusive path uses AaudioExclusiveBackend and never routes through here.
 */
class PlayerEngine(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context))
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
    }
}
