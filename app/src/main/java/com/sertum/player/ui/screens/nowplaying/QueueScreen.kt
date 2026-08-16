package com.sertum.player.ui.screens.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sertum.player.SertumApplication
import com.sertum.player.ui.playback.PlaybackStateHolder

@Composable
fun QueueScreen() {
    val state by PlaybackStateHolder.state.collectAsState()
    val controller = (LocalContext.current.applicationContext as SertumApplication).playbackController
    if (state.queue.isEmpty()) {
        Text(
            text = "Queue is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = { controller.clearQueue() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Clear queue")
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(state.queue) { index, title ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    IconButton(
                        onClick = { controller.moveQueueItem(index, index - 1) },
                        enabled = index > 0,
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(
                        onClick = { controller.moveQueueItem(index, index + 1) },
                        enabled = index < state.queue.lastIndex,
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                    }
                    IconButton(onClick = { controller.removeQueueItem(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
