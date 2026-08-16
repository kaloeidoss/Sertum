package com.sertum.player.data.covers

import java.io.File
import java.security.MessageDigest

/**
 * Non-destructive user cover storage (PRD 7.8): files live under
 * filesDir/covers/<sha256(albumKey)>.img; the original image is kept and
 * thumbnail generation is the UI/image-pipeline's job when rendering.
 */
class CoverStore(private val filesDir: File) {

    private val dir: File by lazy { File(filesDir, "covers").apply { mkdirs() } }

    fun save(albumKey: String, bytes: ByteArray): String {
        val file = fileFor(albumKey)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun userCoverFor(albumKey: String): String? =
        fileFor(albumKey).takeIf { it.exists() && it.length() > 0 }?.absolutePath

    fun delete(albumKey: String): Boolean = fileFor(albumKey).delete()

    fun fileFor(albumKey: String): File = File(dir, sha256(albumKey) + ".img")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
