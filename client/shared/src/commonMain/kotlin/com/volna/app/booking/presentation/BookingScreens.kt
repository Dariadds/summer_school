package com.volna.app.booking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

// CMP-12 / SCR-005: minimal "Мои записи" screen backed by BookingListStore.
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

// CMP-12 / SCR-006 / BS-003: booking details with explicit cancel confirmation.
@Composable
fun BookingDetailsScreen(
    bookingId: BookingId,
    state: BookingDetailsState,
    clock: AppClock,
    onIntent: (BookingDetailsIntent) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(bookingId) {
        onIntent(BookingDetailsIntent.Load(bookingId))
    }
    Box(Modifier.fillMaxSize()) {
        BookingBackButton(onBack)
        BookingScreenTitle("Детали записи")
        when (val booking = state.booking) {
            Loadable.Initial,
            Loadable.Loading -> {
                BookingSkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
                BookingSkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
            }
            is Loadable.Content -> BookingDetailsContent(
                booking = booking.value,
                state = state,
                clock = clock,
                onIntent = onIntent,
            )
            is Loadable.Empty -> BookingStateMessage(
                title = "Запись недоступна",
                description = "Вернитесь к списку и попробуйте снова",
                buttonText = "Назад",
                onClick = onBack,
            )
            is Loadable.Error -> BookingStateMessage(
                title = "Не удалось загрузить запись",
                description = "Проверьте соединение и попробуйте снова",
                buttonText = "Обновить",
                onClick = { onIntent(BookingDetailsIntent.Retry) },
            )
        }
        if (state.showCancelConfirm) {
            CancelConfirmSheet(
                state = state,
                clock = clock,
                onIntent = onIntent,
            )
        }
        if (state.showRouteMap) {
            state.currentBooking?.slot?.let { slot ->
                RouteMapSheet(
                    route = slot.route,
                    meetingPoint = slot.meetingPoint,
                    onDismiss = { onIntent(BookingDetailsIntent.DismissRouteMap) },
                )
            }
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
private fun BookingPreviewPhoto() {
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
private fun BookingTag(
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

@Composable
private fun BookingDetailsContent(
    booking: Booking,
    state: BookingDetailsState,
    clock: AppClock,
    onIntent: (BookingDetailsIntent) -> Unit,
) {
    val slot = booking.slot
    val canCancel = state.canCancel(clock)
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        BookingDetailsEventCard(
            booking = booking,
            status = booking.statusLabel(clock),
        )
        slot?.let {
            BookingDetailsMapCard(
                address = it.meetingPoint.title.ifBlank { "уточняется" },
                onOpenMap = { onIntent(BookingDetailsIntent.OpenRouteMap) },
            )
        }
        BookingDetailsPriceBlock(booking)
        if (canCancel) {
            Text(
                text = cancelDeadlineText(booking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        booking.cancelledAt?.let {
            Text(
                text = "Отменено: ${it.toUiText()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = { onIntent(BookingDetailsIntent.AskCancel) },
            enabled = canCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolnaTheme.tokens.sizing.buttonHeight),
            shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        ) {
            Text(if (canCancel) "Отменить" else "Отмена недоступна")
        }
        Box(
            modifier = Modifier
                .width(138.dp)
                .height(4.dp)
                .align(androidx.compose.ui.Alignment.CenterHorizontally)
                .background(Color(0xFFCCCCCC), RoundedCornerShape(VolnaTheme.tokens.radius.pill)),
        )
        Spacer(Modifier.height(VolnaTheme.tokens.spacing.xs))
    }
}

@Composable
private fun BookingDetailsEventCard(
    booking: Booking,
    status: String,
) {
    val slot = booking.slot
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Box {
            BookingPreviewPhoto()
            BookingStatusPill(
                status = status,
                modifier = Modifier
                    .offset(x = VolnaTheme.tokens.spacing.xs, y = VolnaTheme.tokens.spacing.xs),
            )
        }
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
        )
        Text(
            text = "Инструктор: ${slot?.instructor?.name ?: "уточняется"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BookingStatusPill(
    status: String,
    modifier: Modifier = Modifier,
) {
    val active = status == "Активна"
    Text(
        text = status,
        modifier = modifier
            .width(100.dp)
            .height(36.dp)
            .background(
                color = if (active) Color(0xFFE4FFE5) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(top = 9.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (active) Color(0xFF007108) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BookingDetailsMapCard(
    address: String,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.spacing.xl),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Text(
            text = "Адрес: $address",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        BookingDetailsMapPreview()
        Text(
            text = "Открыть карту",
            modifier = Modifier.clickable { onOpenMap() },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF0093CC),
        )
    }
}

@Composable
private fun BookingDetailsMapPreview() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .background(Color.White, RoundedCornerShape(VolnaTheme.tokens.radius.sm)),
    ) {
        val corner = 12.dp.toPx()
        drawRoundRect(Color(0xFF8AD0F0), cornerRadius = CornerRadius(corner, corner))
        drawRoundRect(
            color = Color(0xFFDDF3CC),
            topLeft = Offset(size.width * 0.02f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height),
            cornerRadius = CornerRadius(corner, corner),
        )
        drawRoundRect(
            color = Color(0xFFDDF3CC),
            topLeft = Offset(size.width * 0.84f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.16f, size.height),
            cornerRadius = CornerRadius(corner, corner),
        )
        listOf(0.22f, 0.50f, 0.78f).forEach { y ->
            drawLine(
                color = Color(0xFFF9F6F0),
                start = Offset(0f, size.height * y),
                end = Offset(size.width, size.height * (y - 0.12f)),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val routePoints = listOf(
            Offset(size.width * 0.34f, size.height * 0.88f),
            Offset(size.width * 0.48f, size.height * 0.58f),
            Offset(size.width * 0.62f, size.height * 0.36f),
        )
        routePoints.zipWithNext().forEach { (start, end) ->
            drawLine(Color(0xFF00A59D), start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFFFF6B4A), radius = 6.dp.toPx(), center = routePoints.first())
        drawCircle(Color.White, radius = 2.5.dp.toPx(), center = routePoints.first())
    }
}

@Composable
private fun BookingDetailsPriceBlock(booking: Booking) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(0.dp))
            .padding(top = VolnaTheme.tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("ⓘ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Оплата на месте: наличные или перевод",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookingInfoBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        content = content,
    )
}

@Composable
private fun CancelConfirmSheet(
    state: BookingDetailsState,
    clock: AppClock,
    onIntent: (BookingDetailsIntent) -> Unit,
) {
    val kind = state.cancellationKind(clock)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .clickable(enabled = !state.isCancelling) { onIntent(BookingDetailsIntent.DismissCancel) },
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
                .verticalScroll(rememberScrollState())
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Text("Отменить запись?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = when (kind) {
                    CancellationKind.Early -> "До старта больше 2 часов: места и прокатные доски освободятся."
                    CancellationKind.Late -> "До старта меньше 2 часов: место не освободится, штрафов нет."
                    else -> "Отмена уже недоступна."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { onIntent(BookingDetailsIntent.ConfirmCancel) },
                enabled = !state.isCancelling && kind != CancellationKind.UnavailableAfterStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isCancelling) "Отменяем..." else "Подтвердить отмену")
            }
            OutlinedButton(
                onClick = { onIntent(BookingDetailsIntent.DismissCancel) },
                enabled = !state.isCancelling,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Не отменять")
            }
        }
    }
}

@Composable
private fun BookingSkeletonCard(y: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .height(VolnaTheme.tokens.sizing.listCardHeight)
            .offset(x = VolnaTheme.tokens.spacing.md, y = y)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            ),
    )
}

@Composable
private fun BookingScreenTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = VolnaTheme.tokens.sizing.topTitleY),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun BookingBackButton(onClick: () -> Unit) {
    Text(
        text = "‹",
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.backButtonY)
            .size(VolnaTheme.tokens.spacing.xl)
            .clickable { onClick() },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BookingStateMessage(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listStateMessageY),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onClick) {
            Text(buttonText)
        }
    }
}

private fun Booking.statusLabel(clock: AppClock? = null, pastGroup: Boolean = false): String = when {
    status == BookingStatus.Cancelled -> "Отменена"
    status == BookingStatus.LateCancel -> "Поздняя отмена"
    pastGroup -> "Прошедшая"
    clock != null && slot?.startAt?.let { it <= clock.now() } == true -> "Прошедшая"
    else -> "Активна"
}

private fun RouteType.toUiText(): String = when (this) {
    RouteType.Novice -> "для новичков"
    RouteType.Experienced -> "для опытных"
}

private fun RouteType.toTagText(): String = when (this) {
    RouteType.Novice -> "Новичковый"
    RouteType.Experienced -> "Опытный"
}

private fun cancelDeadlineText(booking: Booking): String =
    "Бесплатно освободить место можно до ${booking.slot?.startAt?.minus(2.hours)?.toUiText() ?: "уточняется"}"

private fun Instant.toBookingCardStartText(): String {
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())
    val weekday = when (dateTime.dayOfWeek) {
        DayOfWeek.MONDAY -> "Пн"
        DayOfWeek.TUESDAY -> "Вт"
        DayOfWeek.WEDNESDAY -> "Ср"
        DayOfWeek.THURSDAY -> "Чт"
        DayOfWeek.FRIDAY -> "Пт"
        DayOfWeek.SATURDAY -> "Сб"
        DayOfWeek.SUNDAY -> "Вс"
    }
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$weekday, ${dateTime.dayOfMonth} ${dateTime.month.toMonthName()} · ${dateTime.hour}:$minute"
}

private fun Month.toMonthName(): String = when (this) {
    Month.JANUARY -> "января"
    Month.FEBRUARY -> "февраля"
    Month.MARCH -> "марта"
    Month.APRIL -> "апреля"
    Month.MAY -> "мая"
    Month.JUNE -> "июня"
    Month.JULY -> "июля"
    Month.AUGUST -> "августа"
    Month.SEPTEMBER -> "сентября"
    Month.OCTOBER -> "октября"
    Month.NOVEMBER -> "ноября"
    Month.DECEMBER -> "декабря"
}

private fun Int.pluralPlaces(): String = when {
    this % 10 == 1 && this % 100 != 11 -> "место"
    this % 10 in 2..4 && this % 100 !in 12..14 -> "места"
    else -> "мест"
}

private fun Int.pluralRentalBoards(): String = when {
    this % 10 == 1 && this % 100 != 11 -> "прокатная доска"
    this % 10 in 2..4 && this % 100 !in 12..14 -> "прокатные доски"
    else -> "прокатных досок"
}

private fun Instant.toUiText(): String =
    toString()
        .replace("T", " ")
        .removeSuffix("Z")
