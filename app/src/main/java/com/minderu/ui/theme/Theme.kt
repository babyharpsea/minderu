package com.minderu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Minderu is intentionally light-only — the palette below is the whole design language.
 *
 * Every role the UI actually references is defined here. The previous scheme left
 * `primaryContainer`, `secondaryContainer`, `error`, `onTertiaryContainer` and
 * `surfaceVariant` unset, so those components silently fell back to M3's baseline
 * purple, which is why call-to-action colours ended up hardcoded at call sites.
 */
private val MinderuColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = OnCoral,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,

    secondary = Slate40,
    onSecondary = OnCoral,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,

    tertiary = Mauve40,
    onTertiary = OnCoral,
    tertiaryContainer = Mauve90,
    onTertiaryContainer = Mauve10,

    background = Surface,
    onBackground = OnSurface,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerLow = SurfaceContainerLow,

    outline = Outline,
    outlineVariant = OutlineVariant,

    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

/**
 * Accent colours that have no matching Material role but must stay consistent
 * across screens. Exposed through the theme so no screen hardcodes a literal.
 */
object MinderuAccents {
    /** Primary call-to-action: Get Started, Create Task, dock FAB. */
    val action = Coral
    val onAction = OnCoral
}

@Composable
fun MinderuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinderuColorScheme,
        typography = Typography(),
        content = content
    )
}

/** Convenience accessor mirroring `MaterialTheme.colorScheme` for Minderu-specific accents. */
val MaterialTheme.accents: MinderuAccents
    @Composable @ReadOnlyComposable get() = MinderuAccents
