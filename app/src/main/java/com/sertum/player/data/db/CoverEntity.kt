package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "covers")
data class CoverEntity(
    @PrimaryKey val albumKey: String,
    val userCoverPath: String,
    val updatedAtEpochMs: Long,
)
