package dev.lisfox.geyserlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFFFAF9FD)
val Surface = Color(0xFFFFFFFF)
val Primary = Color(0xFF5266A5)
val PrimarySoft = Color(0xFFE1E6FF)
val TextPrimary = Color(0xFF202127)
val TextSecondary = Color(0xFF686A73)
val Divider = Color(0xFFE8E7EC)
val Success = Color(0xFF27845A)
val Danger = Color(0xFFC64B55)

private val LauncherColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Color(0xFF192753),
    background = AppBackground,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0EFF4),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF7A7B84),
    outlineVariant = Divider,
    error = Danger
)

@Composable
fun GeyserLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColors, content = content)
}
