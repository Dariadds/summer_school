package com.volna.app.booking.presentation

import com.volna.app.booking.BookingRepository
import com.volna.app.catalog.PageRequest
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.time.AppClock
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingGroups(
    val upcoming: List<Booking>,
    val past: List<Booking>,
)

data class BookingListState(
    val bookings: Loadable<BookingGroups> = Loadable.Initial,
)

sealed interface BookingListIntent {
    data object Load : BookingListIntent
    data object Refresh : BookingListIntent
    data object Retry : BookingListIntent
    data object Reset : BookingListIntent
}

sealed interface BookingListEffect {
    data object SignedOut : BookingListEffect
}

// CMP-12 / SCR-005: listBookings with client-side upcoming/past grouping.
class BookingListStore(
    private val bookingRepository: BookingRepository,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) : MviStore<BookingListState, BookingListIntent, BookingListEffect> {
    private val mutableState = MutableStateFlow(BookingListState())
    private val effects = Channel<BookingListEffect>(Channel.BUFFERED)

    override val state: StateFlow<BookingListState> = mutableState

    override fun accept(intent: BookingListIntent) {
        when (intent) {
            BookingListIntent.Load -> load(force = false)
            BookingListIntent.Refresh -> load(force = true)
            BookingListIntent.Retry -> load(force = true)
            BookingListIntent.Reset -> mutableState.value = BookingListState()
        }
    }

    override suspend fun effects(): BookingListEffect = effects.receive()

    private fun load(force: Boolean) {
        val current = mutableState.value.bookings
        if (!force && (current == Loadable.Loading || current is Loadable.Content)) return

        scope.launch {
            mutableState.update {
                it.copy(
                    bookings = if (force && current is Loadable.Content) {
                        current.copy(refreshing = true)
                    } else {
                        Loadable.Loading
                    },
                )
            }
            bookingRepository.listBookings(page = PageRequest(limit = 100)).fold(
                onSuccess = { page ->
                    val groups = page.items.toGroups(clock)
                    mutableState.update {
                        it.copy(
                            bookings = if (groups.upcoming.isEmpty() && groups.past.isEmpty()) {
                                Loadable.Empty(EmptyReason.NoUpcomingBookings)
                            } else {
                                Loadable.Content(groups)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(BookingListEffect.SignedOut)
                    } else {
                        mutableState.update { it.copy(bookings = Loadable.Error(appFailure)) }
                    }
                },
            )
        }
    }
}

private fun List<Booking>.toGroups(clock: AppClock): BookingGroups {
    val now = clock.now()
    val (upcoming, past) = partition { booking ->
        booking.status == BookingStatus.Active && booking.slot?.startAt?.let { it > now } == true
    }
    return BookingGroups(
        upcoming = upcoming.sortedBy { it.slot?.startAt },
        past = past.sortedByDescending { it.slot?.startAt },
    )
}
