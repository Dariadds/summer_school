package com.volna.app.catalog.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.policy.AvailabilityPolicy
import com.volna.app.map.RouteMapSheet
import com.volna.app.uikit.icons.Back
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Share
import com.volna.app.uikit.icons.Tune
import com.volna.app.uikit.icons.VolnaIcon
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SlotListScreen(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
    onSlotClick: (Slot) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(SlotListIntent.Load)
    }
    ScreenTitle("Прогулки")
    VolnaIcon(
        imageVector = Icons.Tune,
        contentDescription = "Фильтры",
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.sizing.filterIconX, y = VolnaTheme.tokens.sizing.topTitleY)
            .clickable { onIntent(SlotListIntent.OpenFilters) },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        size = VolnaTheme.tokens.spacing.xl,
    )
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
    if (state.filtersVisible) {
        SlotFiltersSheet(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun SlotFiltersSheet(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
) {
    // CMP-13 / BS-001: filter form only collects conditions; SCR-002 reloads after apply.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onIntent(SlotListIntent.CloseFilters) },
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {}
                .shadow(
                    elevation = 0.dp,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.spacing.xl,
                        topEnd = VolnaTheme.tokens.spacing.xl,
                    ),
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.spacing.xl,
                        topEnd = VolnaTheme.tokens.spacing.xl,
                    ),
                ),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .offset(y = 8.dp)
                        .background(
                            color = Color(0xFFCCCCCC).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.Center)
                        .padding(horizontal = VolnaTheme.tokens.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Фильтры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Сбросить",
                        modifier = Modifier.clickable { onIntent(SlotListIntent.ResetFilters) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.width(VolnaTheme.tokens.sizing.contentWidth),
                verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.lg),
            ) {
                FilterGroup(title = "Дата старта") {
                    FilterChipRow {
                        FilterChipButton("Сегодня", state.draftDatePreset == SlotDatePreset.Today) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Today))
                        }
                        FilterChipButton("Эта неделя", state.draftDatePreset == SlotDatePreset.NextSevenDays) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.NextSevenDays))
                        }
                        FilterChipButton("Выходные", state.draftDatePreset == SlotDatePreset.Weekend) {
                            onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Weekend))
                        }
                    }
                    DateRangePreviewRow(state)
                }

                FilterGroup(title = "Тип маршрута") {
                    FilterChipRow {
                        FilterChipButton("Новичковый", RouteType.Novice in state.draftFilters.routeTypes) {
                            onIntent(SlotListIntent.ToggleRouteType(RouteType.Novice))
                        }
                        FilterChipButton("Опытный", RouteType.Experienced in state.draftFilters.routeTypes) {
                            onIntent(SlotListIntent.ToggleRouteType(RouteType.Experienced))
                        }
                    }
                }

                InstructorFilterSection(
                    instructors = state.instructors,
                    selected = state.draftFilters.instructorIds,
                    onToggle = { onIntent(SlotListIntent.ToggleInstructor(it.id)) },
                    onRetry = { onIntent(SlotListIntent.RetryInstructors) },
                )

                AvailabilitySwitchRow(
                    checked = state.draftFilters.onlyAvailable,
                    onToggle = { onIntent(SlotListIntent.ToggleOnlyAvailable) },
                )
            }

            Button(
                onClick = { onIntent(SlotListIntent.ApplyFilters) },
                modifier = Modifier
                    .width(VolnaTheme.tokens.sizing.contentWidth)
                    .height(VolnaTheme.tokens.sizing.buttonHeight),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Применить", fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .width(138.dp)
                    .height(4.dp)
                    .background(Color(0xFFCCCCCC), RoundedCornerShape(VolnaTheme.tokens.radius.pill)),
            )
            Spacer(Modifier.height(VolnaTheme.tokens.spacing.xs))
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun FilterChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        content = { content() },
    )
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .height(40.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .clickable { onClick() }
            .padding(horizontal = VolnaTheme.tokens.spacing.sm, vertical = 10.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun DateRangePreviewRow(state: SlotListState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        DateRangeField(
            text = state.draftFilters.dateFrom.toFilterDateText("с", "не выбрано"),
            modifier = Modifier.weight(1f),
        )
        DateRangeField(
            text = state.draftFilters.dateTo.toFilterDateText("по", "не выбрано"),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateRangeField(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .height(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.sm),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.sm),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.sm, vertical = 10.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AvailabilitySwitchRow(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "Только со свободными местами",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AvailabilitySwitch(checked = checked)
    }
}

@Composable
private fun AvailabilitySwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .background(
                color = if (checked) MaterialTheme.colorScheme.primary else Color(0xFFCCCCCC),
                shape = RoundedCornerShape(100.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = if (checked) 22.dp else 2.dp, y = 2.dp)
                .background(Color.White, RoundedCornerShape(100.dp))
                .shadow(2.dp, RoundedCornerShape(100.dp)),
        )
    }
}

@Composable
private fun InstructorFilterSection(
    instructors: Loadable<List<Instructor>>,
    selected: Set<com.volna.app.domain.model.InstructorId>,
    onToggle: (Instructor) -> Unit,
    onRetry: () -> Unit,
) {
    when (instructors) {
        Loadable.Initial,
        Loadable.Loading -> FilterGroup("Инструктор") {
            Text("Загружаем инструкторов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is Loadable.Empty -> Unit
        is Loadable.Error -> FilterGroup("Инструктор") {
            Text("Не удалось загрузить инструкторов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onRetry) {
                Text("Обновить")
            }
        }
        is Loadable.Content -> FilterGroup("Инструктор") {
            Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
                instructors.value.chunked(2).forEach { row ->
                    FilterChipRow {
                        row.forEach { instructor ->
                            FilterChipButton(
                                label = instructor.name,
                                selected = instructor.id in selected,
                                onClick = { onToggle(instructor) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotInitialLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 309.dp),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            strokeWidth = 6.dp,
        )
    }
}

@Composable
private fun SlotLoadingSkeleton() {
    SkeletonCard(y = 136.dp, height = 160.dp)
    SkeletonCard(y = 308.dp, height = 160.dp)
}

@Composable
private fun SlotCards(
    slots: List<Slot>,
    onSlotClick: (Slot) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        slots.forEach { slot ->
            SlotCard(slot, onSlotClick)
        }
        Spacer(Modifier.height(VolnaTheme.tokens.sizing.navHeight + VolnaTheme.tokens.spacing.xl))
    }
}

@Composable
private fun SlotCard(
    slot: Slot,
    onSlotClick: (Slot) -> Unit,
) {
    val canOpen = slot.freeSeats > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.listCardHeight)
            .clickable(enabled = canOpen) { onSlotClick(slot) }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
            SlotPreviewPhoto()
            Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
                    SlotTag(
                        text = slot.route.type.toTagText(),
                        color = Color(0xFF92FF9A),
                    )
                    SlotTag(
                        text = slot.route.name,
                        color = Color(0xFFFFF897),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text(
                    text = slot.startAt.toSlotCardStartText(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "Инструктор: ${slot.instructor.name}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${slot.price.value} ₽",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                )
                .padding(horizontal = VolnaTheme.tokens.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = if (canOpen) "Свободно мест" else "Мест нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${slot.freeSeats} из ${slot.totalSeats}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SlotPreviewPhoto() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD8EEF0),
                        Color(0xFFF7F0D8),
                        Color(0xFFCFE4E8),
                    ),
                ),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.36f)),
                    ),
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                ),
        )
    }
}

