package com.sertum.player.data.scan

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioExtensionsTest {

    @Test
    fun `all PRD 7_4 formats are accepted case-insensitively`() {
        val supported = listOf(
            "a.FLAC", "b.Wav", "c.AIFF", "d.aif", "e.M4A", "f.mp3", "g.AAC", "h.ogg", "i.opus",
        )
        supported.forEach { assertThat(AudioExtensions.isSupported(it)).isTrue() }
    }

    @Test
    fun `non-audio and unsupported extensions are rejected`() {
        val rejected = listOf(
            "cover.jpg", "song.txt", "no-extension", "song.dsf", "song.ape", "song.cue", "song.mkv",
        )
        rejected.forEach { assertThat(AudioExtensions.isSupported(it)).isFalse() }
    }

    @Test
    fun `extension set matches the PRD whitelist`() {
        assertThat(AudioExtensions.EXTENSIONS).containsExactly(
            "flac", "wav", "aiff", "aif", "m4a", "mp3", "aac", "ogg", "opus",
        )
    }
}
