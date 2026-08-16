package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val name: String,
    val sortKey: String,
    val albumCount: Int,
)
