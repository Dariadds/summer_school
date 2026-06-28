package com.volna.app.map

import androidx.compose.runtime.Composable
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route

@Composable
actual fun RouteMapPreview(
    route: Route,
    meetingPoint: MeetingPoint,
    state: MapUiState,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    RouteMapPreviewFallback(route, meetingPoint, state, onRetry, onOpenExternal)
}
