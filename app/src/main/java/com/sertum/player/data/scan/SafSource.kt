package com.sertum.player.data.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * SAF folder source (US-3 AC2). Walks a persisted tree URI and reports every
 * supported audio file. Recursion depth is capped to avoid pathological trees.
 */
class SafSource {

    fun scan(context: Context, treeUri: Uri, maxDepth: Int = 16): List<ScanCandidate> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val result = mutableListOf<ScanCandidate>()
        walk(context, root, maxDepth, result)
        return result
    }

    private fun walk(
        context: Context,
        dir: DocumentFile,
        depth: Int,
        out: MutableList<ScanCandidate>,
    ) {
        if (depth <= 0) return
        for (child in dir.listFiles()) {
            when {
                child.isDirectory -> walk(context, child, depth - 1, out)
                child.isFile && AudioExtensions.isSupported(child.name.orEmpty()) -> {
                    out += ScanCandidate(
                        uri = child.uri.toString(),
                        path = child.uri.toString(),
                        sizeBytes = child.length(),
                        modifiedAtEpochMs = child.lastModified(),
                    )
                }
            }
        }
    }
}
