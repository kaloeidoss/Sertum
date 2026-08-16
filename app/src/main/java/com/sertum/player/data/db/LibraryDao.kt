package com.sertum.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE albumKey = :albumKey " +
            "ORDER BY COALESCE(discNumber, 999) ASC, COALESCE(trackNumber, 999) ASC, title COLLATE NOCASE ASC",
    )
    fun tracksForAlbum(albumKey: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM albums ORDER BY albumArtist COLLATE NOCASE, title COLLATE NOCASE")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query(
        "SELECT * FROM albums WHERE albumArtist = :artist " +
            "ORDER BY title COLLATE NOCASE",
    )
    fun albumsForArtist(artist: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM artists ORDER BY sortKey COLLATE NOCASE")
    fun observeArtists(): Flow<List<ArtistEntity>>

    @Upsert
    suspend fun upsertTrack(track: TrackEntity)

    @Upsert
    suspend fun upsertAlbum(album: AlbumEntity)

    @Upsert
    suspend fun upsertArtist(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCover(cover: CoverEntity)

    @Query("SELECT * FROM covers WHERE albumKey = :albumKey")
    suspend fun coverForAlbum(albumKey: String): CoverEntity?

    @Query("UPDATE albums SET coverRef = :coverRef WHERE albumKey = :albumKey")
    suspend fun setAlbumCover(albumKey: String, coverRef: String?)

    @Query("DELETE FROM covers WHERE albumKey = :albumKey")
    suspend fun deleteCover(albumKey: String)

    @Query("DELETE FROM tracks WHERE source = :source")
    suspend fun deleteTracksBySource(source: SourceType)

    @Query("DELETE FROM tracks WHERE uri NOT IN (:uris)")
    suspend fun deleteTracksNotIn(uris: List<String>)

    @Query("SELECT uri FROM tracks")
    suspend fun allTrackUris(): List<String>
}
