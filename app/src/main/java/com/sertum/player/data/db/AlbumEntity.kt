package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val albumKey: String,
    val title: String,
    val albumArtist: String,
    val year: Int?,
    /** Currently active cover (PRD 7.7.6 chain), consumed by the UI. */
    val coverRef: String?,
    val embeddedCoverPath: String?,
    val folderCoverPath: String?,
    val trackCount: Int,
)
