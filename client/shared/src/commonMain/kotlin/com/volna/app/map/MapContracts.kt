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
    fun buildRouteTo(meetingPoint: MeetingPoint)
}

expect object PlatformMapLauncher : MapLauncher {
    override fun openYandexMaps(meetingPoint: MeetingPoint, route: Route?)
    override fun buildRouteTo(meetingPoint: MeetingPoint)
}

internal fun MeetingPoint.toYandexPointUrl(): String {
    val lat = coordinates.lat
    val lng = coordinates.lng
    return "https://yandex.ru/maps/?pt=$lng,$lat&z=16&l=map"
}

internal fun MeetingPoint.toYandexRouteUrl(): String {
    val lat = coordinates.lat
    val lng = coordinates.lng
    return "https://yandex.ru/maps/?rtext=~$lat,$lng&rtt=auto"
}

@Composable
expect fun RouteMapPreview(
    route: Route,
    meetingPoint: MeetingPoint,
    state: MapUiState,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
)
