package com.volna.app

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.volna.app.map.toMapUiState
import com.volna.app.notifications.PlatformPushPermissionFlagStore
import com.volna.app.notifications.PlatformPushPermissionGateway
import com.volna.app.notifications.PushPermissionIntent
import com.volna.app.notifications.PushPermissionState
import com.volna.app.notifications.PushPermissionStore
import com.volna.app.profile.data.KtorProfileRepository
import com.volna.app.profile.presentation.ProfileEffect
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileScreen
import com.volna.app.profile.presentation.ProfileState
import com.volna.app.profile.presentation.ProfileStore

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
        val pushPermissionStore = remember {
            PushPermissionStore(
                gateway = PlatformPushPermissionGateway,
                flagStore = PlatformPushPermissionFlagStore,
                scope = appScope,
            )
        }
        val authState by authStore.state.collectAsState()
        val profileState by profileStore.state.collectAsState()
        val slotListState by slotListStore.state.collectAsState()
        val slotDetailsState by slotDetailsStore.state.collectAsState()
        val bookingFormState by bookingFormStore.state.collectAsState()
        val bookingListState by bookingListStore.state.collectAsState()
        val bookingDetailsState by bookingDetailsStore.state.collectAsState()
        val pushPermissionState by pushPermissionStore.state.collectAsState()
        var rootState by remember { mutableStateOf(RootState.CheckingSession) }

        fun resetToAuth() {
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
                pushPermissionState = pushPermissionState,
                onPushPermissionIntent = pushPermissionStore::accept,
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
    pushPermissionState: PushPermissionState,
    onPushPermissionIntent: (PushPermissionIntent) -> Unit,
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
                        appConfig = appConfig,
                        onIntent = onBookingFormIntent,
                        pushPermissionState = pushPermissionState,
                        onPushPermissionIntent = onPushPermissionIntent,
                        onBack = { slotsRoute = SlotsRoute.Details(route.slot.id) },
                        onDone = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                            slotsRoute = SlotsRoute.List
                        },
                        onOpenBookings = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .clickable { onIntent(SlotListIntent.CloseFilters) },
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
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text("Фильтры", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
                    Text(
                        text = "Сбросить",
                        modifier = Modifier.clickable { onIntent(SlotListIntent.ResetFilters) },
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "×",
                        modifier = Modifier
                            .size(VolnaTheme.tokens.spacing.lg)
                            .clickable { onIntent(SlotListIntent.CloseFilters) },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            FilterGroup(title = "Дата старта") {
                FilterChipRow {
                    FilterChipButton("Любая", state.draftDatePreset == SlotDatePreset.Any) {
                        onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Any))
                    }
                    FilterChipButton("Сегодня", state.draftDatePreset == SlotDatePreset.Today) {
                        onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.Today))
                    }
                    FilterChipButton("7 дней", state.draftDatePreset == SlotDatePreset.NextSevenDays) {
                        onIntent(SlotListIntent.SelectDatePreset(SlotDatePreset.NextSevenDays))
                    }
                }
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

            FilterChipButton(
                label = if (state.draftFilters.onlyAvailable) "✓ Только со свободными местами" else "Только со свободными местами",
                selected = state.draftFilters.onlyAvailable,
                onClick = { onIntent(SlotListIntent.ToggleOnlyAvailable) },
            )

            InstructorFilterSection(
                instructors = state.instructors,
                selected = state.draftFilters.instructorIds,
                onToggle = { onIntent(SlotListIntent.ToggleInstructor(it.id)) },
                onRetry = { onIntent(SlotListIntent.RetryInstructors) },
            )

            Button(
                onClick = { onIntent(SlotListIntent.ApplyFilters) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolnaTheme.tokens.sizing.buttonHeight),
            ) {
                Text("Применить")
            }
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
        Text(title, fontWeight = FontWeight.Bold)
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
    val text = if (selected && !label.startsWith("✓")) "✓ $label" else label
    OutlinedButton(onClick = onClick) {
        Text(text)
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
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        slots.take(3).forEach { slot ->
            SlotCard(slot, onSlotClick)
        }
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
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
            Text(slot.route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Инструктор: ${slot.instructor.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (canOpen) "Свободно мест: ${slot.freeSeats}" else "Мест нет",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("${slot.price.value} ₽", fontWeight = FontWeight.Bold)
    }
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
        BackButton(onBack)
        ScreenTitle("Прогулка")
        when (val slot = state.slot) {
            Loadable.Initial,
            Loadable.Loading -> {
                SkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
                SkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
            }
            is Loadable.Content -> SlotDetailsContent(
                slot = slot.value,
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
    onBook: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val availability = AvailabilityPolicy.availability(slot)
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                )
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        ) {
            Text(slot.route.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(slot.startAt.toUiText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Маршрут: ${slot.route.type.toUiText()}, ${slot.route.durationMin} мин")
            Text("Инструктор: ${slot.instructor.name}")
            Text("Место встречи: ${slot.meetingPoint.title.ifBlank { "уточняется" }}")
        }
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
            Text("Карта маршрута", fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier.clickable { onOpenMap() },
            ) {
                RouteMapPreview(
                    route = slot.route,
                    meetingPoint = slot.meetingPoint,
                    state = slot.route.toMapUiState(),
                    onRetry = {},
                    onOpenExternal = onOpenMap,
                )
            }
        }
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
            Text("Доступность", fontWeight = FontWeight.Bold)
            Text("Можно забронировать мест: ${availability.maxSeatsForBooking}")
            Text("Свободно прокатных досок: ${availability.freeRentalBoards}")
            Text("Место: ${slot.price.value} ₽")
            Text("Прокат доски: ${slot.rentalPrice.value} ₽")
        }
        Button(
            onClick = onBook,
            enabled = availability.canBook,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolnaTheme.tokens.sizing.buttonHeight),
        ) {
            Text(if (availability.canBook) "Записаться" else "Мест нет")
        }
        Spacer(Modifier.height(VolnaTheme.tokens.sizing.navHeight))
    }
}

@Composable
private fun BookingFormScreen(
    slot: Slot,
    state: BookingFormState,
    appConfig: AppConfig,
    onIntent: (BookingFormIntent) -> Unit,
    pushPermissionState: PushPermissionState,
    onPushPermissionIntent: (PushPermissionIntent) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenBookings: () -> Unit,
) {
    LaunchedEffect(slot.id) {
        onIntent(BookingFormIntent.Open(slot))
    }
    Box(Modifier.fillMaxSize()) {
        BackButton(onBack)
        ScreenTitle("Запись")
        BookingFormContent(
            state = state,
            onIntent = onIntent,
        )
        state.createdBooking?.let { booking ->
            LaunchedEffect(booking.id) {
                if (booking.isFirstBooking == true) {
                    onPushPermissionIntent(PushPermissionIntent.BookingSuccessShown)
                }
            }
            BookingSuccessSheet(
                booking = booking,
                fallbackPrice = state.totalPrice?.value ?: 0,
                reminderHours = appConfig.reminderHours,
                pushPermissionState = pushPermissionState,
                onPushPermissionIntent = onPushPermissionIntent,
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
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        state.slot?.let { slot ->
            Text(slot.route.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(slot.startAt.toUiText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CounterRow(
            title = "Места",
            value = state.seatsCount,
            onMinus = { onIntent(BookingFormIntent.DecrementSeats) },
            onPlus = { onIntent(BookingFormIntent.IncrementSeats) },
        )
        CounterRow(
            title = "Прокатные доски",
            value = state.rentalCount,
            onMinus = { onIntent(BookingFormIntent.DecrementRental) },
            onPlus = { onIntent(BookingFormIntent.IncrementRental) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                )
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
        ) {
            Text("Итого", fontWeight = FontWeight.Bold)
            Text("${state.totalPrice?.value ?: 0} ₽", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Оплата на месте: наличные или перевод на карту.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.validationMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = { onIntent(BookingFormIntent.Submit) },
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolnaTheme.tokens.sizing.buttonHeight),
        ) {
            Text(if (state.isSubmitting) "Записываем..." else "Записаться")
        }
        Spacer(Modifier.height(VolnaTheme.tokens.sizing.navHeight))
    }
}

@Composable
private fun BookingSuccessSheet(
    booking: Booking,
    fallbackPrice: Int,
    reminderHours: Int?,
    pushPermissionState: PushPermissionState,
    onPushPermissionIntent: (PushPermissionIntent) -> Unit,
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
            if (pushPermissionState.showPrompt) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
                        )
                        .padding(VolnaTheme.tokens.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
                ) {
                    Text(
                        text = reminderHours?.let { "Напомним за $it часов до старта" } ?: "Напомним перед стартом",
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Включите уведомления, чтобы не пропустить прогулку")
                    Row(horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
                        Button(onClick = { onPushPermissionIntent(PushPermissionIntent.RequestPermission) }) {
                            Text("Включить")
                        }
                        OutlinedButton(onClick = { onPushPermissionIntent(PushPermissionIntent.DismissPrompt) }) {
                            Text("Не сейчас")
                        }
                    }
                }
            }
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

private fun kotlinx.datetime.Instant.toUiText(): String =
    toString()
        .replace("T", " ")
        .removeSuffix("Z")
