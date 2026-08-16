package com.sertum.player.ui.navigation

import android.net.Uri

object SertumDestinations {
    const val MAIN = "main"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val SETTINGS = "settings"
    const val ALBUM_DETAIL = "album/{albumKey}"
    const val ARTIST_DETAIL = "artist/{artistName}"
    const val NOW_PLAYING = "nowplaying"
    const val QUEUE = "queue"

    // Folder-based album keys contain slashes and pipes; encode them so the
    // navigation route parser never treats them as path segments.
    fun albumDetail(albumKey: String) = "album/${Uri.encode(albumKey)}"
    fun artistDetail(artistName: String) = "artist/${Uri.encode(artistName)}"
}
