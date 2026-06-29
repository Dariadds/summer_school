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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.volna.app.booking.data.KtorBookingRepository
import com.volna.app.booking.data.RandomIdempotencyKeyFactory
import com.volna.app.booking.presentation.BookingFormEffect
import com.volna.app.booking.presentation.BookingFormIntent
import com.volna.app.booking.presentation.BookingFormState
import com.volna.app.booking.presentation.BookingFormScreen
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
import com.volna.app.catalog.presentation.SlotDetailsScreen
import com.volna.app.catalog.presentation.SlotListScreen
import com.volna.app.core.config.AppConfig
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.network.VolnaApiClient
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.core.time.AppClock
import com.volna.app.core.time.SystemAppClock
import com.volna.app.core.ui.Loadable
import com.volna.app.uikit.icons.Back
import com.volna.app.uikit.icons.Calendar
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Info
import com.volna.app.uikit.icons.Options
import com.volna.app.uikit.icons.Profile
import com.volna.app.uikit.icons.Share
import com.volna.app.uikit.icons.Tune
import com.volna.app.uikit.icons.VolnaIcon
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

