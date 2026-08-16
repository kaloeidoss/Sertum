package com.sertum.player.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sertum.player.SertumApplication
import com.sertum.player.data.db.TrackEntity
import com.sertum.player.ui.theme.WarmGold

@Composable
fun SongsScreen() {
    val dao = (LocalContext.current.applicationContext as SertumApplication).database.libraryDao()
    val tracks by dao.observeTracks().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    val visible = if (query.isBlank()) tracks else tracks.filter {
        it.title.contains(query, ignoreCase = true) ||
            (it.artist?.contains(query, ignoreCase = true) == true) ||
            (it.albumTitle?.contains(query, ignoreCase = true) == true)
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (visible.isEmpty()) {
            val context = LocalContext.current
            val granted = hasMediaPermission(context)
            EmptyLibrary(
                label = "Songs",
                actionText = if (granted) null else "Open app settings",
                onAction = if (granted) null else {
                    {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                },
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { track ->
                    TrackRow(track)
                }
            }
        }
    }
}

@Composable
fun TrackRow(track: TrackEntity) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = track.trackNumber?.toString() ?: "•",
                style = MaterialTheme.typography.labelLarge,
                color = WarmGold,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = track.artist ?: "Unknown artist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun hasMediaPermission(context: android.content.Context): Boolean {
    val granted = android.content.pm.PackageManager.PERMISSION_GRANTED
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == granted
    } else {
        context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == granted
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
