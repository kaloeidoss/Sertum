package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** PRD US-7: per-track resume position, pruned after 30 days. */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val trackId: Long,
    val positionMs: Long,
    val updatedAtEpochMs: Long,
)
