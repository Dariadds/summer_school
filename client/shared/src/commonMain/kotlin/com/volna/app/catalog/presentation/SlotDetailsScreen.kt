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
fun SlotDetailsScreen(
    slotId: SlotId,
    state: SlotDetailsState,
    onIntent: (SlotDetailsIntent) -> Unit,
    onBack: () -> Unit,
    onBook: (Slot) -> Unit,
) {
    var showRouteMap by remember(slotId) { mutableStateOf(false) }
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
                onBack = onBack,
                onBook = { onBook(slot.value) },
                onOpenMap = { showRouteMap = true },
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
        if (showRouteMap) {
            (state.slot as? Loadable.Content)?.value?.let { slot ->
                RouteMapSheet(
                    route = slot.route,
                    meetingPoint = slot.meetingPoint,
                    onDismiss = { showRouteMap = false },
                )
            }
        }
    }
}

@Composable
private fun SlotDetailsContent(
    slot: Slot,
    onBack: () -> Unit,
    onBook: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val availability = AvailabilityPolicy.availability(slot)
    Box(Modifier.fillMaxSize()) {
        SlotDetailsHero()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VolnaTheme.tokens.spacing.md)
                .offset(y = 74.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleActionButton(icon = Icons.Back, contentDescription = "Назад", onClick = onBack)
            CircleActionButton(icon = Icons.Share, contentDescription = "Поделиться", onClick = {})
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 136.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = VolnaTheme.tokens.spacing.xl,
                        topEnd = VolnaTheme.tokens.spacing.xl,
                    ),
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFCCCCCC).copy(alpha = 0.4f), RoundedCornerShape(VolnaTheme.tokens.radius.lg)),
            )
            SlotDetailsSheetContent(
                slot = slot,
                availability = availability,
                onOpenMap = onOpenMap,
            )
            Button(
                onClick = onBook,
                enabled = availability.canBook,
                modifier = Modifier
                    .width(VolnaTheme.tokens.sizing.contentWidth)
                    .height(VolnaTheme.tokens.sizing.buttonHeight),
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(if (availability.canBook) "Записаться" else "Мест нет", fontWeight = FontWeight.Bold)
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
            tint = MaterialTheme.colorScheme.primary,
            size = 20.dp,
        )
    }
}

@Composable
private fun SlotDetailsSheetContent(
    slot: Slot,
    availability: com.volna.app.domain.policy.Availability,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier.width(VolnaTheme.tokens.sizing.contentWidth),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
            SlotTag(text = slot.route.type.toTagText(), color = Color(0xFF92FF9A))
            SlotTag(
                text = slot.route.name,
                color = Color(0xFFFFF897),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
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
            Text(
                text = slot.startAt.toSlotCardStartText(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Прогулка по маршруту «${slot.route.name}» займет ${slot.route.durationMin} минут и отлично подойдет ${slot.route.type.toDetailsAudienceText()}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Инструктор: ${slot.instructor.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SlotDetailsMapCard(slot = slot, onOpenMap = onOpenMap)
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
            DetailsInfoRow("Свободно мест", "${slot.freeSeats} из ${slot.totalSeats}")
            DetailsInfoRow("Прокатная доска (доступно ${availability.freeRentalBoards} шт.)", "${slot.rentalPrice.value} ₽", boldValue = true)
            DetailsInfoRow("Цена", "${slot.price.value} ₽", boldValue = true)
        }
        Text(
            text = "Оплата на месте: наличные или перевод",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotDetailsMapCard(
    slot: Slot,
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
            text = "Адрес: ${slot.meetingPoint.title.ifBlank { "уточняется" }}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SlotDetailsMapPreview()
        Text(
            text = "Открыть карту",
            modifier = Modifier.clickable { onOpenMap() },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF0093CC),
        )
    }
}

@Composable
private fun SlotDetailsMapPreview() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .background(Color.White, RoundedCornerShape(VolnaTheme.tokens.radius.sm)),
    ) {
        val corner = 12.dp.toPx()
        drawRoundRect(
            color = Color(0xFF8AD0F0),
            cornerRadius = CornerRadius(corner, corner),
        )
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
            drawLine(
                color = Color(0xFF00A59D),
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(Color(0xFFFF6B4A), radius = 6.dp.toPx(), center = routePoints.first())
        drawCircle(Color.White, radius = 2.5.dp.toPx(), center = routePoints.first())
    }
}

@Composable
private fun DetailsInfoRow(
    label: String,
    value: String,
    boldValue: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

