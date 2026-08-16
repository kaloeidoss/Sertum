package com.sertum.player.data.scan

/** One track discovered by any scan source. Paths are source-normalized. */
data class ScanCandidate(
    val uri: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
)

/** A track already stored in the library database. */
data class ExistingTrack(
    val uri: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
)

/**
 * Merges the three scan sources (priority order is caller-controlled),
 * deduplicates by normalized path, and produces the incremental diff against
 * the existing library: add / update / delete (orphan cleanup).
 */
class ScanEngine {

    data class Plan(
        val toAdd: List<ScanCandidate>,
        val toUpdate: List<ScanCandidate>,
        val toDelete: List<ExistingTrack>,
    )

    fun normalizePath(path: String): String =
        path.trim().replace('\\', '/').trimEnd('/')

    fun plan(
        existing: List<ExistingTrack>,
        candidates: List<ScanCandidate>,
    ): Plan {
        val existingByPath = LinkedHashMap<String, ExistingTrack>()
        existing.forEach { existingByPath[normalizePath(it.path)] = it }

        val seen = HashSet<String>()
        val add = mutableListOf<ScanCandidate>()
        val update = mutableListOf<ScanCandidate>()
        candidates.forEach { candidate ->
            val key = normalizePath(candidate.path)
            if (!seen.add(key)) return@forEach
            val current = existingByPath[key]
            when {
                current == null -> add += candidate
                current.sizeBytes != candidate.sizeBytes ||
                    current.modifiedAtEpochMs != candidate.modifiedAtEpochMs -> update += candidate
                else -> Unit // unchanged
            }
        }

        val delete = existing.filter { normalizePath(it.path) !in seen }
        return Plan(add, update, delete)
    }
}
