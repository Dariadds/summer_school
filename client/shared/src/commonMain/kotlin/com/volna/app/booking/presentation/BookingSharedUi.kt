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
internal fun BookingSkeletonCard(y: androidx.compose.ui.unit.Dp) {
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
internal fun BookingScreenTitle(title: String) {
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
internal fun BookingBackButton(onClick: () -> Unit) {
    VolnaIcon(
        imageVector = Icons.Back,
        contentDescription = "Назад",
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.backButtonY)
            .clickable { onClick() },
        tint = MaterialTheme.colorScheme.onSurface,
        size = VolnaTheme.tokens.spacing.xl,
    )
}

@Composable
internal fun BookingStateMessage(
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

internal fun Booking.statusLabel(clock: AppClock? = null, pastGroup: Boolean = false): String = when {
    status == BookingStatus.Cancelled -> "Отменена"
    status == BookingStatus.LateCancel -> "Поздняя отмена"
    pastGroup -> "Прошедшая"
    clock != null && slot?.startAt?.let { it <= clock.now() } == true -> "Прошедшая"
    else -> "Активна"
}

internal fun RouteType.toUiText(): String = when (this) {
    RouteType.Novice -> "для новичков"
    RouteType.Experienced -> "для опытных"
}

internal fun RouteType.toTagText(): String = when (this) {
    RouteType.Novice -> "Новичковый"
    RouteType.Experienced -> "Опытный"
}

internal fun cancelDeadlineText(booking: Booking): String =
    "Бесплатно освободить место можно до ${booking.slot?.startAt?.minus(2.hours)?.toUiText() ?: "уточняется"}"

internal fun Instant.toBookingCardStartText(): String {
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

internal fun Int.pluralPlaces(): String = when {
    this % 10 == 1 && this % 100 != 11 -> "место"
    this % 10 in 2..4 && this % 100 !in 12..14 -> "места"
    else -> "мест"
}

internal fun Int.pluralRentalBoards(): String = when {
    this % 10 == 1 && this % 100 != 11 -> "прокатная доска"
    this % 10 in 2..4 && this % 100 !in 12..14 -> "прокатные доски"
    else -> "прокатных досок"
}

internal fun Instant.toUiText(): String =
    toString()
        .replace("T", " ")
        .removeSuffix("Z")
