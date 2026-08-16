package com.sertum.player.domain.playback

import com.sertum.player.data.db.LibraryDao
import com.sertum.player.data.db.PlaybackPositionEntity

/** Persistence boundary for per-track resume positions. */
interface ResumePositionStore {
    suspend fun get(trackId: Long): Long?
    suspend fun put(trackId: Long, positionMs: Long)
    suspend fun pruneOlderThan(cutoffEpochMs: Long): Int
}

class InMemoryResumePositionStore(
    private val clock: () -> Long = System::currentTimeMillis,
) : ResumePositionStore {
    private val positions = mutableMapOf<Long, Pair<Long, Long>>() // id -> (pos, updatedAt)

    override suspend fun get(trackId: Long): Long? = positions[trackId]?.first

    override suspend fun put(trackId: Long, positionMs: Long) {
        positions[trackId] = positionMs to clock()
    }

    override suspend fun pruneOlderThan(cutoffEpochMs: Long): Int {
        val stale = positions.filterValues { it.second < cutoffEpochMs }.keys
        stale.forEach { positions.remove(it) }
        return stale.size
    }
}

/** Room-backed resume positions with the PRD 30-day cleanup window. */
class RoomResumePositionStore(
    private val dao: LibraryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ResumePositionStore {

    override suspend fun get(trackId: Long): Long? =
        dao.resumePosition(trackId)?.positionMs

    override suspend fun put(trackId: Long, positionMs: Long) {
        dao.putResumePosition(PlaybackPositionEntity(trackId, positionMs, clock()))
    }

    override suspend fun pruneOlderThan(cutoffEpochMs: Long): Int =
        dao.pruneResumePositions(cutoffEpochMs)

    companion object {
        const val PRUNE_AFTER_MS = 30L * 24 * 60 * 60 * 1000
    }
}
