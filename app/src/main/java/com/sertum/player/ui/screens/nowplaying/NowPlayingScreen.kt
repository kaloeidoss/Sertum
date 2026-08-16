package com.sertum.player.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sertum.player.R
import com.sertum.player.SertumApplication
import com.sertum.player.ui.components.UsbBadge
import com.sertum.player.ui.playback.OutputMode
import com.sertum.player.ui.playback.PlaybackStateHolder
import com.sertum.player.ui.theme.SurfaceBlack

/**
 * Full player. The cover stays in the upper area; title, progress and
 * transport controls cluster near the bottom (user preference 2026-08-16).
 */
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
                text = state.trackTitle.ifBlank { stringResource(R.string.no_track_playing) }.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.trackTitle.ifBlank { stringResource(R.string.no_track_playing) },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = stringResource(R.string.cd_queue))
                }
            }

            // Framed USB badge sits ABOVE the rate/depth info (A-18 amendment).
            if (state.outputMode == OutputMode.USB_EXCLUSIVE) {
                UsbBadge(state.bitPerfectState, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                text = buildString {
                    if (state.sampleRate > 0) append("${state.sampleRate / 1000f} kHz")
                    if (state.bitDepth > 0) append(" · ${state.bitDepth} bit")
                    append(" · ")
                    append(
                        when (state.outputMode) {
                            OutputMode.USB_EXCLUSIVE -> stringResource(R.string.output_usb_exclusive)
                            OutputMode.BLUETOOTH -> stringResource(R.string.output_bluetooth)
                            OutputMode.STANDARD -> stringResource(R.string.output_standard)
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
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { controller.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.cd_previous))
                }
                IconButton(
                    onClick = { controller.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                        modifier = Modifier.size(48.dp),
                    )
                }
                IconButton(onClick = { controller.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.cd_next))
                }
            }
        }
    }
}
