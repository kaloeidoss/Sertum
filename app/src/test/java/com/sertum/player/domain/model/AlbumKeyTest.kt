package com.sertum.player.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlbumKeyTest {

    @Test
    fun `album artist wins over artist`() {
        assertThat(
            AlbumKey.resolve("Title", "AlbumArtist", "TrackArtist", null),
        ).isEqualTo(AlbumKey("Title", "AlbumArtist"))
    }

    @Test
    fun `artist fallback when album artist is missing`() {
        assertThat(
            AlbumKey.resolve("Title", null, "Artist", null),
        ).isEqualTo(AlbumKey("Title", "Artist"))
    }

    @Test
    fun `folder fallback when tags are missing`() {
        assertThat(
            AlbumKey.resolve(null, null, null, "/music/AlbumA"),
        ).isEqualTo(AlbumKey("__FOLDER__/music/AlbumA", "__FOLDER__/music/AlbumA"))
    }

    @Test
    fun `unknown fallback when everything is missing`() {
        assertThat(AlbumKey.resolve(null, null, null, null)).isEqualTo(AlbumKey("__UNKNOWN__", "__UNKNOWN__"))
    }

    @Test
    fun `blank strings are treated as missing`() {
        assertThat(AlbumKey.resolve("   ", " ", "Artist", null)).isEqualTo(AlbumKey("__UNKNOWN__", "__UNKNOWN__"))
    }
}
