package com.sertum.player.domain.playback

enum class RepeatMode { OFF, ALL, ONE }

enum class ShuffleMode { OFF, ON }

/**
 * Pure queue state machine. Track ids are opaque Longs; the UI maps them to
 * library rows. Not persisted (PRD 7.5).
 */
class QueueEngine(
    private val seed: Long = System.currentTimeMillis(),
) {

    var repeatMode: RepeatMode = RepeatMode.OFF
        private set
    var shuffleMode: ShuffleMode = ShuffleMode.OFF
        private set

    private val tracks = mutableListOf<Long>()
    private var order = mutableListOf<Int>()
    private var currentIndex = -1

    val size: Int get() = tracks.size
    val currentTrackId: Long? get() = if (currentIndex in 0 until size) tracks[order[currentIndex]] else null

    fun setQueue(trackIds: List<Long>, startIndex: Int = 0): Boolean {
        tracks.clear()
        tracks.addAll(trackIds)
        rebuildOrder()
        if (tracks.isEmpty()) {
            currentIndex = -1
            return false
        }
        currentIndex = startIndex.coerceIn(0, tracks.size - 1)
        return true
    }

    fun setRepeat(mode: RepeatMode) {
        repeatMode = mode
    }

    fun setShuffle(mode: ShuffleMode) {
        shuffleMode = mode
        rebuildOrder()
        if (currentTrackId != null && currentIndex !in order.indices) currentIndex = order.size - 1
    }

    /** Returns the next track id or null when playback stops (OFF at the tail). */
    fun next(): Long? {
        if (size == 0) return null
        return when (repeatMode) {
            RepeatMode.ONE -> currentTrackId
            else -> {
                val next = currentIndex + 1
                if (next < order.size) {
                    currentIndex = next
                    currentTrackId
                } else if (repeatMode == RepeatMode.ALL) {
                    currentIndex = 0
                    currentTrackId
                } else {
                    null
                }
            }
        }
    }

    /** Returns previous track id, wrapping only in ALL mode; never null for a non-empty queue. */
    fun previous(): Long? {
        if (size == 0) return null
        val prev = currentIndex - 1
        return if (prev >= 0) {
            currentIndex = prev
            currentTrackId
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = order.size - 1
            currentTrackId
        } else {
            currentTrackId
        }
    }

    fun removeAt(position: Int): Long? {
        if (position !in 0 until size) return null
        val currentBefore = currentTrackId
        val removed = tracks.removeAt(position)
        if (tracks.isEmpty()) {
            currentIndex = -1
            order.clear()
            return removed
        }
        rebuildOrder()
        currentIndex = order.indexOfFirst { tracks[it] == currentBefore }
            .takeIf { it >= 0 } ?: order.size - 1
        return removed
    }

    fun move(from: Int, to: Int): Boolean {
        if (from !in 0 until size || to !in 0 until size) return false
        val currentBefore = currentTrackId
        val item = tracks.removeAt(from)
        tracks.add(to, item)
        rebuildOrder()
        currentIndex = order.indexOfFirst { tracks[it] == currentBefore }
        return true
    }

    fun clear() {
        tracks.clear()
        order.clear()
        currentIndex = -1
    }

    /** Synchronizes the queue cursor with an externally selected track (Media3 transition). */
    fun seekTo(trackId: Long): Boolean {
        val index = order.indexOfFirst { tracks[it] == trackId }
        if (index < 0) return false
        currentIndex = index
        return true
    }

    fun orderForTest(): List<Long> = order.map { tracks[it] }

    private fun rebuildOrder() {
        order = (0 until tracks.size).toMutableList()
        if (shuffleMode == ShuffleMode.ON && tracks.size > 1) {
            val random = java.util.Random(seed)
            for (i in order.size - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val t = order[i]
                order[i] = order[j]
                order[j] = t
            }
        }
    }
}
