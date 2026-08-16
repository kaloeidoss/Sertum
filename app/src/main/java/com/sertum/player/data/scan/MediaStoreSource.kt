package com.sertum.player.data.scan

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * System media index source (US-3 AC1).
 * Requires READ_EXTERNAL_STORAGE on API 29-32 and READ_MEDIA_AUDIO on API 33+;
 * permission handling stays in the caller (UI/settings layer).
 */
class MediaStoreSource(private val context: Context) {

    fun scan(): List<ScanCandidate> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val result = mutableListOf<ScanCandidate>()
        context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val modCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val data = cursor.getString(dataCol)
                val relative = cursor.getString(relCol)
                val name = cursor.getString(nameCol)
                val path = if (!data.isNullOrBlank()) {
                    data
                } else {
                    val relative = listOfNotNull(relative, name).joinToString("/")
                    android.os.Environment.getExternalStorageDirectory().absolutePath +
                        "/" + relative
                }
                result += ScanCandidate(
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    path = path,
                    sizeBytes = cursor.getLong(sizeCol),
                    modifiedAtEpochMs = cursor.getLong(modCol) * 1000L,
                )
            }
        }
        return result
    }
}
