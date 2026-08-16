package com.sertum.player.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sertum.player.SertumApplication
import com.sertum.player.data.db.AlbumEntity

/** Artist -> albums (PRD US-4: the HiBy gap is fixed here). */
@Composable
fun ArtistDetailScreen(artistName: String, onAlbumClick: (String) -> Unit = {}) {
    val dao = (LocalContext.current.applicationContext as SertumApplication).database.libraryDao()
    val albums by dao.albumsForArtist(artistName).collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                text = artistName,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
        items(albums, key = { it.albumKey }) { album: AlbumEntity ->
            AlbumCard(album, onClick = { onAlbumClick(album.albumKey) })
        }
    }
}
