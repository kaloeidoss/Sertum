package com.sertum.player.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.sertum.player.data.db.AlbumEntity
import com.sertum.player.data.db.ArtistEntity
import com.sertum.player.data.db.LibraryDao
import com.sertum.player.data.db.SourceType
import com.sertum.player.data.db.TrackEntity
import com.sertum.player.domain.model.AlbumKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanStats(
    val candidates: Int,
    val parsed: Int,
    val failed: Int,
    val albums: Int,
    val artists: Int,
)

/**
 * Bootstrap scanner for the production library: MediaStore source ->
 * MediaMetadataRetriever -> Room (tracks/albums/artists). Replaces all
 * MEDIA_STORE rows on each run (full rescan semantics; incremental diffing
 * stays with ScanEngine for the M3 milestone follow-up).
 */
class LibraryScanner(
    private val context: Context,
    private val dao: LibraryDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scanNow(onDone: (Result<ScanStats>) -> Unit = {}) {
        scope.launch {
            onDone(runCatching { scanBlocking() })
        }
    }

    suspend fun scanBlocking(): ScanStats {
        val candidates = withContext(Dispatchers.IO) { MediaStoreSource(context).scan() }
        var parsed = 0
        var failed = 0
        val tracks = mutableListOf<TrackEntity>()
        for (candidate in candidates) {
            val parsedTrack = runCatching { parse(candidate) }.getOrNull()
            if (parsedTrack == null) {
                failed += 1
            } else {
                tracks += parsedTrack
                parsed += 1
            }
        }

        dao.deleteTracksBySource(SourceType.MEDIA_STORE)
        tracks.forEach { dao.upsertTrack(it) }

        val albums = tracks.groupBy { it.albumKey }.map { (key, rows) ->
            val first = rows.first()
            AlbumEntity(
                albumKey = key,
                title = first.albumTitle ?: "Unknown album",
                albumArtist = first.albumArtist ?: first.artist ?: "Unknown artist",
                year = first.year,
                coverRef = null,
                trackCount = rows.size,
            )
        }
        val artists = tracks
            .map { it.albumArtist ?: it.artist ?: "Unknown artist" }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) ->
                ArtistEntity(name = name, sortKey = name.uppercase(), albumCount = count)
            }

        dao.clearAlbumsAndArtists()
        albums.forEach { dao.upsertAlbum(it) }
        artists.forEach { dao.upsertArtist(it) }
        return ScanStats(candidates.size, parsed, failed, albums.size, artists.size)
    }

    private fun parse(candidate: ScanCandidate): TrackEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(candidate.uri))
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle(candidate)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val albumArtist = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            }.getOrNull() ?: artist
            val albumTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val albumKey = AlbumKey.resolve(
                title = albumTitle,
                albumArtist = albumArtist,
                artist = artist,
                folderPath = candidate.path.substringBeforeLast('/', "").takeIf { it.isNotBlank() },
            )
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
            val trackNumber = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()
            }.getOrNull()
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: "audio/*"
            TrackEntity(
                id = candidate.uri.hashCode().toLong(),
                uri = candidate.uri,
                title = title,
                artist = artist,
                albumArtist = albumArtist,
                albumTitle = albumTitle,
                albumKey = albumKey.key,
                discNumber = null,
                trackNumber = trackNumber,
                year = year,
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                durationMs = durationMs,
                sampleRate = null,
                bitDepth = null,
                mimeType = mimeType,
                source = SourceType.MEDIA_STORE,
                coverRef = null,
                isPlayable = true,
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fallbackTitle(candidate: ScanCandidate): String =
        candidate.path.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Unknown track" }
}
