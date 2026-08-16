package com.sertum.player.data.diagnostics

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiagnosticsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val baseEpoch = 1_752_614_400_000L // 2025-07-15T00:00:00Z (arbitrary stable day)

    private fun store(
        now: Long,
        retentionDays: Int = DiagnosticsStore.DEFAULT_RETENTION_DAYS,
        dir: File = tmp.root,
    ) = DiagnosticsStore(directory = File(dir, "diagnostics"), retentionDays = retentionDays) { now }

    @Test
    fun `log appends one line per day and counts entries and errors`() {
        val s = store(baseEpoch)
        s.log(DiagnosticLevel.INFO, "scan", "started")
        s.log(DiagnosticLevel.ERROR, "playback", "decode failed")
        s.log(DiagnosticLevel.WARNING, "scan", "line1\nline2|separator")

        val lines = s.readAll()
        assertThat(lines).hasSize(3)
        assertThat(lines[0]).startsWith("$baseEpoch|INFO|scan|started")
        assertThat(lines[1]).startsWith("$baseEpoch|ERROR|playback|decode failed")
        assertThat(lines[2]).contains("line1 line2/separator")
        assertThat(s.counts.totalEntries).isEqualTo(3)
        assertThat(s.counts.totalErrors).isEqualTo(1)
        assertThat(s.counts.fileCount).isEqualTo(1)
    }

    @Test
    fun `retention keeps the last seven days and deletes older files`() {
        val day = 86_400_000L
        val now = baseEpoch + 10 * day
        val s = store(now)
        for (age in 0..9) {
            val then = now - age * day
            File(s.directory, "sertum-${DiagnosticsStore.dayKey(then)}.log").apply {
                parentFile!!.mkdirs()
                writeText("age=$age\n")
            }
        }
        // 10 days: ages 0..9. Keep days 0..6 (last 7 files), delete ages 7..9.
        val deleted = s.pruneOldFiles()
        assertThat(deleted).isEqualTo(3)
        val names = s.readAll()
        assertThat(names).hasSize(7)
        assertThat(names.first()).contains("age=6")
        assertThat(names.last()).contains("age=0")
    }

    @Test
    fun `retentionDays is capped at seven per PRD`() {
        val error = runCatching { store(baseEpoch, retentionDays = 8) }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `export contains header counts and all retained entries`() {
        val s = store(baseEpoch)
        s.log(DiagnosticLevel.ERROR, "t", "boom")
        val export = s.exportText()
        assertThat(export).contains("Sertum diagnostics export")
        assertThat(export).contains("Errors=1")
        assertThat(export).contains("boom")
        assertThat(export).endsWith("\n")
    }

    @Test
    fun `readAll ignores unrelated files`() {
        val s = store(baseEpoch)
        File(s.directory, "notes.txt").apply {
            parentFile!!.mkdirs()
            writeText("not diagnostics")
        }
        s.log(DiagnosticLevel.INFO, "t", "one")
        assertThat(s.readAll()).hasSize(1)
        assertThat(s.counts.fileCount).isEqualTo(1)
    }
}
