package com.volna.app.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route

@Composable
fun RouteMapPreviewFallback(
    route: Route,
    meetingPoint: MeetingPoint,
    state: MapUiState,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val spacing = VolnaTheme.tokens.spacing
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Text(meetingPoint.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (state) {
                MapUiState.Loading -> Text("Карта загружается")
                MapUiState.Content -> Text("Превью маршрута будет показано здесь")
                MapUiState.GeometryMissing -> Text("Маршрут на карте недоступен, место встречи указано текстом")
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
