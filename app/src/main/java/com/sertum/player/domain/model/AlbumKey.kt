package com.sertum.player.domain.model

/**
 * Album identity per PRD 7.7.1:
 * (Album Title, Album Artist) -> (Album Title, Artist) -> folder path fallback.
 */
data class AlbumKey(val title: String, val albumArtist: String) {
    companion object {
        fun resolve(
            title: String?,
            albumArtist: String?,
            artist: String?,
            folderPath: String?,
        ): AlbumKey = when {
            !title.isNullOrBlank() && !albumArtist.isNullOrBlank() -> AlbumKey(title, albumArtist)
            !title.isNullOrBlank() && !artist.isNullOrBlank() -> AlbumKey(title, artist)
            !folderPath.isNullOrBlank() -> AlbumKey("__FOLDER__$folderPath", "__FOLDER__$folderPath")
            else -> AlbumKey("__UNKNOWN__", "__UNKNOWN__")
        }
    }
}
