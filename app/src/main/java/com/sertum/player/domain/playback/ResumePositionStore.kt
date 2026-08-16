package com.sertum.player.domain.playback

/** Persistence boundary for per-track resume positions. */
interface ResumePositionStore {
    fun get(trackId: Long): Long?
    fun put(trackId: Long, positionMs: Long)
    fun pruneOlderThan(cutoffEpochMs: Long): Int
}

class InMemoryResumePositionStore : ResumePositionStore {
    private val positions = mutableMapOf<Long, Pair<Long, Long>>() // id -> (pos, updatedAt)

    override fun get(trackId: Long): Long? = positions[trackId]?.first

    override fun put(trackId: Long, positionMs: Long) {
        positions[trackId] = positionMs to System.currentTimeMillis()
    }

    override fun pruneOlderThan(cutoffEpochMs: Long): Int {
        val stale = positions.filterValues { it.second < cutoffEpochMs }.keys
        stale.forEach { positions.remove(it) }
        return stale.size
    }
}
