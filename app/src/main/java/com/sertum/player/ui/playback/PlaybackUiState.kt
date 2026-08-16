package com.sertum.player.ui.playback

import com.sertum.player.domain.playback.BitPerfectState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OutputMode { USB_EXCLUSIVE, STANDARD, BLUETOOTH }

data class PlaybackUiState(
    val trackTitle: String = "",
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
 * UI projection of [com.sertum.player.audio.PlaybackCoordinator].
 * Screens map the blank title to the localized "no track playing" string.
 */
object PlaybackStateHolder {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun update(transform: (PlaybackUiState) -> PlaybackUiState) {
        _state.value = transform(_state.value)
    }
}
