package com.volna.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun BindBrowserNavigation(
    currentPath: String,
    onPathChange: (String) -> Unit,
) {
    LaunchedEffect(currentPath) {
        val targetPath = normalizePath(currentPath)
        if (currentBrowserPath() != targetPath) {
            window.history.pushState(null, "", targetPath)
        }
    }

    DisposableEffect(onPathChange) {
        val listener: (Event) -> Unit = {
            onPathChange(currentBrowserPath() ?: DEFAULT_PATH)
        }
        window.addEventListener("popstate", listener)
        onDispose {
            window.removeEventListener("popstate", listener)
        }
    }
}

@Composable
actual fun BindSystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
) {
}

actual fun currentBrowserPath(): String? =
    normalizePath(window.location.pathname.ifBlank { DEFAULT_PATH })

private fun normalizePath(path: String): String =
    path
        .substringBefore('?')
        .substringBefore('#')
        .ifBlank { DEFAULT_PATH }
        .let { if (it.startsWith("/")) it else "/$it" }

private const val DEFAULT_PATH = "/"
