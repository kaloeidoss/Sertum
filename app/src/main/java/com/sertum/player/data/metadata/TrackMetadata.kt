package com.sertum.player.data.metadata

/** Normalized track metadata extracted from audio files. */
data class TrackMetadata(
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val albumTitle: String?,
    val discNumber: Int?,
    val trackNumber: Int?,
    val year: Int?,
    val genre: String?,
    val durationMs: Long?,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val mimeType: String?,
)
