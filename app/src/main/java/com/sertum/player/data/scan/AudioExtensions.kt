package com.sertum.player.data.scan

/** File extensions the V1 scanner accepts (PRD 7.4). */
object AudioExtensions {
    val EXTENSIONS: Set<String> = setOf(
        "flac", "wav", "aiff", "aif", "m4a", "mp3", "aac", "ogg", "opus",
    )

    fun isSupported(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in EXTENSIONS
    }
}
