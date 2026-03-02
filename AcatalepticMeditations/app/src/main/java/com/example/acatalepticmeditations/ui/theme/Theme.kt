package com.example.acatalepticmeditations.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable


private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyber,
    onPrimary = OnPrimaryCyber,
    secondary = SecondaryCyber,
    onSecondary = OnSecondaryCyber,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = TextColor,
    onBackground = TextColor
)

@Composable
fun AcatalepticMeditationsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}