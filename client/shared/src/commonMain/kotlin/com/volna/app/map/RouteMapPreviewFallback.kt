package com.volna.app.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.domain.model.GeoPoint
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route
import kotlin.math.max

@Composable
fun RouteMapPreviewFallback(
    route: Route,
    meetingPoint: MeetingPoint,
    state: MapUiState,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val spacing = VolnaTheme.tokens.spacing
    val mapBackground = MaterialTheme.colorScheme.surfaceVariant
    val routeColor = MaterialTheme.colorScheme.primary
    val pinColor = MaterialTheme.colorScheme.error
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Text(meetingPoint.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (state) {
                MapUiState.Loading -> Text("Карта загружается")
                MapUiState.Content -> {
                    RouteMapSchematic(
                        route = route,
                        meetingPoint = meetingPoint,
                        drawRoute = true,
                        backgroundColor = mapBackground,
                        routeColor = routeColor,
                        pinColor = pinColor,
                    )
                }
                MapUiState.GeometryMissing -> {
                    RouteMapSchematic(
                        route = route,
                        meetingPoint = meetingPoint,
                        drawRoute = false,
                        backgroundColor = mapBackground,
                        routeColor = routeColor,
                        pinColor = pinColor,
                    )
                    Text("Маршрут на карте недоступен, место встречи указано текстом")
                }
                MapUiState.Error -> {
                    Text("Не удалось загрузить карту")
                    Button(onClick = onRetry) {
                        Text("Обновить")
                    }
                }
            }
            OutlinedButton(onClick = onOpenExternal) {
                Text("Открыть в Яндекс.Картах")
            }
        }
    }
}

@Composable
private fun RouteMapSchematic(
    route: Route,
    meetingPoint: MeetingPoint,
    drawRoute: Boolean,
    backgroundColor: Color,
    routeColor: Color,
    pinColor: Color,
) {
    val routePoints = route.geometry?.points.orEmpty()
    val visiblePoints = if (drawRoute) routePoints + meetingPoint.coordinates else listOf(meetingPoint.coordinates)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val corner = 16.dp.toPx()
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(corner, corner),
        )
        if (visiblePoints.isEmpty()) return@Canvas

        val padding = 18.dp.toPx()
        val bounds = visiblePoints.bounds()
        fun GeoPoint.toOffset(): Offset {
            val width = max(bounds.maxLng - bounds.minLng, 0.0001)
            val height = max(bounds.maxLat - bounds.minLat, 0.0001)
            val x = padding + ((lng - bounds.minLng) / width).toFloat() * (size.width - padding * 2)
            val y = padding + ((bounds.maxLat - lat) / height).toFloat() * (size.height - padding * 2)
            return Offset(x, y)
        }

        if (drawRoute && routePoints.size > 1) {
            routePoints.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = routeColor,
                    start = start.toOffset(),
                    end = end.toOffset(),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(
            color = pinColor,
            radius = 7.dp.toPx(),
            center = meetingPoint.coordinates.toOffset(),
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = meetingPoint.coordinates.toOffset(),
        )
    }
}

private data class GeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
)

private fun List<GeoPoint>.bounds(): GeoBounds = GeoBounds(
    minLat = minOf { it.lat },
    maxLat = maxOf { it.lat },
    minLng = minOf { it.lng },
    maxLng = maxOf { it.lng },
)
