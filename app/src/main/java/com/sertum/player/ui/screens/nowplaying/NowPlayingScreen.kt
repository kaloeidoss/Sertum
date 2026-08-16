package com.sertum.player.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sertum.player.SertumApplication
import com.sertum.player.ui.components.UsbBadge
import com.sertum.player.ui.playback.OutputMode
import com.sertum.player.ui.playback.PlaybackStateHolder
import com.sertum.player.ui.theme.SurfaceBlack

@Composable
fun NowPlayingScreen(onOpenQueue: () -> Unit) {
    val state by PlaybackStateHolder.state.collectAsState()
    val controller = (LocalContext.current.applicationContext as SertumApplication).playbackController
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(SurfaceBlack, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.trackTitle.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.trackTitle, style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = state.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Filled.QueueMusic, contentDescription = "Queue")
            }
        }

        // Framed USB badge sits ABOVE the rate/depth info (A-18 amendment).
        if (state.outputMode == OutputMode.USB_EXCLUSIVE) {
            UsbBadge(state.bitPerfectState, modifier = Modifier.padding(top = 16.dp))
        }
        Text(
            text = buildString {
                if (state.sampleRate > 0) append("${state.sampleRate / 1000f} kHz")
                if (state.bitDepth > 0) append(" · ${state.bitDepth} bit")
                append(" · ")
                append(
                    when (state.outputMode) {
                        OutputMode.USB_EXCLUSIVE -> "USB exclusive"
                        OutputMode.BLUETOOTH -> "Bluetooth"
                        OutputMode.STANDARD -> "Standard output"
                    },
                )
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        val displayedPosition = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }
        Slider(
            value = if (dragging) dragValue else displayedPosition,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                if (state.durationMs > 0) {
                    controller.seekTo((dragValue * state.durationMs).toLong())
                }
                dragging = false
            },
            modifier = Modifier.padding(top = 24.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { controller.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(
                onClick = { controller.togglePlayPause() },
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp),
                )
            }
            IconButton(onClick = { controller.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }
}
