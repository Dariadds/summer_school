package com.volna.app.core.navigation

import androidx.activity.compose.BackHandler
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
    BackHandler(enabled = enabled, onBack = onBack)
}

actual fun currentBrowserPath(): String? = null
