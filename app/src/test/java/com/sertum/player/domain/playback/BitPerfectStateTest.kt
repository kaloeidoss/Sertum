package com.sertum.player.domain.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BitPerfectStateTest {

    @Test
    fun `hardware volume is always intact`() {
        assertThat(VolumePolicy(true, false, 0.5f).bitPerfectState).isEqualTo(BitPerfectState.INTACT)
        assertThat(VolumePolicy(true, true, 0.3f).bitPerfectState).isEqualTo(BitPerfectState.INTACT)
    }

    @Test
    fun `locked software volume is intact`() {
        assertThat(VolumePolicy(false, false, 0.3f).bitPerfectState).isEqualTo(BitPerfectState.INTACT)
    }

    @Test
    fun `software 100 percent is intact`() {
        assertThat(VolumePolicy(false, true, 1.0f).bitPerfectState).isEqualTo(BitPerfectState.INTACT)
    }

    @Test
    fun `software below 100 percent is degraded`() {
        assertThat(VolumePolicy(false, true, 0.7f).bitPerfectState).isEqualTo(BitPerfectState.DEGRADED)
        assertThat(VolumePolicy(false, true, 0f).bitPerfectState).isEqualTo(BitPerfectState.DEGRADED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `volume outside range is rejected`() {
        VolumePolicy(false, true, 1.2f)
    }
}
