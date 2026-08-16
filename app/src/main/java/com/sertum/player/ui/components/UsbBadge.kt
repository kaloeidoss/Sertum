package com.sertum.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sertum.player.domain.playback.BitPerfectState

private val BadgeGreen = Color(0xFF4CAF50)
private val BadgeYellow = Color(0xFFE6B800)

/**
 * Framed "USB" letters badge (PRD A-18 user amendment):
 * green = bit-perfect intact; yellow = software volume below 100%.
 * The string "bit-perfect" is never rendered.
 */
@Composable
fun UsbBadge(state: BitPerfectState, modifier: Modifier = Modifier) {
    val color = when (state) {
        BitPerfectState.INTACT -> BadgeGreen
        BitPerfectState.DEGRADED -> BadgeYellow
        BitPerfectState.NOT_APPLICABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        border = BorderStroke(1.dp, color),
        modifier = modifier,
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(
                text = "USB",
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}
