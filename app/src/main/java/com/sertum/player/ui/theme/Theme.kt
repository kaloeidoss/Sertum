package com.sertum.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SertumShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

private val DarkScheme = darkColorScheme(
    primary = WarmGold,
    onPrimary = Color.Black,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = SurfaceBlack,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2A2A2A),
    error = Color(0xFFCF6679),
)

private val LightScheme = lightColorScheme(
    primary = WarmGold,
    onPrimary = Color.White,
    background = PaperWhite,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFFECE5D8),
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFD8D0C2),
)

@Composable
fun SertumTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = SertumTypography,
        shapes = SertumShapes,
        content = content,
    )
}
