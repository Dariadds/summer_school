package com.volna.app.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route

@Composable
fun RouteMapPreviewFallback(
    route: Route,
    meetingPoint: MeetingPoint,
    onOpenExternal: () -> Unit,
) {
    val spacing = VolnaTheme.tokens.spacing
    val waterColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val parkColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    val streetColor = MaterialTheme.colorScheme.surface
    val routeColor = MaterialTheme.colorScheme.primary
    val pinColor = MaterialTheme.colorScheme.error
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Text(meetingPoint.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MockRouteScreenshot(
                waterColor = waterColor,
                parkColor = parkColor,
                streetColor = streetColor,
                routeColor = routeColor,
                pinColor = pinColor,
            )
            OutlinedButton(onClick = onOpenExternal) {
                Text("Открыть в картах")
            }
        }
    }
}

@Composable
private fun MockRouteScreenshot(
    waterColor: Color,
    parkColor: Color,
    streetColor: Color,
    routeColor: Color,
    pinColor: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        val corner = 16.dp.toPx()
        drawRoundRect(
            color = waterColor,
            cornerRadius = CornerRadius(corner, corner),
        )
        drawRoundRect(
            color = parkColor,
            topLeft = Offset(size.width * 0.58f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.34f),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        )
        listOf(0.22f, 0.48f, 0.74f).forEach { y ->
            drawLine(
                color = streetColor,
                start = Offset(12.dp.toPx(), size.height * y),
                end = Offset(size.width - 12.dp.toPx(), size.height * (y - 0.08f)),
                strokeWidth = 9.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        listOf(0.18f, 0.42f, 0.68f).forEach { x ->
            drawLine(
                color = streetColor,
                start = Offset(size.width * x, 12.dp.toPx()),
                end = Offset(size.width * (x + 0.08f), size.height - 12.dp.toPx()),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val routePoints = listOf(
            Offset(size.width * 0.18f, size.height * 0.72f),
            Offset(size.width * 0.34f, size.height * 0.48f),
            Offset(size.width * 0.58f, size.height * 0.55f),
            Offset(size.width * 0.78f, size.height * 0.28f),
        )
        routePoints.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = routeColor,
                start = start,
                end = end,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val pinCenter = routePoints.first()
        drawCircle(
            color = pinColor,
            radius = 7.dp.toPx(),
            center = pinCenter,
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = pinCenter,
        )
    }
}
