package com.sertum.player.data.scan

import android.net.Uri
import java.io.File

/**
 * Optional full-disk scan source (A-10R). The caller is responsible for the
 * MANAGE_EXTERNAL_STORAGE permission and for surfacing the opt-in switch;
 * this class only walks the filesystem under the 20,000-track design cap.
 */
class FullScanSource(private val maxTracks: Int = 20_000) {

    fun scan(root: File, maxDepth: Int = 24): List<ScanCandidate> {
        val result = mutableListOf<ScanCandidate>()
        walk(root, maxDepth, result)
        return result
    }

    private fun walk(dir: File, depth: Int, out: MutableList<ScanCandidate>) {
        if (depth <= 0 || out.size >= maxTracks) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (out.size >= maxTracks) return
            when {
                file.isDirectory -> walk(file, depth - 1, out)
                file.isFile && AudioExtensions.isSupported(file.name) -> {
                    out += ScanCandidate(
                        uri = Uri.fromFile(file).toString(),
                        path = file.absolutePath,
                        sizeBytes = file.length(),
                        modifiedAtEpochMs = file.lastModified(),
                    )
                }
            }
        }
    }
}
