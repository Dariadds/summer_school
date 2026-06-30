package com.volna.app

import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.SlotId

internal enum class MainTab(val title: String) {
    Slots("Прогулки"),
    Bookings("Мои записи"),
    Profile("Профиль"),
}

internal enum class RootState {
    CheckingSession,
    Ready,
}

internal const val AUTH_ROUTE = "auth"
internal const val SLOTS_ROUTE = "slots"
internal const val SLOT_DETAILS_ROUTE = "slot/{slotId}"
internal const val SLOT_BOOKING_ROUTE = "slot/{slotId}/booking"
internal const val BOOKINGS_ROUTE = "bookings"
internal const val BOOKING_DETAILS_ROUTE = "booking/{bookingId}"
internal const val PROFILE_ROUTE = "profile"

internal fun slotDetailsRoute(slotId: SlotId): String = "slot/${slotId.value}"

internal fun slotBookingRoute(slotId: SlotId): String = "slot/${slotId.value}/booking"

internal fun bookingDetailsRoute(bookingId: BookingId): String = "booking/${bookingId.value}"

internal fun String?.asSlotId(): SlotId = SlotId(orEmpty())

internal fun String?.asBookingId(): BookingId = BookingId(orEmpty())

internal fun MainTab.destinationRoute(): String = when (this) {
    MainTab.Slots -> SLOTS_ROUTE
    MainTab.Bookings -> BOOKINGS_ROUTE
    MainTab.Profile -> PROFILE_ROUTE
}

internal fun String?.mainTab(): MainTab = when {
    this == BOOKINGS_ROUTE -> MainTab.Bookings
    this == BOOKING_DETAILS_ROUTE -> MainTab.Bookings
    this == PROFILE_ROUTE -> MainTab.Profile
    else -> MainTab.Slots
}
