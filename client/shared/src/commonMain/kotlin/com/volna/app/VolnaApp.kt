package com.volna.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.volna.app.auth.data.DefaultSessionRepository
import com.volna.app.auth.data.KtorAuthRepository
import com.volna.app.auth.presentation.AuthEffect
import com.volna.app.auth.presentation.AuthIntent
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthStore
import com.volna.app.booking.data.KtorBookingRepository
import com.volna.app.booking.data.RandomIdempotencyKeyFactory
import com.volna.app.booking.presentation.*
import com.volna.app.catalog.data.KtorInstructorRepository
import com.volna.app.catalog.data.KtorSlotRepository
import com.volna.app.catalog.presentation.*
import com.volna.app.core.config.AppConfig
import com.volna.app.core.navigation.BindBrowserNavigation
import com.volna.app.core.navigation.currentBrowserPath
import com.volna.app.core.network.VolnaApiClient
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.time.AppClock
import com.volna.app.core.time.SystemAppClock
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.profile.data.KtorProfileRepository
import com.volna.app.profile.presentation.*
import com.volna.app.uikit.icons.*
import kotlinx.coroutines.launch

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
        val initialBrowserPath = remember { currentBrowserPath() ?: ROUTE_AUTH }
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
        var selectedTab by remember { mutableStateOf(MainTab.Slots) }
        var slotsRoute by remember { mutableStateOf<SlotsRoute>(SlotsRoute.List) }
        var bookingsRoute by remember { mutableStateOf<BookingsRoute>(BookingsRoute.List) }

        fun applyBrowserPath(path: String, hasSession: Boolean) {
            val route = parseBrowserRoute(path)
            if (!hasSession) {
                rootState = RootState.Auth
                return
            }

            rootState = RootState.Main
            when (route) {
                BrowserRoute.Auth,
                BrowserRoute.SlotsList,
                BrowserRoute.Unknown,
                    -> {
                    selectedTab = MainTab.Slots
                    slotsRoute = SlotsRoute.List
                }

                is BrowserRoute.SlotDetails -> {
                    selectedTab = MainTab.Slots
                    slotsRoute = SlotsRoute.Details(route.slotId)
                }

                BrowserRoute.BookingsList -> {
                    selectedTab = MainTab.Bookings
                    bookingsRoute = BookingsRoute.List
                }

                is BrowserRoute.BookingDetails -> {
                    selectedTab = MainTab.Bookings
                    bookingsRoute = BookingsRoute.Details(route.bookingId)
                }

                BrowserRoute.Profile -> {
                    selectedTab = MainTab.Profile
                }
            }
        }

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
            selectedTab = MainTab.Slots
            slotsRoute = SlotsRoute.List
            bookingsRoute = BookingsRoute.List
            rootState = RootState.Auth
        }

        val browserPath = remember(rootState, selectedTab, slotsRoute, bookingsRoute, initialBrowserPath) {
            if (rootState == RootState.CheckingSession) {
                initialBrowserPath
            } else {
                browserPathFor(rootState, selectedTab, slotsRoute, bookingsRoute)
            }
        }

        if (rootState != RootState.CheckingSession) {
            BindBrowserNavigation(currentPath = browserPath) { path ->
                applyBrowserPath(path, hasSession = rootState == RootState.Main)
            }
        }

        LaunchedEffect(sessionRepository) {
            if (sessionRepository.token().isNullOrBlank()) {
                selectedTab = MainTab.Slots
                slotsRoute = SlotsRoute.List
                bookingsRoute = BookingsRoute.List
                rootState = RootState.Auth
            } else {
                applyBrowserPath(initialBrowserPath, hasSession = true)
            }
        }

        LaunchedEffect(authStore) {
            while (true) {
                when (authStore.effects()) {
                    AuthEffect.Authenticated -> {
                        applyBrowserPath(currentBrowserPath() ?: ROUTE_SLOTS, hasSession = true)
                    }
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
                selectedTab = selectedTab,
                onSelectedTabChange = { selectedTab = it },
                slotsRoute = slotsRoute,
                onSlotsRouteChange = { slotsRoute = it },
                bookingsRoute = bookingsRoute,
                onBookingsRouteChange = { bookingsRoute = it },
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
    selectedTab: MainTab,
    onSelectedTabChange: (MainTab) -> Unit,
    slotsRoute: SlotsRoute,
    onSlotsRouteChange: (SlotsRoute) -> Unit,
    bookingsRoute: BookingsRoute,
    onBookingsRouteChange: (BookingsRoute) -> Unit,
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
                            onSlotsRouteChange(SlotsRoute.Details(slot.id))
                        },
                    )
                    is SlotsRoute.Details -> SlotDetailsScreen(
                        slotId = route.slotId,
                        state = slotDetailsState,
                        onIntent = onSlotDetailsIntent,
                        onBack = { onSlotsRouteChange(SlotsRoute.List) },
                        onBook = { slot -> onSlotsRouteChange(SlotsRoute.Booking(slot)) },
                    )
                    is SlotsRoute.Booking -> BookingFormScreen(
                        slot = route.slot,
                        state = bookingFormState,
                        onIntent = onBookingFormIntent,
                        onBack = { onSlotsRouteChange(SlotsRoute.Details(route.slot.id)) },
                        onDone = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                            onSlotListIntent(SlotListIntent.Retry)
                            onSlotsRouteChange(SlotsRoute.List)
                        },
                        onOpenBookings = {
                            onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                            onSlotListIntent(SlotListIntent.Retry)
                            onSlotsRouteChange(SlotsRoute.List)
                            onBookingsRouteChange(BookingsRoute.List)
                            onBookingListIntent(BookingListIntent.Refresh)
                            onSelectedTabChange(MainTab.Bookings)
                        },
                    )
                }
                MainTab.Bookings -> when (val route = bookingsRoute) {
                    BookingsRoute.List -> BookingListScreen(
                        state = bookingListState,
                        onIntent = onBookingListIntent,
                        onBookingClick = { bookingId -> onBookingsRouteChange(BookingsRoute.Details(bookingId)) },
                        onBookWalk = {
                            onSlotsRouteChange(SlotsRoute.List)
                            onSelectedTabChange(MainTab.Slots)
                        },
                    )
                    is BookingsRoute.Details -> BookingDetailsScreen(
                        bookingId = route.bookingId,
                        state = bookingDetailsState,
                        clock = clock,
                        onIntent = onBookingDetailsIntent,
                        onBack = { onBookingsRouteChange(BookingsRoute.List) },
                    )
                }
                MainTab.Profile -> ProfileScreen(
                    state = profileState,
                    appConfig = appConfig,
                    onIntent = onProfileIntent,
                )
            }
            val navBarVisible = when {
                slotsRoute is SlotsRoute.Details -> false
                slotsRoute is SlotsRoute.Booking -> false
                bookingsRoute is BookingsRoute.Details -> false
                slotListState.filtersVisible -> false
                slotDetailsState.showRouteMap -> false
                bookingFormState.createdBooking != null -> false
                bookingDetailsState.showCancelConfirm -> false
                bookingDetailsState.showRouteMap -> false
                profileState.logoutConfirmVisible -> false
                profileState.deleteConfirmVisible -> false
                else -> true
            }
            if (navBarVisible) {
                FloatingNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        if (it == MainTab.Slots) {
                            onSlotsRouteChange(SlotsRoute.List)
                        }
                        if (it == MainTab.Bookings) {
                            onBookingsRouteChange(BookingsRoute.List)
                        }
                        onSelectedTabChange(it)
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                )
            }
        }
    }
}

private sealed interface BrowserRoute {
    data object Auth : BrowserRoute
    data object SlotsList : BrowserRoute
    data class SlotDetails(val slotId: SlotId) : BrowserRoute
    data object BookingsList : BrowserRoute
    data class BookingDetails(val bookingId: BookingId) : BrowserRoute
    data object Profile : BrowserRoute
    data object Unknown : BrowserRoute
}

private fun parseBrowserRoute(path: String): BrowserRoute {
    val normalized = path
        .substringBefore('?')
        .substringBefore('#')
        .trim()
        .ifBlank { ROUTE_AUTH }
    val segments = normalized.trim('/').split('/').filter { it.isNotBlank() }

    return when {
        normalized == ROUTE_AUTH -> BrowserRoute.Auth
        normalized == ROUTE_SLOTS -> BrowserRoute.SlotsList
        segments.size == 2 && segments[0] == "slots" -> BrowserRoute.SlotDetails(SlotId(segments[1]))
        normalized == ROUTE_BOOKINGS -> BrowserRoute.BookingsList
        segments.size == 2 && segments[0] == "bookings" -> BrowserRoute.BookingDetails(BookingId(segments[1]))
        normalized == ROUTE_PROFILE -> BrowserRoute.Profile
        else -> BrowserRoute.Unknown
    }
}

private fun browserPathFor(
    rootState: RootState,
    selectedTab: MainTab,
    slotsRoute: SlotsRoute,
    bookingsRoute: BookingsRoute,
): String = when (rootState) {
    RootState.CheckingSession,
    RootState.Auth,
        -> ROUTE_AUTH

    RootState.Main -> when (selectedTab) {
        MainTab.Slots -> when (slotsRoute) {
            SlotsRoute.List -> ROUTE_SLOTS
            is SlotsRoute.Details -> "$ROUTE_SLOTS/${slotsRoute.slotId.value}"
            is SlotsRoute.Booking -> "$ROUTE_SLOTS/${slotsRoute.slot.id.value}"
        }

        MainTab.Bookings -> when (bookingsRoute) {
            BookingsRoute.List -> ROUTE_BOOKINGS
            is BookingsRoute.Details -> "$ROUTE_BOOKINGS/${bookingsRoute.bookingId.value}"
        }

        MainTab.Profile -> ROUTE_PROFILE
    }
}

private const val ROUTE_AUTH = "/auth"
private const val ROUTE_SLOTS = "/slots"
private const val ROUTE_BOOKINGS = "/bookings"
private const val ROUTE_PROFILE = "/profile"

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
            icon = Icons.Calendar,
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Bookings,
            selected = selectedTab == MainTab.Bookings,
            icon = Icons.Options,
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Profile,
            selected = selectedTab == MainTab.Profile,
            icon = Icons.Profile,
            onClick = onTabSelected,
        )
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    selected: Boolean,
    icon: ImageVector,
    onClick: (MainTab) -> Unit,
) {
    VolnaIcon(
        imageVector = icon,
        contentDescription = tab.title,
        modifier = Modifier
            .clickable { onClick(tab) },
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        size = VolnaTheme.tokens.spacing.xl,
    )
}
