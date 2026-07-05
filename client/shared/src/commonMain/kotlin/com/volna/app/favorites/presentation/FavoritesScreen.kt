package com.volna.app.favorites.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.FavoriteRoute

@Composable
fun FavoritesScreen(
    state: FavoritesState,
    onIntent: (FavoritesIntent) -> Unit,
    onBack: () -> Unit,
    onSlotClick: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(FavoritesIntent.Load)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
    ) {
        Text(
            text = "Избранные заезды",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        when (val favorites = state.favorites) {
            Loadable.Initial,
            Loadable.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is Loadable.Content -> FavoritesList(
                favorites = favorites.value,
                removing = state.removing,
                onRemove = { onIntent(FavoritesIntent.Remove(it)) },
                onSlotClick = onSlotClick,
            )
            is Loadable.Empty -> EmptyFavoriteState(onBack)
            is Loadable.Error -> ErrorFavoriteState(onIntent)
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteRoute>,
    removing: Set<String>,
    onRemove: (String) -> Unit,
    onSlotClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        items(favorites, key = { it.slotId }) { favorite ->
            FavoriteRouteCard(
                favorite = favorite,
                removing = removing.contains(favorite.slotId),
                onRemove = { onRemove(favorite.slotId) },
                onSlotClick = { onSlotClick(favorite.slotId) },
            )
        }
    }
}

@Composable
private fun FavoriteRouteCard(
    favorite: FavoriteRoute,
    removing: Boolean,
    onRemove: () -> Unit,
    onSlotClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.xl),
            )
            .clickable { onSlotClick() }
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Text(
            text = favorite.routeName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = favorite.routeDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Маршал: ${favorite.instructorName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${favorite.price} ₽",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onRemove,
                enabled = !removing,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                modifier = Modifier.height(40.dp),
            ) {
                Text(if (removing) "Удаляем..." else "Удалить")
            }
        }
    }
}

@Composable
private fun EmptyFavoriteState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = VolnaTheme.tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Text(
            text = "Пока нет избранных заездов",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        ) {
            Text("Вернуться")
        }
    }
}

@Composable
private fun ErrorFavoriteState(onIntent: (FavoritesIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = VolnaTheme.tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Text(
            text = "Не удалось загрузить избранное",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onIntent(FavoritesIntent.Load) },
            shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        ) {
            Text("Повторить")
        }
    }
}
