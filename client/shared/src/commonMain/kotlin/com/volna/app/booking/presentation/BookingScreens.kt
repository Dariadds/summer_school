package com.volna.app.booking.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.time.AppClock
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.BookingStatus
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.policy.CancellationKind
import com.volna.app.map.RouteMapSheet
import com.volna.app.map.RouteMapPreview
import com.volna.app.map.toMapUiState
import kotlinx.datetime.Instant
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
    onBookingClick: (BookingId) -> Unit,
    onBookWalk: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        BookingSection(
            title = "Предстоящие",
            bookings = groups.upcoming,
            pastGroup = false,
            emptyTitle = "Пока нет предстоящих записей",
            emptyDescription = "Можно выбрать ближайшую прогулку",
            onBookingClick = onBookingClick,
            onBookWalk = onBookWalk,
        )
        BookingSection(
            title = "Прошедшие",
            bookings = groups.past,
            pastGroup = true,
            emptyTitle = "Здесь появятся прошедшие прогулки",
            emptyDescription = "Отменённые записи тоже будут здесь",
            onBookingClick = onBookingClick,
            onBookWalk = onBookWalk,
        )
        Spacer(Modifier.height(VolnaTheme.tokens.sizing.navHeight))
    }
}

@Composable
private fun BookingSection(
    title: String,
    bookings: List<Booking>,
    pastGroup: Boolean,
    emptyTitle: String,
    emptyDescription: String,
    onBookingClick: (BookingId) -> Unit,
    onBookWalk: () -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (bookings.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                )
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        ) {
            Text(emptyTitle, fontWeight = FontWeight.Bold)
            Text(emptyDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onBookWalk, modifier = Modifier.fillMaxWidth()) {
                Text("Записаться")
            }
        }
    } else {
        bookings.forEach { booking ->
            BookingCard(booking = booking, pastGroup = pastGroup, onClick = { onBookingClick(booking.id) })
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    pastGroup: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(booking.slot?.startAt?.toUiText() ?: "Время уточняется", fontWeight = FontWeight.Bold)
        Text(booking.slot?.route?.name ?: "Маршрут уточняется")
        Text("Инструктор: ${booking.slot?.instructor?.name ?: "уточняется"}")
        Text("${booking.seatsCount} мест · ${booking.rentalCount} прокатных")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${booking.priceTotal?.value ?: 0} ₽", fontWeight = FontWeight.Bold)
            Text(booking.statusLabel(pastGroup = pastGroup), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BookingDetailsContent(
    booking: Booking,
    state: BookingDetailsState,
    clock: AppClock,
    onIntent: (BookingDetailsIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        BookingInfoBlock {
            Text(booking.statusLabel(clock), fontWeight = FontWeight.Bold)
            Text(booking.slot?.startAt?.toUiText() ?: "Время уточняется", style = MaterialTheme.typography.titleLarge)
        }
        BookingInfoBlock {
            Text("Маршрут", fontWeight = FontWeight.Bold)
            Text(booking.slot?.route?.name ?: "Маршрут уточняется")
            Text(booking.slot?.route?.type?.toUiText() ?: "Тип уточняется")
            Text("Инструктор: ${booking.slot?.instructor?.name ?: "уточняется"}")
        }
        booking.slot?.let { slot ->
            Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
                Text("Карта маршрута", fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.clickable { onIntent(BookingDetailsIntent.OpenRouteMap) },
                ) {
                    RouteMapPreview(
                        route = slot.route,
                        meetingPoint = slot.meetingPoint,
                        state = slot.route.toMapUiState(),
                        onRetry = {},
                        onOpenExternal = { onIntent(BookingDetailsIntent.OpenRouteMap) },
                    )
                }
                Text(
                    text = "Место встречи: ${slot.meetingPoint.title.ifBlank { "уточняется" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BookingInfoBlock {
            Text("Места и доски", fontWeight = FontWeight.Bold)
            Text("Мест: ${booking.seatsCount}")
            Text("Прокатных досок: ${booking.rentalCount}")
            Text("Своя доска: ${(booking.seatsCount - booking.rentalCount).coerceAtLeast(0)}")
        }
        BookingInfoBlock {
            Text("Цена", fontWeight = FontWeight.Bold)
            Text("${booking.priceTotal?.value ?: 0} ₽", style = MaterialTheme.typography.headlineSmall)
            Text("Оплата на месте: наличные или перевод на карту.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BookingInfoBlock {
            Text("Записано: ${booking.createdAt.toUiText()}")
            booking.cancelledAt?.let { Text("Отменено: ${it.toUiText()}") }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
        val canCancel = state.canCancel(clock)
        if (canCancel) {
            Text(cancelDeadlineText(booking), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = { onIntent(BookingDetailsIntent.AskCancel) },
            enabled = canCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolnaTheme.tokens.sizing.buttonHeight),
        ) {
            Text(if (canCancel) "Отменить" else "Отмена недоступна")
        }
        Spacer(Modifier.height(VolnaTheme.tokens.spacing.xl))
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .width(VolnaTheme.tokens.sizing.contentWidth)
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
            Text("Отменить запись?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = when (kind) {
                    CancellationKind.Early -> "До старта больше 2 часов: места и прокатные доски освободятся."
                    CancellationKind.Late -> "До старта меньше 2 часов: запись станет поздней отменой, место не освободится."
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

private fun cancelDeadlineText(booking: Booking): String =
    "Бесплатно освободить место можно до ${booking.slot?.startAt?.minus(2.hours)?.toUiText() ?: "уточняется"}"

private fun Instant.toUiText(): String =
    toString()
        .replace("T", " ")
        .removeSuffix("Z")
