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
import com.sertum.player.data.db.TrackEntity

@Composable
fun AlbumDetailScreen(albumKey: String) {
    val dao = (LocalContext.current.applicationContext as SertumApplication).database.libraryDao()
    val tracks by dao.tracksForAlbum(albumKey).collectAsState(initial = emptyList())
    val album = tracks.firstOrNull()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = album?.albumTitle ?: "Album",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = album?.albumArtist ?: "Unknown artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(tracks, key = { it.id }) { track ->
            TrackRow(track)
        }
    }
}
