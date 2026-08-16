package com.sertum.player.ui.playback

import com.sertum.player.domain.playback.BitPerfectState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OutputMode { USB_EXCLUSIVE, STANDARD, BLUETOOTH }

data class PlaybackUiState(
    val trackTitle: String = "Sertum",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val outputMode: OutputMode = OutputMode.STANDARD,
    val bitPerfectState: BitPerfectState = BitPerfectState.NOT_APPLICABLE,
    val queue: List<String> = emptyList(),
)

/**
 * M4 placeholder holder; M2's PlaybackCoordinator and the AAudio backend are
 * wired into this object when playback integration lands.
 */
object PlaybackStateHolder {
    private val _state = MutableStateFlow(
        PlaybackUiState(
            trackTitle = "No track playing",
            artist = "—",
            album = "—",
        ),
    )
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun update(transform: (PlaybackUiState) -> PlaybackUiState) {
        _state.value = transform(_state.value)
    }
}
