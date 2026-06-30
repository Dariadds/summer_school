package com.volna.app.core.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BindBrowserNavigation(
    currentPath: String,
    onPathChange: (String) -> Unit,
) {
}

actual fun currentBrowserPath(): String? = null
