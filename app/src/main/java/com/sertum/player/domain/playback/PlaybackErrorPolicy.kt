package com.sertum.player.domain.playback

/**
 * PRD 7.13 / US-9 AC2: consecutive playback failures must skip to the next
 * track and notify the user exactly once per streak of >= 3 failures.
 * Pure state machine; the player layer performs the skip and shows the prompt.
 */
data class PlaybackFailureDecision(
    val skipToNext: Boolean,
    val notifyUser: Boolean,
)

class PlaybackErrorPolicy(
    private val consecutiveFailureThreshold: Int = DEFAULT_CONSECUTIVE_FAILURE_THRESHOLD,
) {
    init {
        require(consecutiveFailureThreshold > 0) { "threshold must be positive" }
    }

    var consecutiveFailures: Int = 0
        private set

    /** Called when the current track fails. Returns the action the player must take. */
    fun onTrackFailed(): PlaybackFailureDecision {
        consecutiveFailures += 1
        return PlaybackFailureDecision(
            skipToNext = true,
            notifyUser = consecutiveFailures == consecutiveFailureThreshold,
        )
    }

    /** Called when a track starts (or recovers) successfully. Resets the streak. */
    fun onTrackStarted() {
        consecutiveFailures = 0
    }

    companion object {
        const val DEFAULT_CONSECUTIVE_FAILURE_THRESHOLD = 3
    }
}
