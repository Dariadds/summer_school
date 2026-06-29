package com.volna.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.volna.app.booking.data.KtorBookingRepository
import com.volna.app.booking.data.RandomIdempotencyKeyFactory
import com.volna.app.booking.presentation.BookingFormEffect
import com.volna.app.booking.presentation.BookingFormIntent
import com.volna.app.booking.presentation.BookingFormState
import com.volna.app.booking.presentation.BookingFormStore
import com.volna.app.booking.presentation.BookingDetailsEffect
import com.volna.app.booking.presentation.BookingDetailsIntent
import com.volna.app.booking.presentation.BookingDetailsScreen
import com.volna.app.booking.presentation.BookingDetailsState
import com.volna.app.booking.presentation.BookingDetailsStore
import com.volna.app.booking.presentation.BookingListEffect
import com.volna.app.booking.presentation.BookingListIntent
import com.volna.app.booking.presentation.BookingListScreen
import com.volna.app.booking.presentation.BookingListState
import com.volna.app.booking.presentation.BookingListStore
import com.volna.app.auth.data.DefaultSessionRepository
import com.volna.app.auth.data.KtorAuthRepository
import com.volna.app.auth.presentation.AuthEffect
import com.volna.app.auth.presentation.AuthIntent
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthStore
import com.volna.app.catalog.data.KtorInstructorRepository
import com.volna.app.catalog.data.KtorSlotRepository
import com.volna.app.catalog.presentation.SlotDetailsEffect
import com.volna.app.catalog.presentation.SlotDetailsIntent
import com.volna.app.catalog.presentation.SlotDetailsState
import com.volna.app.catalog.presentation.SlotDetailsStore
import com.volna.app.catalog.presentation.SlotListEffect
import com.volna.app.catalog.presentation.SlotListIntent
import com.volna.app.catalog.presentation.SlotListState
import com.volna.app.catalog.presentation.SlotListStore
import com.volna.app.catalog.presentation.SlotDatePreset
import com.volna.app.core.config.AppConfig
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.network.VolnaApiClient
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.core.time.AppClock
import com.volna.app.core.time.SystemAppClock
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.policy.AvailabilityPolicy
import com.volna.app.domain.policy.BookingPriceCalculator
import com.volna.app.map.RouteMapPreview
import com.volna.app.map.RouteMapSheet
import com.volna.app.profile.data.KtorProfileRepository
import com.volna.app.profile.presentation.ProfileEffect
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileScreen
import com.volna.app.profile.presentation.ProfileState
import com.volna.app.profile.presentation.ProfileStore
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class MainTab(val title: String) {
    Slots("Прогулки"),
    Bookings("Мои записи"),
    Profile("Профиль"),
}

private enum class RootState {
    CheckingSession,
    Auth,
    Main,
}

private sealed interface SlotsRoute {
    data object List : SlotsRoute
    data class Details(val slotId: SlotId) : SlotsRoute
    data class Booking(val slot: Slot) : SlotsRoute
}

private sealed interface BookingsRoute {
    data object List : BookingsRoute
    data class Details(val bookingId: BookingId) : BookingsRoute
}

@Composable
fun VolnaApp(appConfig: AppConfig = AppConfig()) {
    VolnaTheme {
        val appScope = rememberCoroutineScope()
        val clock = remember { SystemAppClock }
        val sessionRepository = remember { DefaultSessionRepository(PlatformSessionStorage) }
        val apiClient = remember { VolnaApiClient(sessionRepository) }
        val authRepository = remember { KtorAuthRepository(apiClient, sessionRepository) }
        val profileRepository = remember { KtorProfileRepository(apiClient, sessionRepository) }
        val slotRepository = remember { KtorSlotRepository(apiClient) }
        val instructorRepository = remember { KtorInstructorRepository(apiClient) }
        val bookingRepository = remember { KtorBookingRepository(apiClient) }
        val idempotencyKeyFactory = remember { RandomIdempotencyKeyFactory() }
        val authStore = remember { AuthStore(authRepository, profileRepository, appScope) }
        val profileStore = remember { ProfileStore(profileRepository, authRepository, appScope) }
        val slotListStore = remember { SlotListStore(slotRepository, instructorRepository, appScope) }
        val slotDetailsStore = remember { SlotDetailsStore(slotRepository, appScope) }
        val bookingFormStore = remember {
            BookingFormStore(bookingRepository, idempotencyKeyFactory, appScope)
        }
        val bookingListStore = remember { BookingListStore(bookingRepository, clock, appScope) }
        val bookingDetailsStore = remember { BookingDetailsStore(bookingRepository, clock, appScope) }
        val authState by authStore.state.collectAsState()
        val profileState by profileStore.state.collectAsState()
        val slotListState by slotListStore.state.collectAsState()
        val slotDetailsState by slotDetailsStore.state.collectAsState()
        val bookingFormState by bookingFormStore.state.collectAsState()
        val bookingListState by bookingListStore.state.collectAsState()
        val bookingDetailsState by bookingDetailsStore.state.collectAsState()
        var rootState by remember { mutableStateOf(RootState.CheckingSession) }

        fun resetToAuth() {
            appScope.launch {
                sessionRepository.clearToken()
            }
            authStore.accept(AuthIntent.Reset)
            profileStore.accept(ProfileIntent.Reset)
            slotListStore.accept(SlotListIntent.Reset)
            slotDetailsStore.accept(SlotDetailsIntent.Reset)
            bookingFormStore.accept(BookingFormIntent.Reset)
            bookingListStore.accept(BookingListIntent.Reset)
            bookingDetailsStore.accept(BookingDetailsIntent.Reset)
            rootState = RootState.Auth
        }

        LaunchedEffect(sessionRepository) {
            rootState = if (sessionRepository.token().isNullOrBlank()) {
                RootState.Auth
            } else {
                RootState.Main
            }
        }

        LaunchedEffect(authStore) {
            while (true) {
                when (authStore.effects()) {
                    AuthEffect.Authenticated -> rootState = RootState.Main
                }
            }
        }

        LaunchedEffect(profileStore) {
            while (true) {
                when (profileStore.effects()) {
                    ProfileEffect.SignedOut -> resetToAuth()
                }
            }
        }

        LaunchedEffect(slotListStore) {
            while (true) {
                when (slotListStore.effects()) {
                    SlotListEffect.SignedOut -> resetToAuth()
                }
            }
        }

        LaunchedEffect(slotDetailsStore) {
            while (true) {
                when (slotDetailsStore.effects()) {
                    SlotDetailsEffect.SignedOut -> resetToAuth()
                }
            }
        }

        LaunchedEffect(bookingFormStore) {
            while (true) {
                when (bookingFormStore.effects()) {
                    BookingFormEffect.SignedOut -> resetToAuth()
                }
            }
        }

        LaunchedEffect(bookingListStore) {
            while (true) {
                when (bookingListStore.effects()) {
                    BookingListEffect.SignedOut -> resetToAuth()
                }
            }
        }

        LaunchedEffect(bookingDetailsStore) {
            while (true) {
                when (bookingDetailsStore.effects()) {
                    BookingDetailsEffect.SignedOut -> resetToAuth()
                    BookingDetailsEffect.BookingChanged -> bookingListStore.accept(BookingListIntent.Refresh)
                }
            }
        }

        when (rootState) {
            RootState.CheckingSession -> SessionSplash()
            RootState.Auth -> AuthScreen(
                state = authState,
                onIntent = authStore::accept,
            )
            RootState.Main -> MainTabs(
                slotListState = slotListState,
                onSlotListIntent = slotListStore::accept,
                slotDetailsState = slotDetailsState,
                onSlotDetailsIntent = slotDetailsStore::accept,
                bookingFormState = bookingFormState,
                onBookingFormIntent = bookingFormStore::accept,
                bookingListState = bookingListState,
                onBookingListIntent = bookingListStore::accept,
                bookingDetailsState = bookingDetailsState,
                onBookingDetailsIntent = bookingDetailsStore::accept,
                clock = clock,
                appConfig = appConfig,
                profileState = profileState,
                onProfileIntent = profileStore::accept,
            )
        }
    }
}

@Composable
private fun SessionSplash() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Волна",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MainTabs(
    slotListState: SlotListState,
    onSlotListIntent: (SlotListIntent) -> Unit,
    slotDetailsState: SlotDetailsState,
    onSlotDetailsIntent: (SlotDetailsIntent) -> Unit,
    bookingFormState: BookingFormState,
    onBookingFormIntent: (BookingFormIntent) -> Unit,
    bookingListState: BookingListState,
    onBookingListIntent: (BookingListIntent) -> Unit,
    bookingDetailsState: BookingDetailsState,
    onBookingDetailsIntent: (BookingDetailsIntent) -> Unit,
    clock: AppClock,
    appConfig: AppConfig,
    profileState: ProfileState,
    onProfileIntent: (ProfileIntent) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Slots) }
    var slotsRoute by remember { mutableStateOf<SlotsRoute>(SlotsRoute.List) }
    var bookingsRoute by remember { mutableStateOf<BookingsRoute>(BookingsRoute.List) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = VolnaTheme.tokens.sizing.screenMaxWidth),
        ) {
            when (selectedTab) {
                MainTab.Slots -> when (val route = slotsRoute) {
                    SlotsRoute.List -> SlotListScreen(
                        state = slotListState,
                        onIntent = onSlotListIntent,
                        onSlotClick = { slot ->
                            slotsRoute = SlotsRoute.Details(slot.id)
                        },
                    )
                    is SlotsRoute.Details -> SlotDetailsScreen(
                        slotId = route.slotId,
                        state = slotDetailsState,
                        onIntent = onSlotDetailsIntent,
                        onBack = { slotsRoute = SlotsRoute.List },
                        onBook = { slot -> slotsRoute = SlotsRoute.Booking(slot) },
                    )
                    is SlotsRoute.Booking -> BookingFormScreen(
                        slot = route.slot,
                        state = bookingFormState,
                        onIntent = onBookingFormIntent,
                        onBack = { slotsRoute = SlotsRoute.Details(route.slot.id) },
                        onDone = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                            onSlotListIntent(SlotListIntent.Retry)
                            slotsRoute = SlotsRoute.List
                        },
                        onOpenBookings = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                            onSlotListIntent(SlotListIntent.Retry)
                            slotsRoute = SlotsRoute.List
                            bookingsRoute = BookingsRoute.List
                            onBookingListIntent(BookingListIntent.Refresh)
                            selectedTab = MainTab.Bookings
                        },
                    )
                }
                MainTab.Bookings -> when (val route = bookingsRoute) {
                    BookingsRoute.List -> BookingListScreen(
                        state = bookingListState,
                        onIntent = onBookingListIntent,
                        onBookingClick = { bookingId -> bookingsRoute = BookingsRoute.Details(bookingId) },
                        onBookWalk = {
                            slotsRoute = SlotsRoute.List
                            selectedTab = MainTab.Slots
                        },
                    )
                    is BookingsRoute.Details -> BookingDetailsScreen(
                        bookingId = route.bookingId,
                        state = bookingDetailsState,
                        clock = clock,
                        onIntent = onBookingDetailsIntent,
                        onBack = { bookingsRoute = BookingsRoute.List },
                    )
                }
                MainTab.Profile -> ProfileScreen(
                    state = profileState,
                    appConfig = appConfig,
                    onIntent = onProfileIntent,
                )
            }
            if (selectedTab != MainTab.Bookings || bookingsRoute == BookingsRoute.List) {
                FloatingNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        if (it == MainTab.Slots) {
                            slotsRoute = SlotsRoute.List
                        }
                        if (it == MainTab.Bookings) {
                            bookingsRoute = BookingsRoute.List
                        }
                        selectedTab = it
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SlotListScreen(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
    onSlotClick: (Slot) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(SlotListIntent.Load)
    }
    ScreenTitle("Прогулки")
    Text(
        text = "≡",
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.sizing.filterIconX, y = VolnaTheme.tokens.sizing.topTitleY)
            .size(VolnaTheme.tokens.spacing.xl)
            .clickable { onIntent(SlotListIntent.OpenFilters) },
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
    )
    when (val slots = state.slots) {
        Loadable.Initial,
        Loadable.Loading -> {
            SkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
            SkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
        }
        is Loadable.Content -> SlotCards(slots.value, onSlotClick)
        is Loadable.Empty -> if (slots.reason == com.volna.app.core.ui.EmptyReason.NoSlotsByFilters) {
            StateMessage(
                title = "Нет слотов по условиям",
                description = "Попробуйте изменить фильтры",
                buttonText = "Фильтры",
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
            buttonText = "Повторить",
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
private fun SkeletonCard(
    y: androidx.compose.ui.unit.Dp,
) {
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

@Composable
private fun SlotTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = color,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.sm),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.xs, vertical = VolnaTheme.tokens.spacing.xxs),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SlotDetailsScreen(
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
            CircleActionButton(text = "‹", onClick = onBack)
            CircleActionButton(text = "↗", onClick = {})
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
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, RoundedCornerShape(200.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(200.dp))
            .clickable { onClick() }
            .padding(top = 4.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
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

@Composable
private fun BookingFormScreen(
    slot: Slot,
    state: BookingFormState,
    onIntent: (BookingFormIntent) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenBookings: () -> Unit,
) {
    LaunchedEffect(slot.id) {
        onIntent(BookingFormIntent.Open(slot))
    }
    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VolnaTheme.tokens.spacing.md)
                .offset(y = 74.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
        ) {
            CircleActionButton(text = "‹", onClick = onBack)
            Text(
                text = "Оформление записи",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        BookingFormContent(
            state = state,
            onIntent = onIntent,
        )
        state.createdBooking?.let { booking ->
            BookingSuccessSheet(
                booking = booking,
                fallbackPrice = state.totalPrice?.value ?: 0,
                onDone = onDone,
                onOpenBookings = onOpenBookings,
            )
        }
    }
}

@Composable
private fun BookingFormContent(
    state: BookingFormState,
    onIntent: (BookingFormIntent) -> Unit,
) {
    val slot = state.slot
    val maxSeats = state.availability?.maxSeatsForBooking ?: 1
    val seatPrice = slot?.price?.value ?: 0
    val rentalPrice = slot?.rentalPrice?.value ?: 0
    val seatsTotal = seatPrice * state.seatsCount
    val rentalTotal = rentalPrice * state.rentalCount
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.lg),
    ) {
        slot?.let {
            BookingSlotSummaryCard(it)
        }
        BookingSeatsCard(
            value = state.seatsCount,
            maxSeats = maxSeats,
            onMinus = { onIntent(BookingFormIntent.DecrementSeats) },
            onPlus = { onIntent(BookingFormIntent.IncrementSeats) },
        )
        if (slot != null) {
            BookingBoardsSection(
                seatsCount = state.seatsCount,
                rentalCount = state.rentalCount,
                freeRentalBoards = slot.freeRentalBoards,
                onTargetRentalCount = { target ->
                    when {
                        target > state.rentalCount -> repeat(target - state.rentalCount) {
                            onIntent(BookingFormIntent.IncrementRental)
                        }
                        target < state.rentalCount -> repeat(state.rentalCount - target) {
                            onIntent(BookingFormIntent.DecrementRental)
                        }
                    }
                },
            )
        }
        BookingPriceDetails(
            seatsCount = state.seatsCount,
            rentalCount = state.rentalCount,
            seatPrice = seatPrice,
            rentalPrice = rentalPrice,
            seatsTotal = seatsTotal,
            rentalTotal = rentalTotal,
            total = state.totalPrice?.value ?: 0,
        )
        state.validationMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { onIntent(BookingFormIntent.Submit) },
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolnaTheme.tokens.sizing.buttonHeight),
            shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(if (state.isSubmitting) "Записываем..." else "Записаться", fontWeight = FontWeight.Bold)
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
private fun BookingSlotSummaryCard(slot: Slot) {
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
        Text(
            text = slot.startAt.toSlotCardStartText(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xxs)) {
            SlotTag(text = slot.route.type.toTagText(), color = Color(0xFF92FF9A))
            SlotTag(
                text = slot.route.name,
                color = Color(0xFFFFF897),
                modifier = Modifier.weight(1f, fill = false),
            )
            SlotTag(
                text = "Инструктор: ${slot.instructor.name}",
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun BookingSeatsCard(
    value: Int,
    maxSeats: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
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
            text = "Число мест",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            BookingCounterButton("−", onMinus)
            Text(
                text = value.toString(),
                modifier = Modifier
                    .width(52.dp)
                    .height(54.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(VolnaTheme.tokens.radius.lg))
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BookingCounterButton("+", onPlus)
        }
        Text(
            text = "Можно записать до $maxSeats мест",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookingCounterButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .size(54.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(VolnaTheme.tokens.radius.lg))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(VolnaTheme.tokens.radius.lg))
            .clickable { onClick() }
            .padding(top = 12.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BookingBoardsSection(
    seatsCount: Int,
    rentalCount: Int,
    freeRentalBoards: Int,
    onTargetRentalCount: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm)) {
        Text(
            text = "Доска для каждого места",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        repeat(seatsCount) { index ->
            val seatNumber = index + 1
            val rentalSelected = seatNumber <= rentalCount
            BookingBoardRow(
                label = if (seatNumber == 1) "Место 1 (вы)" else "Место $seatNumber (гость)",
                rentalSelected = rentalSelected,
                rentalEnabled = seatNumber <= freeRentalBoards,
                onOwn = { onTargetRentalCount((seatNumber - 1).coerceAtLeast(0)) },
                onRental = { onTargetRentalCount(seatNumber.coerceAtMost(freeRentalBoards)) },
            )
        }
        Text(
            text = "Прокатных выбрано: $rentalCount из $freeRentalBoards",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookingBoardRow(
    label: String,
    rentalSelected: Boolean,
    rentalEnabled: Boolean,
    onOwn: () -> Unit,
    onRental: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(VolnaTheme.tokens.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(VolnaTheme.tokens.radius.lg))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(VolnaTheme.tokens.radius.lg)),
        ) {
            BoardSegment(
                text = "Своя",
                selected = !rentalSelected,
                onClick = onOwn,
            )
            BoardSegment(
                text = "Прокатная",
                selected = rentalSelected,
                enabled = rentalEnabled,
                onClick = onRental,
            )
        }
    }
}

@Composable
private fun BoardSegment(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .width(100.dp)
            .height(44.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(top = 12.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun BookingPriceDetails(
    seatsCount: Int,
    rentalCount: Int,
    seatPrice: Int,
    rentalPrice: Int,
    seatsTotal: Int,
    rentalTotal: Int,
    total: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(0.dp))
            .padding(top = VolnaTheme.tokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        BookingPriceRow("Места: $seatPrice ₽ × $seatsCount", "$seatsTotal ₽")
        BookingPriceRow("Прокат: $rentalPrice ₽ × $rentalCount", "$rentalTotal ₽")
        BookingPriceRow("Итого", "$total ₽", bold = true)
    }
}

@Composable
private fun BookingPriceRow(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookingSuccessSheet(
    booking: Booking,
    fallbackPrice: Int,
    onDone: () -> Unit,
    onOpenBookings: () -> Unit,
) {
    // CMP-15 / BS-002: successful createBooking summary; no network requests on open.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .clickable { onDone() },
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
            Text("Вы записаны", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            booking.slot?.let { slot ->
                Text(slot.route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(slot.startAt.toUiText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Инструктор: ${slot.instructor.name}")
            }
            Text("Мест: ${booking.seatsCount}")
            Text("Прокатных досок: ${booking.rentalCount}")
            Text("Своя доска: ${(booking.seatsCount - booking.rentalCount).coerceAtLeast(0)}")
            Text(
                "${BookingPriceCalculator.calculate(booking)?.value ?: fallbackPrice} ₽",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text("Оплата на месте: наличные или перевод на карту.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenBookings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolnaTheme.tokens.sizing.buttonHeight),
            ) {
                Text("Мои записи")
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Готово")
            }
        }
    }
}

@Composable
private fun CounterRow(
    title: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.md),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "−",
                modifier = Modifier
                    .size(VolnaTheme.tokens.spacing.xl)
                    .clickable { onMinus() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(value.toString(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Text(
                text = "+",
                modifier = Modifier
                    .size(VolnaTheme.tokens.spacing.xl)
                    .clickable { onPlus() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
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
private fun ScreenTitle(title: String) {
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
private fun StateMessage(
    title: String,
    description: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null,
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
        if (buttonText != null && onClick != null) {
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(bottom = VolnaTheme.tokens.sizing.navBottomPadding)
            .width(VolnaTheme.tokens.sizing.navWidth)
            .height(VolnaTheme.tokens.sizing.navHeight)
            .shadow(
                elevation = VolnaTheme.tokens.spacing.sm,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        NavItem(
            tab = MainTab.Slots,
            selected = selectedTab == MainTab.Slots,
            icon = "▣",
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Bookings,
            selected = selectedTab == MainTab.Bookings,
            icon = "☷",
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Profile,
            selected = selectedTab == MainTab.Profile,
            icon = "○",
            onClick = onTabSelected,
        )
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    selected: Boolean,
    icon: String,
    onClick: (MainTab) -> Unit,
) {
    Text(
        text = icon,
        modifier = Modifier
            .size(VolnaTheme.tokens.spacing.xl)
            .clickable { onClick(tab) },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

private fun RouteType.toUiText(): String = when (this) {
    RouteType.Novice -> "для новичков"
    RouteType.Experienced -> "для опытных"
}

private fun RouteType.toTagText(): String = when (this) {
    RouteType.Novice -> "Новичковый"
    RouteType.Experienced -> "Опытный"
}

private fun RouteType.toDetailsAudienceText(): String = when (this) {
    RouteType.Novice -> "для новичков"
    RouteType.Experienced -> "для опытных райдеров"
}

private fun kotlinx.datetime.Instant.toSlotCardStartText(): String {
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
    val month = when (dateTime.month) {
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
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$weekday, ${dateTime.dayOfMonth} $month · ${dateTime.hour}:$minute"
}

private fun kotlinx.datetime.Instant?.toFilterDateText(prefix: String, fallback: String): String =
    if (this == null) {
        "$prefix: $fallback"
    } else {
        val date = toLocalDateTime(TimeZone.currentSystemDefault()).date
        "$prefix: ${date.dayOfMonth} ${date.month.toMonthName()}"
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

private fun kotlinx.datetime.Instant.toUiText(): String =
    toString()
        .replace("T", " ")
        .removeSuffix("Z")
