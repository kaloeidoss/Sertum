package com.sertum.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Full-player host: the player surface, with the queue as a second layer
 * that slides up over it and slides back down to reveal the player.
 */
@Composable
fun NowPlayingHost() {
    var showQueue by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        NowPlayingScreen(onOpenQueue = { showQueue = true })

        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                QueueScreen(onBack = { showQueue = false })
            }
        }
    }
}
