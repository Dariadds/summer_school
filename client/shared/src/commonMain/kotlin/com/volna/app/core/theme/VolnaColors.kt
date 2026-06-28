package com.volna.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class VolnaColorScheme(
    val brand: Color,
    val onBrand: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

val VolnaLightColors = VolnaColorScheme(
    brand = Color(0xFF0B6E78),
    onBrand = Color.White,
    background = Color(0xFFF7FAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7F1F2),
    textPrimary = Color(0xFF152022),
    textSecondary = Color(0xFF5B6A6D),
    border = Color(0xFFD3DEE0),
    success = Color(0xFF237A4B),
    warning = Color(0xFF9A6400),
    error = Color(0xFFB3261E),
)
