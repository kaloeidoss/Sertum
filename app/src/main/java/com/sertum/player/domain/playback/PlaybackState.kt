package com.sertum.player.domain.playback

/** Lifecycle of an output backend instance, mirrored by the UI state machine. */
enum class PlaybackState {
    IDLE,
    OPENED,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR,
}
