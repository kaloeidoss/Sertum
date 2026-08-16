package com.sertum.player.ui.screens.nowplaying

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sertum.player.ui.playback.PlaybackStateHolder

@Composable
fun QueueScreen() {
    val state by PlaybackStateHolder.state.collectAsState()
    if (state.queue.isEmpty()) {
        Text(
            text = "Queue is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(state.queue) { index, title ->
            Text(
                text = "$index. $title",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
