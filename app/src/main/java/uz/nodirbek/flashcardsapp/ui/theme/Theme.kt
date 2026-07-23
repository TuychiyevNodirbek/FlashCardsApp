package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// True when the APP theme (user preference, not just system) is dark.
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary         = FdPrimary,
    onPrimary       = Color.White,
    primaryContainer = FdPrimaryLight,
    secondary       = FdGreen,
    onSecondary     = Color.White,
    tertiary        = FdPurple,
    onTertiary      = Color.White,
    background      = FdBackground,
    onBackground    = FdText,
    surface         = FdSurface,
    onSurface       = FdText,
    surfaceVariant  = FdSurface2,
    onSurfaceVariant = FdTextSub,
    outline         = FdBorder,
    error           = FdRed,
    onError         = Color.White
)

private val DarkColors = darkColorScheme(
    primary         = FdDarkPrimary,
    onPrimary       = FdDarkText,  // Light text on dark primary
    primaryContainer = Color(0xFF1A2F6B),
    secondary       = Color(0xFF4FD897),  // Lighter green for dark mode
    onSecondary     = FdDarkText,
    tertiary        = Color(0xFFA78BFA),
    onTertiary      = FdDarkText,
    background      = FdDarkBackground,
    onBackground    = FdDarkText,
    surface         = FdDarkSurface,
    onSurface       = FdDarkText,
    surfaceVariant  = FdDarkSurface2,
    onSurfaceVariant = FdDarkTextSub,
    outline         = FdDarkBorder,
    error           = Color(0xFFFF8F8F),
    onError         = FdDarkText
)

@Composable
fun FlashCardsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
