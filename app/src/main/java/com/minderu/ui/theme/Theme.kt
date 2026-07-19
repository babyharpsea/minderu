package com.minderu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinderuColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerLow = Color(0xFFF7F2FA),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun MinderuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinderuColors,
        typography = Typography(),
        content = content
    )
}
