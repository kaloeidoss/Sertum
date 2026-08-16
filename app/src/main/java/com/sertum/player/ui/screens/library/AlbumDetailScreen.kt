package com.sertum.player.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sertum.player.SertumApplication
import com.sertum.player.data.db.CoverEntity
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(albumKey: String) {
    val context = LocalContext.current
    val app = context.applicationContext as SertumApplication
    val dao = app.database.libraryDao()
    val tracks by dao.tracksForAlbum(albumKey).collectAsState(initial = emptyList())
    val album = tracks.firstOrNull()
    val scope = rememberCoroutineScope()

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val path = app.coverStore.save(albumKey, bytes)
                dao.setAlbumCover(albumKey, path)
                dao.insertCover(CoverEntity(albumKey, path, System.currentTimeMillis()))
            }
        }
    }

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
                OutlinedButton(
                    onClick = {
                        pickCover.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(if (album?.coverRef != null) "Replace cover" else "Add cover")
                }
                if (album?.coverRef != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                app.coverStore.delete(albumKey)
                                dao.deleteCover(albumKey)
                                dao.setAlbumCover(albumKey, null)
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Remove cover")
                    }
                }
            }
        }
        items(tracks, key = { it.id }) { track ->
            TrackRow(track)
        }
    }
}
