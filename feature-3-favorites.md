# Фича: «Избранные заезды»



## 1. Цель фичи

Позволить пользователю сохранять понравившиеся заезды в избранное, просматривать их список в профиле, переходить к деталям заезда и удалять заезды из избранного. Данные избранного хранятся локально между запусками приложения.

## 2. Что было сделано

**Новые файлы:**
- `shared/src/commonMain/kotlin/com/apex/app/domain/model/FavoriteRide.kt`
- `shared/src/commonMain/kotlin/com/apex/app/favorites/FavoritesRepository.kt`
- `shared/src/commonMain/kotlin/com/apex/app/favorites/LocalFavoritesRepository.kt`
- `shared/src/commonMain/kotlin/com/apex/app/favorites/presentation/FavoritesScreen.kt`

**Изменённые файлы:**
- `shared/src/commonMain/kotlin/com/apex/app/catalog/presentation/SlotListScreen.kt` — карточка заезда, список расписания
- `shared/src/commonMain/kotlin/com/apex/app/catalog/presentation/SlotDetailsScreen.kt` — добавлена иконка сердца в детали и поддержка переключения избранного
- `shared/src/commonMain/kotlin/com/apex/app/profile/presentation/ProfileScreen.kt` — добавлен пункт "Избранное" в профиль
- `shared/src/commonMain/kotlin/com/apex/app/ApexApp.kt` — добавлен маршрут и передача `FavoritesStore` в `MainTabs`


## 3. Код реализации

Ниже — полный код новых файлов и полные версии изменённых файлов (взяты из репозитория на момент составления документа).

---

### `shared/src/commonMain/kotlin/com/apex/app/domain/FavoriteRide.kt`

```kotlin
package com.apex.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRide(
    val slotId: String,
    val rideName: String,
    val rideDescription: String,
    val price: Int,
    val marshalName: String,
    val addedAt: Long,
)
```

---

### `shared/src/commonMain/kotlin/com/apex/app/favorites/FavoritesRepository.kt`

```kotlin
package com.apex.app.favorites

import com.apex.app.domain.model.FavoriteRide

interface FavoritesRepository {
    suspend fun addFavorite(ride: FavoriteRide): Result<Unit>
    suspend fun removeFavorite(slotId: String): Result<Unit>
    suspend fun getFavorites(): Result<List<FavoriteRide>>
    suspend fun isFavorite(slotId: String): Result<Boolean>
}
```

---

### `shared/src/commonMain/kotlin/com/apex/app/favorites/LocalFavoritesRepository.kt`

```kotlin
package com.apex.app.favorites

import com.apex.app.core.storage.PlatformKeyValueStorage
import com.apex.app.domain.model.FavoriteRide
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class LocalFavoritesRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FavoritesRepository {
    private val key = "apex_favorites_v1"

    private fun loadAll(): MutableList<FavoriteRide> {
        val raw = PlatformKeyValueStorage.getString(key) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(FavoriteRide.serializer()), raw).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<FavoriteRide>) {
        val raw = json.encodeToString(ListSerializer(FavoriteRide.serializer()), list)
        PlatformKeyValueStorage.putString(key, raw)
    }

    override suspend fun addFavorite(ride: FavoriteRide): Result<Unit> {
        return try {
            val all = loadAll().toMutableList()
            if (all.none { it.slotId == ride.slotId }) {
                all.add(ride)
                saveAll(all)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(slotId: String): Result<Unit> {
        return try {
            val all = loadAll().filterNot { it.slotId == slotId }
            saveAll(all)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(): Result<List<FavoriteRide>> {
        return try {
            Result.success(loadAll())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(slotId: String): Result<Boolean> {
        return try {
            val all = loadAll()
            Result.success(all.any { it.slotId == slotId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### `shared/src/commonMain/kotlin/com/apex/app/favorites/presentation/FavoritesScreen.kt`

```kotlin
package com.apex.app.favorites.presentation

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apex.app.core.theme.ApexTheme
import com.apex.app.core.ui.Loadable
import com.apex.app.domain.model.FavoriteRide

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
            .padding(ApexTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.tokens.spacing.md),
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
    favorites: List<FavoriteRide>,
    removing: Set<String>,
    onRemove: (String) -> Unit,
    onSlotClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.tokens.spacing.sm),
    ) {
        items(favorites, key = { it.slotId }) { favorite ->
            FavoriteRideCard(
                favorite = favorite,
                removing = removing.contains(favorite.slotId),
                onRemove = { onRemove(favorite.slotId) },
                onSlotClick = { onSlotClick(favorite.slotId) },
            )
        }
    }
}

@Composable
private fun FavoriteRideCard(
    favorite: FavoriteRide,
    removing: Boolean,
    onRemove: () -> Unit,
    onSlotClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(ApexTheme.tokens.radius.xl),
            )
            .clickable { onSlotClick() }
            .padding(ApexTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.tokens.spacing.sm),
    ) {
        Text(
            text = favorite.rideName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = favorite.rideDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Маршал: ${favorite.marshalName}",
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
                shape = RoundedCornerShape(ApexTheme.tokens.radius.pill),
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
            .padding(top = ApexTheme.tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.tokens.spacing.sm),
    ) {
        Text(
            text = "Пока нет избранных заездов",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(ApexTheme.tokens.radius.pill),
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
            .padding(top = ApexTheme.tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.tokens.spacing.sm),
    ) {
        Text(
            text = "Не удалось загрузить избранное",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onIntent(FavoritesIntent.Load) },
            shape = RoundedCornerShape(ApexTheme.tokens.radius.pill),
        ) {
            Text("Повторить")
        }
    }
}
```

---

### `shared/src/commonMain/kotlin/com/apex/app/catalog/presentation/SlotListScreen.kt` (ключевые изменения)

```kotlin
// В карточке заезда добавлена иконка сердца
VolnaIcon(
    imageVector = if (isFavorite) Icons.Heart else Icons.HeartEmpty,
    contentDescription = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
    modifier = Modifier
        .clickable { onToggleFavorite(slotId) }
        .size(24.dp),
    tint = if (isFavorite) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurfaceVariant,
)
```

---

### `shared/src/commonMain/kotlin/com/apex/app/catalog/presentation/SlotDetailsScreen.kt` (ключевые изменения)

```kotlin
// В хедере деталей добавлена иконка сердца
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    CircleActionButton(icon = Icons.Back, contentDescription = "Назад", onClick = onBack)
    CircleActionButton(
        icon = if (isFavorite) Icons.Heart else Icons.HeartEmpty,
        contentDescription = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
        onClick = onToggleFavorite,
        tint = if (isFavorite) Color(0xFFE63946) else MaterialTheme.colorScheme.primary,
    )
}
```

---

### `shared/src/commonMain/kotlin/com/apex/app/profile/presentation/ProfileScreen.kt` (ключевые изменения)

```kotlin
// Добавлен пункт "Избранное" в меню профиля
Button(
    onClick = onFavoritesClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(ApexTheme.tokens.radius.md),
) {
    Text("Избранное")
    VolnaIcon(
        imageVector = Icons.ArrowRight,
        contentDescription = null,
        modifier = Modifier.align(Alignment.CenterEnd),
    )
}
```

---

### `shared/src/commonMain/kotlin/com/apex/app/ApexApp.kt` (ключевые изменения)

```kotlin
// Внутри ApexApp добавлена регистрация favoritesStore и передача в MainTabs
val favoritesStore = koinViewModel<FavoritesStore>()
...
val favoritesState by favoritesStore.state.collectAsState()
...
favoritesStore.accept(FavoritesIntent.Reset)
...
MainTabs(
    ...
    favoritesState = favoritesState,
    onFavoritesIntent = favoritesStore::accept,
    onFavoritesRouteClick = { slotId -> navController.navigate(SlotDetailsDestination(slotId)) },
    onFavoritesBack = { navController.popBackStack() },
    onProfileFavoritesClick = { navController.navigate(FavoritesDestination) },
)
```

---

## 4. Как проверить (ручной тест)

1. Открыть экран расписания → нажать на иконку сердца на карточке заезда → заезд добавился в избранное (сердце заполнилось).
2. Открыть детали заезда → нажать на иконку сердца → заезд добавился/удалился из избранного.
3. Открыть профиль → нажать пункт "Избранное" → открывается список избранных заездов.
4. В списке избранного нажать на заезд → открывается `SlotDetailsScreen`.
5. Удалить заезд из избранного (кнопка "Удалить") → он исчезает из списка.
6. Перезапустить приложение → данные избранного сохраняются (проверяется, что заезд остаётся в списке).

## 5. Промпты, использованные при разработке

- "Открыть SlotListScreen и найти карточку заезда"
- "Добавить иконку сердца в карточку"
- "Добавить пункт 'Избранное' в профиль"
- "Создать отдельный экран для избранного"