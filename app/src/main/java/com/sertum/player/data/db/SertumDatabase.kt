package com.sertum.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, AlbumEntity::class, ArtistEntity::class, CoverEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SertumDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
