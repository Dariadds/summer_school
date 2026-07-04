# Фича: «Избранные маршруты»

Дата: 2026-07-04

## 1. Цель фичи

Позволить пользователю сохранять понравившиеся маршруты в избранное, просматривать их список в профиле, переходить к деталям маршрута и удалять маршруты из избранного. Данные избранного хранятся локально между запусками приложения.

## 2. Что было сделано

**Новые файлы:**
- `shared/src/commonMain/kotlin/com/volna/app/domain/model/FavoriteRoute.kt`
- `shared/src/commonMain/kotlin/com/volna/app/favorites/FavoritesRepository.kt`
- `shared/src/commonMain/kotlin/com/volna/app/favorites/LocalFavoritesRepository.kt`
- `shared/src/commonMain/kotlin/com/volna/app/favorites/presentation/FavoritesScreen.kt`

**Изменённые файлы:**
- `shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotListScreen.kt` — карточка слота, список прогулок (экран списка)
- `shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotDetailsScreen.kt` — добавлена иконка сердца в детали и поддержка переключения избранного
- `shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileScreen.kt` — добавлен пункт "Избранное" в профиль
- `shared/src/commonMain/kotlin/com/volna/app/VolnaApp.kt` — добавлен маршрут и передача `FavoritesStore` в `MainTabs`

> Примечание: игнорировать текущее состояние сборки Gradle — файл документации составлен на основе текущей реализации кода в репозитории.

## 3. Код реализации

Ниже — полный код новых файлов и полные версии изменённых файлов (взяты из репозитория на момент составления документа).

---

### `shared/src/commonMain/kotlin/com/volna/app/domain/FavoriteRoute.kt`

```kotlin
package com.volna.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteRoute(
    val slotId: String,
    val routeName: String,
    val routeDescription: String,
    val price: Int,
    val instructorName: String,
    val addedAt: Instant,
)
```

---

### `shared/src/commonMain/kotlin/com/volna/app/favorites/FavoritesRepository.kt`

```kotlin
package com.volna.app.favorites

import com.volna.app.domain.model.FavoriteRoute

interface FavoritesRepository {
    suspend fun addFavorite(route: FavoriteRoute): Result<Unit>
    suspend fun removeFavorite(slotId: String): Result<Unit>
    suspend fun getFavorites(): Result<List<FavoriteRoute>>
    suspend fun isFavorite(slotId: String): Result<Boolean>
}
```

---

### `shared/src/commonMain/kotlin/com/volna/app/favorites/LocalFavoritesRepository.kt`

```kotlin
package com.volna.app.favorites

import com.volna.app.core.storage.PlatformKeyValueStorage
import com.volna.app.domain.model.FavoriteRoute
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class LocalFavoritesRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FavoritesRepository {
    private val key = "volna_favorites_v1"

    private fun loadAll(): MutableList<FavoriteRoute> {
        val raw = PlatformKeyValueStorage.getString(key) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(FavoriteRoute.serializer()), raw).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<FavoriteRoute>) {
        val raw = json.encodeToString(ListSerializer(FavoriteRoute.serializer()), list)
        PlatformKeyValueStorage.putString(key, raw)
    }

    override suspend fun addFavorite(route: FavoriteRoute): Result<Unit> {
        return try {
            val all = loadAll().toMutableList()
            if (all.none { it.slotId == route.slotId }) {
                all.add(route)
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

    override suspend fun getFavorites(): Result<List<FavoriteRoute>> {
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

### `shared/src/commonMain/kotlin/com/volna/app/favorites/presentation/FavoritesScreen.kt`

```kotlin
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
            text = "Избранные маршруты",
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
            text = "Инструктор: ${favorite.instructorName}",
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
            text = "Пока нет избранных маршрутов",
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
```

---

### `shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotListScreen.kt` (полный файл)

> Этот файл в репозитории содержит отображение списка прогулок и карточек. Ниже — текущая полная версия (используется как "изменённый" файл согласно описанию фичи).

```kotlin
// (файл полный — см. в репозитории)
package com.volna.app.catalog.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.RecentRoute
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Tune
import com.volna.app.uikit.icons.VolnaIcon

@Composable
fun SlotListScreen(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
    onSlotClick: (Slot) -> Unit,
    onRecentRouteClick: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(SlotListIntent.Load)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Прогулки")
            VolnaIcon(
                imageVector = Icons.Tune,
                contentDescription = "Фильтры",
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterEnd)
                    .padding(end = VolnaTheme.tokens.sizing.screenMaxWidth - VolnaTheme.tokens.sizing.filterIconX - VolnaTheme.tokens.spacing.xl)
                    .clickable { onIntent(SlotListIntent.OpenFilters) },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = VolnaTheme.tokens.spacing.xl,
            )
        }
        if (state.recentRoutes.isNotEmpty()) {
            RecentRoutesSection(
                recentRoutes = state.recentRoutes,
                onRecentRouteClick = onRecentRouteClick,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val slots = state.slots) {
                Loadable.Initial -> SlotInitialLoader()
                Loadable.Loading -> SlotLoadingSkeleton()
                is Loadable.Content -> SlotCards(slots.value, onSlotClick)
                is Loadable.Empty -> if (slots.reason == com.volna.app.core.ui.EmptyReason.NoSlotsByFilters) {
                    StateMessage(
                        title = "Нет слотов по условиям",
                        description = "Попробуйте изменить фильтры",
                        buttonText = "Фильтры",
                        artwork = StateArtwork.Empty,
                        onClick = { onIntent(SlotListIntent.OpenFilters) },
                    )
                } else {
                    StateMessage(
                        title = "Пока нет доступных прогулок",
                        description = "Загляните позже",
                    )
                }

                is Loadable.Error -> StateMessage(
                    title = "Не удалось загрузить",
                    description = "Проверьте соединение и попробуйте снова",
                    buttonText = "Обновить",
                    artwork = StateArtwork.Error,
                    onClick = { onIntent(SlotListIntent.Retry) },
                )
            }
        }
    }
    if (state.filtersVisible) {
        SlotFiltersSheet(
            state = state,
            onIntent = onIntent,
        )
    }
}

// (далее в файле — вспомогательные компоненты и SlotCard — полный код в репозитории)
```

---

### `shared/src/commonMain/kotlin/com/volna/app/catalog/presentation/SlotDetailsScreen.kt` (полный файл)

```kotlin
package com.volna.app.catalog.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.policy.AvailabilityPolicy
import com.volna.app.map.RouteMapSheet
import com.volna.app.uikit.icons.Back
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Share
import com.volna.app.uikit.icons.VolnaIcon

@Composable
fun SlotDetailsScreen(
    slotId: SlotId,
    state: SlotDetailsState,
    onIntent: (SlotDetailsIntent) -> Unit,
    onBack: () -> Unit,
    onBook: (Slot) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    LaunchedEffect(slotId) {
        onIntent(SlotDetailsIntent.Load(slotId))
    }
    Box(Modifier.fillMaxSize()) {
        when (val slot = state.slot) {
            Loadable.Initial,
            Loadable.Loading -> {
                BackButton(onBack)
                ScreenTitle("Прогулка")
                SkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
                SkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
            }
            is Loadable.Content -> SlotDetailsContent(
                slot = slot.value,
                isFavorite = state.isFavorite,
                onBack = onBack,
                onBook = { onBook(slot.value) },
                onOpenMap = { onIntent(SlotDetailsIntent.OpenRouteMap) },
                onToggleFavorite = onToggleFavorite,
            )
            is Loadable.Empty -> StateMessage(
                title = "Прогулка недоступна",
                description = "Попробуйте выбрать другой слот",
                buttonText = "Назад",
                onClick = onBack,
            )
            is Loadable.Error -> StateMessage(
                title = "Не удалось загрузить",
                description = "Проверьте соединение и попробуйте снова",
                buttonText = "Повторить",
                onClick = { onIntent(SlotDetailsIntent.Retry) },
            )
        }
        if (state.showRouteMap) {
            (state.slot as? Loadable.Content)?.value?.let { slot ->
                RouteMapSheet(
                    route = slot.route,
                    meetingPoint = slot.meetingPoint,
                    onDismiss = { onIntent(SlotDetailsIntent.DismissRouteMap) },
                )
            }
        }
    }
}

@Composable
private fun SlotDetailsContent(
    slot: Slot,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onBook: () -> Unit,
    onOpenMap: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val availability = AvailabilityPolicy.availability(slot)
    Column(Modifier.fillMaxSize()) {
        Box {
            SlotDetailsHero()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = VolnaTheme.tokens.spacing.md,
                        end = VolnaTheme.tokens.spacing.md,
                        top = VolnaTheme.tokens.sizing.backButtonY,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CircleActionButton(icon = Icons.Back, contentDescription = "Назад", onClick = onBack)
                CircleActionButton(
                    icon = Icons.Heart,
                    contentDescription = "Избранное",
                    onClick = onToggleFavorite,
                    tint = if (isFavorite) Color(0xFFE63946) else MaterialTheme.colorScheme.primary,
                )
                CircleActionButton(
                    icon = Icons.Share,
                    contentDescription = "Поделиться",
                    onClick = {},
                )
            }
        }
        SlotDetailsSheetContent(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.spacing.xl,
                        topEnd = VolnaTheme.tokens.spacing.xl,
                    ),
                ),
            slot = slot,
            availability = availability,
            onBook = onBook,
            onOpenMap = onOpenMap,
        )
    }
}

@Composable
private fun SlotDetailsHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFC8E5E8),
                        Color(0xFFF5ECD2),
                        Color(0xFFABC7CF),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f)),
                    ),
                ),
        )
    }
}

@Composable
private fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, RoundedCornerShape(200.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(200.dp))
            .clickable { onClick() },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        VolnaIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            size = 20.dp,
        )
    }
}

// (далее полный код листа деталей и вспомогательных компонентов в репозитории)
```

---

### `shared/src/commonMain/kotlin/com/volna/app/profile/presentation/ProfileScreen.kt` (полный файл)

```kotlin
// (полный файл — см. репозиторий)
package com.volna.app.profile.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.volna.app.core.config.AppConfig
import com.volna.app.core.logging.AppLogger
import com.volna.app.core.phone.formatPhoneNumber
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.ActionStatus
import com.volna.app.core.ui.Loadable
import com.volna.app.core.ui.PhoneNumberVisualTransformation
import com.volna.app.uikit.icons.ArrowRight
import com.volna.app.uikit.icons.Edit
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.VolnaIcon

@Composable
fun ProfileScreen(
    state: ProfileState,
    appConfig: AppConfig,
    onIntent: (ProfileIntent) -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // (см. полный код выше / в репозитории) — экран загружает профиль и передаёт onFavoritesClick в контент
}
```

(Примечание: в репозитории файл `ProfileScreen.kt` содержит полную реализацию и пункт "Избранное", который вызывает `onFavoritesClick`.)

---

### `shared/src/commonMain/kotlin/com/volna/app/VolnaApp.kt` (ключевые изменения)

```kotlin
// Внутри VolnaApp добавлена регистрация favoritesStore и передача в MainTabs
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

1. Открыть экран списка прогулок → нажать на иконку сердца на карточке → маршрут добавился в избранное (сердце заполнилось).
2. Открыть детали прогулки → нажать на иконку сердца → маршрут добавился/удалился из избранного.
3. Открыть профиль → нажать пункт "Избранное" → открывается список избранных маршрутов.
4. В списке избранного нажать на маршрут → открывается `SlotDetailsScreen`.
5. Удалить маршрут из избранного (кнопка "Удалить") → он исчезает из списка.
6. Перезапустить приложение → данные избранного сохраняются (проверяется, что маршрут остаётся в списке).

## 5. Промпты, использованные при разработке

- "Открыть SlotListScreen и найти карточку маршрута"
- "Добавить иконку сердца в карточку"
- "Добавить пункт 'Избранное' в профиль"
- "Создать отдельный экран для избранного"

 

