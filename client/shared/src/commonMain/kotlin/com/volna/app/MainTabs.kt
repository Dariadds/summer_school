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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
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
import com.volna.app.core.ui.Loadable
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileScreen
import com.volna.app.profile.presentation.ProfileState
import com.volna.app.uikit.icons.Calendar
import com.volna.app.uikit.icons.Icons
import com.volna.app.uikit.icons.Options
import com.volna.app.uikit.icons.Profile
import com.volna.app.uikit.icons.VolnaIcon
import com.volna.app.auth.presentation.AuthIntent
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthState

@Composable
internal fun MainTabs(
    navController: NavHostController,
    authState: AuthState,
    onAuthIntent: (AuthIntent) -> Unit,
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = currentRoute.mainTab()

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
            NavHost(
                navController = navController,
                startDestination = AUTH_ROUTE,
            ) {
                composable(AUTH_ROUTE) {
                    AuthScreen(
                        state = authState,
                        onIntent = onAuthIntent,
                    )
                }

                composable(SLOTS_ROUTE) {
                    SlotListScreen(
                        state = slotListState,
                        onIntent = onSlotListIntent,
                        onSlotClick = { slot ->
                            navController.navigate(slotDetailsRoute(slot.id))
                        },
                    )
                }

                composable(
                    route = SLOT_DETAILS_ROUTE,
                    arguments = listOf(navArgument("slotId") { type = NavType.StringType }),
                ) { entry ->
                    val slotId = entry.arguments?.getString("slotId").asSlotId()
                    SlotDetailsScreen(
                        slotId = slotId,
                        state = slotDetailsState,
                        onIntent = onSlotDetailsIntent,
                        onBack = { navController.popBackStack() },
                        onBook = { slot -> navController.navigate(slotBookingRoute(slot.id)) },
                    )
                }

                composable(
                    route = SLOT_BOOKING_ROUTE,
                    arguments = listOf(navArgument("slotId") { type = NavType.StringType }),
                ) { entry ->
                    val slotId = entry.arguments?.getString("slotId").asSlotId()
                    val loadedSlot = (slotDetailsState.slot as? Loadable.Content)?.value
                        ?.takeIf { it.id == slotId }
                    if (loadedSlot == null) {
                        SlotDetailsScreen(
                            slotId = slotId,
                            state = slotDetailsState,
                            onIntent = onSlotDetailsIntent,
                            onBack = { navController.popBackStack() },
                            onBook = { slot -> navController.navigate(slotBookingRoute(slot.id)) },
                        )
                    } else {
                        BookingFormScreen(
                            slot = loadedSlot,
                            state = bookingFormState,
                            onIntent = onBookingFormIntent,
                            onBack = { navController.popBackStack() },
                            onDone = {
                                onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                                onSlotListIntent(SlotListIntent.Retry)
                                navController.navigate(SLOTS_ROUTE) {
                                    popUpTo(SLOTS_ROUTE) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onOpenBookings = {
                                onBookingFormIntent(BookingFormIntent.SuccessDismissed)
                                onSlotListIntent(SlotListIntent.Retry)
                                onBookingListIntent(BookingListIntent.Refresh)
                                navController.navigate(BOOKINGS_ROUTE) {
                                    popUpTo(SLOTS_ROUTE) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }

                composable(BOOKINGS_ROUTE) {
                    BookingListScreen(
                        state = bookingListState,
                        onIntent = onBookingListIntent,
                        onBookingClick = { bookingId ->
                            navController.navigate(bookingDetailsRoute(bookingId))
                        },
                        onBookWalk = {
                            navController.navigate(SLOTS_ROUTE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }

                composable(
                    route = BOOKING_DETAILS_ROUTE,
                    arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
                ) { entry ->
                    val bookingId = entry.arguments?.getString("bookingId").asBookingId()
                    BookingDetailsScreen(
                        bookingId = bookingId,
                        state = bookingDetailsState,
                        clock = clock,
                        onIntent = onBookingDetailsIntent,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(PROFILE_ROUTE) {
                    ProfileScreen(
                        state = profileState,
                        appConfig = appConfig,
                        onIntent = onProfileIntent,
                    )
                }
            }

            if (isNavBarVisible(
                    currentRoute = currentRoute,
                    slotListState = slotListState,
                    slotDetailsState = slotDetailsState,
                    bookingFormState = bookingFormState,
                    bookingDetailsState = bookingDetailsState,
                    profileState = profileState,
                )
            ) {
                FloatingNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        navController.navigate(tab.destinationRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun isNavBarVisible(
    currentRoute: String?,
    slotListState: SlotListState,
    slotDetailsState: SlotDetailsState,
    bookingFormState: BookingFormState,
    bookingDetailsState: BookingDetailsState,
    profileState: ProfileState,
): Boolean = when {
    currentRoute == AUTH_ROUTE -> false
    currentRoute == SLOT_DETAILS_ROUTE -> false
    currentRoute == SLOT_BOOKING_ROUTE -> false
    currentRoute == BOOKING_DETAILS_ROUTE -> false
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
