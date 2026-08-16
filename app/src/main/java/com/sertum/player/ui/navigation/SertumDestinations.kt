package com.sertum.player.ui.navigation

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

    fun albumDetail(albumKey: String) = "album/$albumKey"
    fun artistDetail(artistName: String) = "artist/$artistName"
}
