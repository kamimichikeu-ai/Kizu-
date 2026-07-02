package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SoftAmber,
    onPrimary = CharcoalBg,
    secondary = MutedText,
    onSecondary = WarmOffWhite,
    background = CharcoalBg,
    onBackground = WarmOffWhite,
    surface = DarkCardBg,
    onSurface = WarmOffWhite,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = MutedText,
    outline = SoftAmber
)

private val LightColorScheme = lightColorScheme(
    primary = SoftAmberDark,
    onPrimary = CreamBg,
    secondary = MutedTextDark,
    onSecondary = DarkCharcoalText,
    background = CreamBg,
    onBackground = DarkCharcoalText,
    surface = LightCardBg,
    onSurface = DarkCharcoalText,
    surfaceVariant = LightCardBg,
    onSurfaceVariant = MutedTextDark,
    outline = SoftAmberDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
