package com.volna.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import com.volna.app.booking.presentation.BookingDetailsIntent
import com.volna.app.booking.presentation.BookingDetailsScreen
import com.volna.app.booking.presentation.BookingDetailsState
import com.volna.app.booking.presentation.BookingFormIntent
import com.volna.app.booking.presentation.BookingFormScreen
import com.volna.app.booking.presentation.BookingFormState
import com.volna.app.booking.presentation.BookingListIntent
import com.volna.app.booking.presentation.BookingListScreen
import com.volna.app.booking.presentation.BookingListState
import com.volna.app.catalog.presentation.SlotDetailsIntent
import com.volna.app.catalog.presentation.SlotDetailsScreen
import com.volna.app.catalog.presentation.SlotDetailsState
import com.volna.app.catalog.presentation.SlotListIntent
import com.volna.app.catalog.presentation.SlotListScreen
import com.volna.app.catalog.presentation.SlotListState
import com.volna.app.core.config.AppConfig
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.time.AppClock
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileScreen
import com.volna.app.profile.presentation.ProfileState
import com.volna.app.uikit.icons.Calendar
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Options
import com.volna.app.uikit.icons.Profile
import com.volna.app.uikit.icons.VolnaIcon

@Composable
internal fun MainTabs(
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = VolnaTheme.tokens.sizing.screenMaxWidth),
        ) {
            when (selectedTab) {
                MainTab.Slots -> SlotsTabContent(
                    route = slotsRoute,
                    onRouteChange = onSlotsRouteChange,
                    slotListState = slotListState,
                    onSlotListIntent = onSlotListIntent,
                    slotDetailsState = slotDetailsState,
                    onSlotDetailsIntent = onSlotDetailsIntent,
                    bookingFormState = bookingFormState,
                    onBookingFormIntent = onBookingFormIntent,
                    onBookingsRouteChange = onBookingsRouteChange,
                    onBookingListIntent = onBookingListIntent,
                    onSelectedTabChange = onSelectedTabChange,
                )

                MainTab.Bookings -> BookingsTabContent(
                    route = bookingsRoute,
                    onRouteChange = onBookingsRouteChange,
                    bookingListState = bookingListState,
                    onBookingListIntent = onBookingListIntent,
                    bookingDetailsState = bookingDetailsState,
                    onBookingDetailsIntent = onBookingDetailsIntent,
                    clock = clock,
                    onSlotsRouteChange = onSlotsRouteChange,
                    onSelectedTabChange = onSelectedTabChange,
                )

                MainTab.Profile -> ProfileScreen(
                    state = profileState,
                    appConfig = appConfig,
                    onIntent = onProfileIntent,
                )
            }

            if (isNavBarVisible(
                    slotsRoute = slotsRoute,
                    bookingsRoute = bookingsRoute,
                    slotListState = slotListState,
                    slotDetailsState = slotDetailsState,
                    bookingFormState = bookingFormState,
                    bookingDetailsState = bookingDetailsState,
                    profileState = profileState,
                )
            ) {
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
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SlotsTabContent(
    route: SlotsRoute,
    onRouteChange: (SlotsRoute) -> Unit,
    slotListState: SlotListState,
    onSlotListIntent: (SlotListIntent) -> Unit,
    slotDetailsState: SlotDetailsState,
    onSlotDetailsIntent: (SlotDetailsIntent) -> Unit,
    bookingFormState: BookingFormState,
    onBookingFormIntent: (BookingFormIntent) -> Unit,
    onBookingsRouteChange: (BookingsRoute) -> Unit,
    onBookingListIntent: (BookingListIntent) -> Unit,
    onSelectedTabChange: (MainTab) -> Unit,
) {
    when (route) {
        SlotsRoute.List -> SlotListScreen(
            state = slotListState,
            onIntent = onSlotListIntent,
            onSlotClick = { slot ->
                onRouteChange(SlotsRoute.Details(slot.id))
            },
        )

        is SlotsRoute.Details -> SlotDetailsScreen(
            slotId = route.slotId,
            state = slotDetailsState,
            onIntent = onSlotDetailsIntent,
            onBack = { onRouteChange(SlotsRoute.List) },
            onBook = { slot -> onRouteChange(SlotsRoute.Booking(slot)) },
        )

        is SlotsRoute.Booking -> BookingFormScreen(
            slot = route.slot,
            state = bookingFormState,
            onIntent = onBookingFormIntent,
            onBack = { onRouteChange(SlotsRoute.Details(route.slot.id)) },
            onDone = {
                onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                onSlotListIntent(SlotListIntent.Retry)
                onRouteChange(SlotsRoute.List)
            },
            onOpenBookings = {
                onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                onSlotListIntent(SlotListIntent.Retry)
                onRouteChange(SlotsRoute.List)
                onBookingsRouteChange(BookingsRoute.List)
                onBookingListIntent(BookingListIntent.Refresh)
                onSelectedTabChange(MainTab.Bookings)
            },
        )
    }
}

@Composable
private fun BookingsTabContent(
    route: BookingsRoute,
    onRouteChange: (BookingsRoute) -> Unit,
    bookingListState: BookingListState,
    onBookingListIntent: (BookingListIntent) -> Unit,
    bookingDetailsState: BookingDetailsState,
    onBookingDetailsIntent: (BookingDetailsIntent) -> Unit,
    clock: AppClock,
    onSlotsRouteChange: (SlotsRoute) -> Unit,
    onSelectedTabChange: (MainTab) -> Unit,
) {
    when (route) {
        BookingsRoute.List -> BookingListScreen(
            state = bookingListState,
            onIntent = onBookingListIntent,
            onBookingClick = { bookingId -> onRouteChange(BookingsRoute.Details(bookingId)) },
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
            onBack = { onRouteChange(BookingsRoute.List) },
        )
    }
}

private fun isNavBarVisible(
    slotsRoute: SlotsRoute,
    bookingsRoute: BookingsRoute,
    slotListState: SlotListState,
    slotDetailsState: SlotDetailsState,
    bookingFormState: BookingFormState,
    bookingDetailsState: BookingDetailsState,
    profileState: ProfileState,
): Boolean = when {
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
        verticalAlignment = Alignment.CenterVertically,
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
        modifier = Modifier.clickable { onClick(tab) },
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        size = VolnaTheme.tokens.spacing.xl,
    )
}
