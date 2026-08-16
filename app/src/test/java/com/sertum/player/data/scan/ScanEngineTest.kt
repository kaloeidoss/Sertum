package com.sertum.player.data.scan

import com.google.common.truth.Truth.assertThat
import com.sertum.player.data.covers.CoverChoice
import com.sertum.player.data.covers.CoverOrigin
import com.sertum.player.data.covers.CoverResolver
import org.junit.Test

class ScanEngineTest {

    private val engine = ScanEngine()

    @Test
    fun `normalizes windows separators and trailing slash`() {
        assertThat(engine.normalizePath("  /music\\Album/  ")).isEqualTo("/music/Album")
    }

    @Test
    fun `duplicate paths collapse to the first candidate`() {
        val first = ScanCandidate("uri://1", "/music/a.flac", 10, 1)
        val duplicate = ScanCandidate("uri://2", "\\music\\a.flac", 99, 2)
        val plan = engine.plan(emptyList(), listOf(first, duplicate))
        assertThat(plan.toAdd).containsExactly(first)
    }

    @Test
    fun `new tracks are added and changed tracks are updated`() {
        val existing = listOf(
            ExistingTrack("u1", "/music/a.flac", 10, 1),
            ExistingTrack("u2", "/music/b.flac", 20, 2),
        )
        val aChanged = ScanCandidate("u1", "/music/a.flac", 11, 3)
        val bSame = ScanCandidate("u2", "/music/b.flac", 20, 2)
        val cNew = ScanCandidate("u3", "/music/c.flac", 30, 4)
        val plan = engine.plan(existing, listOf(aChanged, bSame, cNew))
        assertThat(plan.toAdd).containsExactly(cNew)
        assertThat(plan.toUpdate).containsExactly(aChanged)
        assertThat(plan.toDelete).isEmpty()
    }

    @Test
    fun `missing files become orphans`() {
        val existing = listOf(
            ExistingTrack("u1", "/music/a.flac", 10, 1),
            ExistingTrack("u2", "/music/b.flac", 20, 2),
        )
        val plan = engine.plan(existing, listOf(ScanCandidate("u1", "/music/a.flac", 10, 1)))
        assertThat(plan.toDelete).containsExactly(existing[1])
    }

    @Test
    fun `empty input produces empty plan`() {
        val plan = engine.plan(emptyList(), emptyList())
        assertThat(plan.toAdd).isEmpty()
        assertThat(plan.toUpdate).isEmpty()
        assertThat(plan.toDelete).isEmpty()
    }
}

class CoverResolverTest {

    @Test
    fun `user cover wins over embedded and folder`() {
        val choice = CoverResolver.resolve(user = "u.jpg", embedded = "e.jpg", folder = "f.jpg")
        assertThat(choice).isEqualTo(CoverChoice("u.jpg", CoverOrigin.USER))
    }

    @Test
    fun `embedded wins over folder`() {
        assertThat(CoverResolver.resolve(null, "e.jpg", "f.jpg"))
            .isEqualTo(CoverChoice("e.jpg", CoverOrigin.EMBEDDED))
    }

    @Test
    fun `folder image is used when nothing else exists`() {
        assertThat(CoverResolver.resolve(null, null, "f.jpg"))
            .isEqualTo(CoverChoice("f.jpg", CoverOrigin.FOLDER))
    }

    @Test
    fun `placeholder is the final fallback`() {
        assertThat(CoverResolver.resolve(null, null, null))
            .isEqualTo(CoverChoice(CoverResolver.PLACEHOLDER_REF, CoverOrigin.PLACEHOLDER))
    }

    @Test
    fun `removing a user cover resumes the chain below it`() {
        assertThat(CoverResolver.resolveAfterUserRemoval("e.jpg", null))
            .isEqualTo(CoverChoice("e.jpg", CoverOrigin.EMBEDDED))
    }
}
