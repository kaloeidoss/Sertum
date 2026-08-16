package com.sertum.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index("uri"), Index("albumKey")],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val artist: String?,
    val albumArtist: String?,
    val albumTitle: String?,
    val albumKey: String,
    val discNumber: Int?,
    val trackNumber: Int?,
    val year: Int?,
    val genre: String?,
    val durationMs: Long,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val mimeType: String,
    val source: SourceType,
    val coverRef: String?,
    val isPlayable: Boolean,
)
