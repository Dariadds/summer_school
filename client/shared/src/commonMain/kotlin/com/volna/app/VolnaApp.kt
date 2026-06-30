package com.volna.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.volna.app.auth.SessionRepository
import com.volna.app.auth.presentation.AuthEffect
import com.volna.app.auth.presentation.AuthIntent
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthStore
import com.volna.app.booking.presentation.*
import com.volna.app.catalog.presentation.*
import com.volna.app.core.config.AppConfig
import com.volna.app.core.navigation.BindBrowserNavigation
import com.volna.app.core.navigation.BindSystemBack
import com.volna.app.core.navigation.currentBrowserPath
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.time.AppClock
import com.volna.app.profile.presentation.ProfileEffect
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileMode
import com.volna.app.profile.presentation.ProfileStore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VolnaApp() {
    VolnaTheme {
        val initialBrowserPath = remember { currentBrowserPath() ?: ROUTE_AUTH }
        val appScope = rememberCoroutineScope()
        val appConfig = koinInject<AppConfig>()
        val clock = koinInject<AppClock>()
        val sessionRepository = koinInject<SessionRepository>()
        val authStore = koinViewModel<AuthStore>()
        val profileStore = koinViewModel<ProfileStore>()
        val slotListStore = koinViewModel<SlotListStore>()
        val slotDetailsStore = koinViewModel<SlotDetailsStore>()
        val bookingFormStore = koinViewModel<BookingFormStore>()
        val bookingListStore = koinViewModel<BookingListStore>()
        val bookingDetailsStore = koinViewModel<BookingDetailsStore>()
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

        fun canHandleSystemBack(): Boolean = when (rootState) {
            RootState.CheckingSession -> false

            RootState.Auth -> authState.step != com.volna.app.auth.presentation.AuthStep.Phone

            RootState.Main -> when {
                slotListState.filtersVisible -> true
                slotDetailsState.showRouteMap -> true
                bookingFormState.createdBooking != null -> true
                bookingDetailsState.showCancelConfirm -> true
                bookingDetailsState.showRouteMap -> true
                profileState.logoutConfirmVisible -> true
                profileState.deleteConfirmVisible -> true
                selectedTab == MainTab.Slots && slotsRoute is SlotsRoute.Booking -> true
                selectedTab == MainTab.Slots && slotsRoute is SlotsRoute.Details -> true
                selectedTab == MainTab.Bookings && bookingsRoute is BookingsRoute.Details -> true
                selectedTab == MainTab.Profile && profileState.mode == ProfileMode.ConfirmPhone -> true
                selectedTab == MainTab.Profile && profileState.mode == ProfileMode.Edit -> true
                selectedTab != MainTab.Slots -> true
                else -> false
            }
        }

        fun handleSystemBack() = when (rootState) {
            RootState.CheckingSession -> false

            RootState.Auth -> when (authState.step) {
                com.volna.app.auth.presentation.AuthStep.Phone -> false
                com.volna.app.auth.presentation.AuthStep.Otp,
                com.volna.app.auth.presentation.AuthStep.Name,
                    -> {
                    authStore.accept(AuthIntent.BackToPhone)
                    true
                }
            }

            RootState.Main -> when {
                slotListState.filtersVisible -> {
                    slotListStore.accept(SlotListIntent.CloseFilters)
                    true
                }

                slotDetailsState.showRouteMap -> {
                    slotDetailsStore.accept(SlotDetailsIntent.DismissRouteMap)
                    true
                }

                bookingFormState.createdBooking != null -> {
                    bookingFormStore.accept(BookingFormIntent.SuccessDismissed)
                    true
                }

                bookingDetailsState.showCancelConfirm -> {
                    bookingDetailsStore.accept(BookingDetailsIntent.DismissCancel)
                    true
                }

                bookingDetailsState.showRouteMap -> {
                    bookingDetailsStore.accept(BookingDetailsIntent.DismissRouteMap)
                    true
                }

                profileState.logoutConfirmVisible -> {
                    profileStore.accept(ProfileIntent.LogoutDismissed)
                    true
                }

                profileState.deleteConfirmVisible -> {
                    profileStore.accept(ProfileIntent.DeleteDismissed)
                    true
                }

                selectedTab == MainTab.Slots && slotsRoute is SlotsRoute.Booking -> {
                    val slot = (slotsRoute as SlotsRoute.Booking).slot
                    slotsRoute = SlotsRoute.Details(slot.id)
                    true
                }

                selectedTab == MainTab.Slots && slotsRoute is SlotsRoute.Details -> {
                    slotsRoute = SlotsRoute.List
                    true
                }

                selectedTab == MainTab.Bookings && bookingsRoute is BookingsRoute.Details -> {
                    bookingsRoute = BookingsRoute.List
                    true
                }

                selectedTab == MainTab.Profile && profileState.mode == ProfileMode.ConfirmPhone -> {
                    profileStore.accept(ProfileIntent.BackToEdit)
                    true
                }

                selectedTab == MainTab.Profile && profileState.mode == ProfileMode.Edit -> {
                    profileStore.accept(ProfileIntent.EditCancelled)
                    true
                }

                selectedTab != MainTab.Slots -> {
                    selectedTab = MainTab.Slots
                    slotsRoute = SlotsRoute.List
                    true
                }

                else -> false
            }
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

        BindSystemBack(enabled = canHandleSystemBack()) {
            handleSystemBack()
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
