package com.volna.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class VolnaSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

@Immutable
data class VolnaRadius(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
)

data class VolnaTokens(
    val colors: VolnaColorScheme,
    val spacing: VolnaSpacing = VolnaSpacing(),
    val radius: VolnaRadius = VolnaRadius(),
)

val LocalVolnaTokens = staticCompositionLocalOf {
    VolnaTokens(colors = VolnaLightColors)
}

@Composable
fun VolnaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = VolnaTokens(colors = VolnaLightColors)
    androidx.compose.runtime.CompositionLocalProvider(LocalVolnaTokens provides tokens) {
        MaterialTheme(
            colorScheme = tokens.colors.toMaterialColorScheme(darkTheme),
            typography = Typography(),
            content = content,
        )
    }
}

object VolnaTheme {
    val tokens: VolnaTokens
        @Composable get() = LocalVolnaTokens.current
}

private fun VolnaColorScheme.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) {
        androidx.compose.material3.darkColorScheme()
    } else {
        androidx.compose.material3.lightColorScheme()
    }
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = border,
        error = error,
        onError = Color.White,
    )
}
