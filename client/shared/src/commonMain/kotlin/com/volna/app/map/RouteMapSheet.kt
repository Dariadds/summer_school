package com.volna.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route
import com.volna.app.domain.model.RouteType

// BS-004 / LOGIC-006: route map sheet shows a mock screenshot and hands off the meeting point to external maps.
@Composable
fun RouteMapSheet(
    route: Route,
    meetingPoint: MeetingPoint,
    mapLauncher: MapLauncher = PlatformMapLauncher,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .width(VolnaTheme.tokens.sizing.contentWidth)
                .clickable {}
                .shadow(
                    elevation = VolnaTheme.tokens.spacing.sm,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.radius.lg,
                        topEnd = VolnaTheme.tokens.radius.lg,
                    ),
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.radius.lg,
                        topEnd = VolnaTheme.tokens.radius.lg,
                    ),
                )
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Text("Карта маршрута", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            RouteMapPreview(
                route = route,
                meetingPoint = meetingPoint,
                onOpenExternal = { mapLauncher.openExternalMap(meetingPoint) },
            )
            Text(route.name, fontWeight = FontWeight.Bold)
            Text("${route.type.toUiText()}, ${route.durationMin} мин")
            Text("Место встречи: ${meetingPoint.title.ifBlank { "уточняется" }}")
            OutlinedButton(
                onClick = { mapLauncher.buildRouteTo(meetingPoint) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проложить маршрут")
            }
            OutlinedButton(
                onClick = { mapLauncher.openExternalMap(meetingPoint) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Открыть в картах")
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Закрыть")
            }
        }
    }
}

private fun RouteType.toUiText(): String = when (this) {
    RouteType.Novice -> "для новичков"
    RouteType.Experienced -> "для опытных"
}
