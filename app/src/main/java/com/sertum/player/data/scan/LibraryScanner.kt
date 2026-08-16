package com.sertum.player.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.sertum.player.data.covers.CoverResolver
import com.sertum.player.data.covers.CoverStore
import com.sertum.player.data.db.AlbumEntity
import com.sertum.player.data.db.ArtistEntity
import com.sertum.player.data.db.LibraryDao
import com.sertum.player.data.db.SourceType
import com.sertum.player.data.db.TrackEntity
import com.sertum.player.domain.model.AlbumKey
import java.io.File
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
    private val coverStore: CoverStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class ParsedTrack(
        val track: TrackEntity,
        val embeddedPicture: ByteArray?,
        val folderPath: String,
    )

    fun scanNow(onDone: (Result<ScanStats>) -> Unit = {}) {
        scope.launch {
            onDone(runCatching { scanBlocking() })
        }
    }

    suspend fun scanBlocking(): ScanStats {
        val candidates = withContext(Dispatchers.IO) { MediaStoreSource(context).scan() }
        var parsed = 0
        var failed = 0
        val parsedTracks = mutableListOf<ParsedTrack>()
        for (candidate in candidates) {
            val parsedTrack = runCatching { parse(candidate) }.getOrNull()
            if (parsedTrack == null) {
                failed += 1
            } else {
                parsedTracks += parsedTrack
                parsed += 1
            }
        }
        val tracks = parsedTracks.map { it.track }

        dao.deleteTracksBySource(SourceType.MEDIA_STORE)
        tracks.forEach { dao.upsertTrack(it) }

        val albums = mutableListOf<AlbumEntity>()
        for ((key, rows) in parsedTracks.groupBy { it.track.albumKey }) {
            val first = rows.first()
            val folderCover = resolveFolderCover(first.folderPath)
            val embeddedPath = rows.firstNotNullOfOrNull { it.embeddedPicture }?.let { picture ->
                runCatching { coverStore.saveEmbedded(key, picture) }.getOrNull()
            }
            val userCover = dao.coverForAlbum(key)?.userCoverPath
            val resolved = CoverResolver.resolve(userCover, embeddedPath, folderCover).reference
            albums += AlbumEntity(
                albumKey = key,
                title = first.track.albumTitle ?: "Unknown album",
                albumArtist = first.track.albumArtist ?: first.track.artist ?: "Unknown artist",
                year = first.track.year,
                coverRef = resolved,
                embeddedCoverPath = embeddedPath,
                folderCoverPath = folderCover,
                trackCount = rows.size,
            )
        }
        val artists = tracks
            .map { (it.albumArtist ?: it.artist ?: "Unknown artist") to it.albumKey }
            .groupBy { it.first }
            .map { (name, rows) ->
                ArtistEntity(
                    name = name,
                    sortKey = name.uppercase(),
                    albumCount = rows.map { it.second }.distinct().size,
                )
            }

        dao.clearAlbumsAndArtists()
        albums.forEach { dao.upsertAlbum(it) }
        artists.forEach { dao.upsertArtist(it) }
        return ScanStats(candidates.size, parsed, failed, albums.size, artists.size)
    }

    private fun resolveFolderCover(folderPath: String): String? {
        val dir = File(folderPath)
        if (!dir.isDirectory) return null
        return listOf("cover.jpg", "cover.png", "folder.jpg", "Folder.jpg", "Cover.jpg")
            .map { File(dir, it) }
            .firstOrNull { it.isFile }
            ?.absolutePath
    }

    private fun parse(candidate: ScanCandidate): ParsedTrack? {
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
            val embeddedPicture = runCatching { retriever.embeddedPicture }.getOrNull()
            val track = TrackEntity(
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
            ParsedTrack(track, embeddedPicture, candidate.path.substringBeforeLast('/', ""))
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fallbackTitle(candidate: ScanCandidate): String =
        candidate.path.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Unknown track" }
}
