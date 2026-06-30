package com.volna.app.core.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun BindBrowserNavigation(
    currentPath: String,
    onPathChange: (String) -> Unit,
)

@Composable
expect fun BindSystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
)

expect fun currentBrowserPath(): String?
