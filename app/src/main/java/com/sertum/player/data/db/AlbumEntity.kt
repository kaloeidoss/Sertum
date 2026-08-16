package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val albumKey: String,
    val title: String,
    val albumArtist: String,
    val year: Int?,
    val coverRef: String?,
    val trackCount: Int,
)
