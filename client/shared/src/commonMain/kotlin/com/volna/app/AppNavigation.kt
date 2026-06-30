package com.volna.app

import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId

internal enum class MainTab(val title: String) {
    Slots("Прогулки"),
    Bookings("Мои записи"),
    Profile("Профиль"),
}

internal enum class RootState {
    CheckingSession,
    Auth,
    Main,
}

internal sealed interface SlotsRoute {
    data object List : SlotsRoute
    data class Details(val slotId: SlotId) : SlotsRoute
    data class Booking(val slot: Slot) : SlotsRoute
}

internal sealed interface BookingsRoute {
    data object List : BookingsRoute
    data class Details(val bookingId: BookingId) : BookingsRoute
}

internal sealed interface BrowserRoute {
    data object Auth : BrowserRoute
    data object SlotsList : BrowserRoute
    data class SlotDetails(val slotId: SlotId) : BrowserRoute
    data object BookingsList : BrowserRoute
    data class BookingDetails(val bookingId: BookingId) : BrowserRoute
    data object Profile : BrowserRoute
    data object Unknown : BrowserRoute
}

internal fun parseBrowserRoute(path: String): BrowserRoute {
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

internal fun browserPathFor(
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

internal const val ROUTE_AUTH = "/auth"
internal const val ROUTE_SLOTS = "/slots"
internal const val ROUTE_BOOKINGS = "/bookings"
internal const val ROUTE_PROFILE = "/profile"
