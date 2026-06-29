package com.volna.app.booking.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.time.AppClock
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.BookingStatus
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.policy.BookingPriceCalculator
import com.volna.app.domain.policy.CancellationKind
import com.volna.app.map.RouteMapSheet
import com.volna.app.uikit.icons.Back
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Info
import com.volna.app.uikit.icons.VolnaIcon
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

@Composable
fun BookingListScreen(
    state: BookingListState,
    onIntent: (BookingListIntent) -> Unit,
    onBookingClick: (BookingId) -> Unit,
    onBookWalk: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(BookingListIntent.Load)
    }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(2_500)
            onIntent(BookingListIntent.MessageShown)
        }
    }
    Box(Modifier.fillMaxSize()) {
        BookingScreenTitle("Мои записи")
        when (val bookings = state.bookings) {
            Loadable.Initial,
            Loadable.Loading -> {
                BookingSkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
                BookingSkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
            }
            is Loadable.Content -> BookingGroupsContent(
                groups = bookings.value,
                refreshing = bookings.refreshing,
                message = state.message,
                onBookingClick = onBookingClick,
                onBookWalk = onBookWalk,
            )
            is Loadable.Empty -> BookingStateMessage(
                title = "У вас пока нет записей",
                description = "Выберите прогулку и оформите бронь",
                buttonText = "Записаться",
                onClick = onBookWalk,
            )
            is Loadable.Error -> BookingStateMessage(
                title = "Не удалось загрузить записи",
                description = "Проверьте соединение и попробуйте снова",
                buttonText = "Обновить",
                onClick = { onIntent(BookingListIntent.Retry) },
            )
        }
    }
}

@Composable
private fun BookingGroupsContent(
    groups: BookingGroups,
    refreshing: Boolean,
    message: String?,
    onBookingClick: (BookingId) -> Unit,
    onBookWalk: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BookingListTab.Upcoming) }
    val visibleBookings = when (selectedTab) {
        BookingListTab.Upcoming -> groups.upcoming
        BookingListTab.Past -> groups.past
    }
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
    ) {
        if (refreshing) {
            Text("Обновляем записи...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        BookingTabs(
            selected = selectedTab,
            onSelected = { selectedTab = it },
        )
        if (visibleBookings.isEmpty()) {
            BookingEmptyCard(
                title = if (selectedTab == BookingListTab.Upcoming) {
                    "Пока нет предстоящих записей"
                } else {
                    "Здесь появятся прошедшие прогулки"
                },
                description = if (selectedTab == BookingListTab.Upcoming) {
                    "Можно выбрать ближайшую прогулку"
                } else {
                    "Отменённые записи тоже будут здесь"
                },
                onBookWalk = onBookWalk,
            )
        } else {
            visibleBookings.forEach { booking ->
                BookingCard(
                    booking = booking,
                    pastGroup = selectedTab == BookingListTab.Past,
                    onClick = { onBookingClick(booking.id) },
                )
            }
        }
        Spacer(Modifier.height(VolnaTheme.tokens.sizing.navHeight + VolnaTheme.tokens.spacing.xl))
    }
}

private enum class BookingListTab {
    Upcoming,
    Past,
}

@Composable
private fun BookingTabs(
    selected: BookingListTab,
    onSelected: (BookingListTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(VolnaTheme.tokens.radius.pill))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(VolnaTheme.tokens.radius.pill)),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        BookingTabButton(
            text = "Предстоящие",
            selected = selected == BookingListTab.Upcoming,
            onClick = { onSelected(BookingListTab.Upcoming) },
        )
        BookingTabButton(
            text = "Прошедшие",
            selected = selected == BookingListTab.Past,
            onClick = { onSelected(BookingListTab.Past) },
        )
    }
}

@Composable
private fun BookingTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .width(180.dp)
            .height(40.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .clickable { onClick() }
            .padding(top = 10.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BookingEmptyCard(
    title: String,
    description: String,
    onBookWalk: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onBookWalk, modifier = Modifier.fillMaxWidth()) {
            Text("Записаться")
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    pastGroup: Boolean,
    onClick: () -> Unit,
) {
    val slot = booking.slot
    val status = booking.statusLabel(pastGroup = pastGroup)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        BookingPreviewPhoto()
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
                slot?.let {
                    BookingTag(text = it.route.type.toTagText(), color = Color(0xFF92FF9A))
                    BookingTag(
                        text = it.route.name,
                        color = Color(0xFFFFF897),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Text(
                text = slot?.startAt?.toBookingCardStartText() ?: "Время уточняется",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "Инструктор: ${slot?.instructor?.name ?: "уточняется"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "${booking.seatsCount} ${booking.seatsCount.pluralPlaces()} · ${booking.rentalCount} ${booking.rentalCount.pluralRentalBoards()}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${BookingPriceCalculator.calculate(booking)?.value ?: 0} ₽",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        BookingStatusBadge(status)
    }
}

@Composable
internal fun BookingPreviewPhoto() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFD8EEF0), Color(0xFFF7F0D8), Color(0xFFCFE4E8)),
                ),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            ),
    )
}

@Composable
internal fun BookingTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(color, RoundedCornerShape(VolnaTheme.tokens.radius.sm))
            .padding(horizontal = VolnaTheme.tokens.spacing.xs, vertical = VolnaTheme.tokens.spacing.xxs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BookingStatusBadge(status: String) {
    val active = status == "Активна"
    Text(
        text = status,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
                color = if (active) Color(0xFFE4FFE5) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(top = 9.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (active) Color(0xFF007108) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

