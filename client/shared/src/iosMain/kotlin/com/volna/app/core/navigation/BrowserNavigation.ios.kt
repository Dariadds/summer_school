package com.volna.app.core.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BindBrowserNavigation(
    currentPath: String,
    onPathChange: (String) -> Unit,
) {
}

@Composable
actual fun BindSystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
) {
}

actual fun currentBrowserPath(): String? = null
