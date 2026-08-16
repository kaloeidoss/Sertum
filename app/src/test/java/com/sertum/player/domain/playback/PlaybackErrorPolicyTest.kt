package com.sertum.player.domain.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackErrorPolicyTest {

    @Test
    fun `first two failures skip without notifying`() {
        val policy = PlaybackErrorPolicy()
        val first = policy.onTrackFailed()
        assertThat(first.skipToNext).isTrue()
        assertThat(first.notifyUser).isFalse()
        val second = policy.onTrackFailed()
        assertThat(second.skipToNext).isTrue()
        assertThat(second.notifyUser).isFalse()
        assertThat(policy.consecutiveFailures).isEqualTo(2)
    }

    @Test
    fun `third consecutive failure notifies exactly once`() {
        val policy = PlaybackErrorPolicy()
        policy.onTrackFailed()
        policy.onTrackFailed()
        val third = policy.onTrackFailed()
        assertThat(third.notifyUser).isTrue()
    }

    @Test
    fun `failures beyond threshold stay silent`() {
        val policy = PlaybackErrorPolicy()
        repeat(3) { policy.onTrackFailed() }
        val fourth = policy.onTrackFailed()
        assertThat(fourth.skipToNext).isTrue()
        assertThat(fourth.notifyUser).isFalse()
        assertThat(policy.consecutiveFailures).isEqualTo(4)
    }

    @Test
    fun `successful track start resets the streak`() {
        val policy = PlaybackErrorPolicy()
        repeat(2) { policy.onTrackFailed() }
        policy.onTrackStarted()
        assertThat(policy.consecutiveFailures).isEqualTo(0)
        val first = policy.onTrackFailed()
        assertThat(first.notifyUser).isFalse()
    }

    @Test
    fun `custom threshold is honored`() {
        val policy = PlaybackErrorPolicy(consecutiveFailureThreshold = 5)
        repeat(4) { assertThat(policy.onTrackFailed().notifyUser).isFalse() }
        assertThat(policy.onTrackFailed().notifyUser).isTrue()
    }

    @Test
    fun `threshold must be positive`() {
        val error = runCatching { PlaybackErrorPolicy(0) }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}
