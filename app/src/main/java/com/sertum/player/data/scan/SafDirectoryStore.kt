package com.sertum.player.data.scan

import android.content.Context
import android.net.Uri

/**
 * Persists the user-selected SAF tree URIs. The system grant itself lives in
 * the platform's persisted-uri-permissions table; this list is the app-side
 * record of which directories to scan and display.
 */
class SafDirectoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("saf_dirs", Context.MODE_PRIVATE)

    fun load(): List<Uri> =
        prefs.getStringSet(KEY_URIS, emptySet())
            .orEmpty()
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }

    fun add(uri: Uri) {
        val updated = load().map { it.toString() }.toMutableSet().apply { add(uri.toString()) }
        prefs.edit().putStringSet(KEY_URIS, updated).apply()
    }

    fun remove(uri: Uri) {
        val updated = load().map { it.toString() }.toMutableSet().apply { remove(uri.toString()) }
        prefs.edit().putStringSet(KEY_URIS, updated).apply()
    }

    private companion object {
        const val KEY_URIS = "uris"
    }
}
