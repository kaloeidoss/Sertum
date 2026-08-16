package com.sertum.player.data.covers

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoverResolverTest {

    @Test
    fun `user cover beats embedded and folder`() {
        val choice = CoverResolver.resolve("user.jpg", "embedded.jpg", "folder.jpg")
        assertThat(choice).isEqualTo(CoverChoice("user.jpg", CoverOrigin.USER))
    }

    @Test
    fun `embedded beats folder`() {
        val choice = CoverResolver.resolve(null, "embedded.jpg", "folder.jpg")
        assertThat(choice).isEqualTo(CoverChoice("embedded.jpg", CoverOrigin.EMBEDDED))
    }

    @Test
    fun `folder beats placeholder`() {
        val choice = CoverResolver.resolve(null, null, "folder.jpg")
        assertThat(choice).isEqualTo(CoverChoice("folder.jpg", CoverOrigin.FOLDER))
    }

    @Test
    fun `blank refs fall through to placeholder`() {
        val choice = CoverResolver.resolve("  ", "", "  ")
        assertThat(choice).isEqualTo(CoverChoice(CoverResolver.PLACEHOLDER_REF, CoverOrigin.PLACEHOLDER))
    }

    @Test
    fun `removal resumes the chain below the user cover`() {
        val choice = CoverResolver.resolveAfterUserRemoval(embedded = "embedded.jpg", folder = "folder.jpg")
        assertThat(choice).isEqualTo(CoverChoice("embedded.jpg", CoverOrigin.EMBEDDED))
    }
}
