package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = BlueText,
    secondary = BlueAccentDark,
    onSecondary = OnAccentText,
    background = AmoledBlack,
    onBackground = OnAccentText,
    surface = DarkSurface,
    onSurface = OnAccentText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnAccentText,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for AMOLED media player feel
    dynamicColor: Boolean = false, // Disable dynamic colors to keep elegant dark design theme intact
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
