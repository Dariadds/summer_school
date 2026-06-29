package com.volna.app.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(
                    start = VolnaTheme.tokens.spacing.md,
                    end = VolnaTheme.tokens.spacing.md,
                    bottom = VolnaTheme.tokens.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = VolnaTheme.tokens.spacing.xs)
                    .height(4.dp)
                    .fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.12f)
                        .height(4.dp)
                        .background(
                            color = Color(0xFFCCCCCC).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                        ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "Маршрут",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Закрыть",
                    modifier = Modifier.clickable { onDismiss() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs),
            ) {
                RouteMapTag(text = route.type.toUiText(), color = Color(0xFF92FF9A))
                RouteMapTag(text = route.name, color = Color(0xFFFFF897))
            }
            RouteMapPreview(
                route = route,
                meetingPoint = meetingPoint,
                onOpenExternal = { mapLauncher.openExternalMap(meetingPoint) },
            )
            Text(
                text = "Прогулка по маршруту займет ${route.durationMin} минут",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF797979),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { mapLauncher.buildRouteTo(meetingPoint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Проложить маршрут")
            }
            Button(
                onClick = { mapLauncher.openExternalMap(meetingPoint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Text("Открыть в Яндекс.Картах")
            }
        }
    }
}

@Composable
private fun RouteMapTag(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(VolnaTheme.tokens.radius.sm))
            .padding(horizontal = VolnaTheme.tokens.spacing.xs, vertical = VolnaTheme.tokens.spacing.xxs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun RouteType.toUiText(): String = when (this) {
    RouteType.Novice -> "Новичковый"
    RouteType.Experienced -> "Опытный"
}
