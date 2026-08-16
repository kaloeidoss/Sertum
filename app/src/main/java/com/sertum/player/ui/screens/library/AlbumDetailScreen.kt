package com.sertum.player.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sertum.player.R
import com.sertum.player.SertumApplication
import com.sertum.player.data.covers.CoverResolver
import com.sertum.player.data.db.CoverEntity
import com.sertum.player.ui.theme.SurfaceBlack
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(albumKey: String) {
    val context = LocalContext.current
    val app = context.applicationContext as SertumApplication
    val dao = app.database.libraryDao()
    val tracks by dao.tracksForAlbum(albumKey).collectAsState(initial = emptyList())
    val albums by dao.observeAlbums().collectAsState(initial = emptyList())
    val covers by dao.observeCovers().collectAsState(initial = emptyList())
    val album = albums.firstOrNull { it.albumKey == albumKey }
    val hasUserCover = covers.any { it.albumKey == albumKey }
    val firstTrack = tracks.firstOrNull()
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
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .background(SurfaceBlack, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    val coverRef = album?.coverRef?.takeUnless {
                        it == com.sertum.player.data.covers.CoverResolver.PLACEHOLDER_REF
                    }
                    if (coverRef != null) {
                        AsyncImage(
                            model = coverRef,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = (album?.title ?: firstTrack?.title ?: "?").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = album?.title ?: firstTrack?.albumTitle ?: stringResource(R.string.album_default_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = album?.albumArtist ?: firstTrack?.albumArtist ?: stringResource(R.string.unknown_artist),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            val coverRef = album?.coverRef
                            app.playbackController.playTracks(
                                tracks.map { it.toPlayable().copy(coverRef = coverRef) },
                                startIndex = 0,
                            )
                        }
                    },
                    enabled = tracks.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.play_all))
                }
                OutlinedButton(
                    onClick = {
                        pickCover.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        stringResource(
                            if (hasUserCover) R.string.replace_cover else R.string.add_cover,
                        ),
                    )
                }
                if (hasUserCover) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                app.coverStore.delete(albumKey)
                                dao.deleteCover(albumKey)
                                val fallback = CoverResolver.resolveAfterUserRemoval(
                                    album?.embeddedCoverPath,
                                    album?.folderCoverPath,
                                ).reference
                                dao.setAlbumCover(albumKey, fallback)
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.remove_cover))
                    }
                }
            }
        }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                onClick = {
                    val coverRef = album?.coverRef
                    app.playbackController.playTracks(
                        tracks.map { it.toPlayable().copy(coverRef = coverRef) },
                        startIndex = index,
                    )
                },
            )
        }
    }
}
