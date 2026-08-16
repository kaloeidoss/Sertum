package com.sertum.player.ui.screens.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-player host: the player surface, with the queue as a second layer
 * that slides up over it and slides back down to reveal the player.
 * [showQueue] is hoisted so the outer bottom sheet can refuse to dismiss
 * while the queue layer is open.
 */
@Composable
fun NowPlayingHost(
    showQueue: Boolean,
    onShowQueueChange: (Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        NowPlayingScreen(onOpenQueue = { onShowQueueChange(true) })

        // Side-swipe/back while the queue layer is open closes only the
        // queue layer and leaves the player sheet in place.
        BackHandler(enabled = showQueue) { onShowQueueChange(false) }

        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                QueueScreen(onBack = { onShowQueueChange(false) })
            }
        }
    }
}
