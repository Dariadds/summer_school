package com.volna.app.map

import androidx.compose.runtime.Composable
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route

sealed interface MapUiState {
    data object Loading : MapUiState
    data object Content : MapUiState
    data object GeometryMissing : MapUiState
    data object Error : MapUiState
}

interface MapLauncher {
    fun openYandexMaps(meetingPoint: MeetingPoint, route: Route?)
}

@Composable
expect fun RouteMapPreview(
    route: Route,
    meetingPoint: MeetingPoint,
    state: MapUiState,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
)
