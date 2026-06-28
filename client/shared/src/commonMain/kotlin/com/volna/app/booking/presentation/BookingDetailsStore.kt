package com.volna.app.booking.presentation

import com.volna.app.booking.BookingRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.time.AppClock
import com.volna.app.core.ui.ActionStatus
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingId
import com.volna.app.domain.model.BookingStatus
import com.volna.app.domain.policy.CancellationKind
import com.volna.app.domain.policy.CancellationPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingDetailsState(
    val booking: Loadable<Booking> = Loadable.Initial,
    val cancelStatus: ActionStatus = ActionStatus.Idle,
    val showCancelConfirm: Boolean = false,
    val showRouteMap: Boolean = false,
    val message: String? = null,
) {
    val currentBooking: Booking?
        get() = (booking as? Loadable.Content)?.value
    val isCancelling: Boolean = cancelStatus == ActionStatus.Submitting
}

sealed interface BookingDetailsIntent {
    data class Load(val bookingId: BookingId) : BookingDetailsIntent
    data object Retry : BookingDetailsIntent
    data object AskCancel : BookingDetailsIntent
    data object DismissCancel : BookingDetailsIntent
    data object ConfirmCancel : BookingDetailsIntent
    data object OpenRouteMap : BookingDetailsIntent
    data object DismissRouteMap : BookingDetailsIntent
    data object MessageShown : BookingDetailsIntent
    data object Reset : BookingDetailsIntent
}

sealed interface BookingDetailsEffect {
    data object SignedOut : BookingDetailsEffect
    data object BookingChanged : BookingDetailsEffect
}

// CMP-12 / SCR-006 / BS-003: getBooking details and confirmed cancellation flow.
class BookingDetailsStore(
    private val bookingRepository: BookingRepository,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) : MviStore<BookingDetailsState, BookingDetailsIntent, BookingDetailsEffect> {
    private val mutableState = MutableStateFlow(BookingDetailsState())
    private val effects = Channel<BookingDetailsEffect>(Channel.BUFFERED)
    private var lastBookingId: BookingId? = null

    override val state: StateFlow<BookingDetailsState> = mutableState

    override fun accept(intent: BookingDetailsIntent) {
        when (intent) {
            is BookingDetailsIntent.Load -> load(intent.bookingId)
            BookingDetailsIntent.Retry -> lastBookingId?.let(::load)
            BookingDetailsIntent.AskCancel -> mutableState.update {
                if (it.canCancel(clock)) it.copy(showCancelConfirm = true, message = null) else it
            }
            BookingDetailsIntent.DismissCancel -> mutableState.update { it.copy(showCancelConfirm = false) }
            BookingDetailsIntent.ConfirmCancel -> cancel()
            BookingDetailsIntent.OpenRouteMap -> mutableState.update { it.copy(showRouteMap = true) }
            BookingDetailsIntent.DismissRouteMap -> mutableState.update { it.copy(showRouteMap = false) }
            BookingDetailsIntent.MessageShown -> mutableState.update { it.copy(message = null) }
            BookingDetailsIntent.Reset -> {
                lastBookingId = null
                mutableState.value = BookingDetailsState()
            }
        }
    }

    override suspend fun effects(): BookingDetailsEffect = effects.receive()

    private fun load(bookingId: BookingId) {
        if (mutableState.value.booking == Loadable.Loading && lastBookingId == bookingId) return
        lastBookingId = bookingId

        scope.launch {
            mutableState.update {
                it.copy(
                    booking = Loadable.Loading,
                    showCancelConfirm = false,
                    showRouteMap = false,
                    message = null,
                )
            }
            bookingRepository.getBooking(bookingId).fold(
                onSuccess = { booking ->
                    mutableState.update { it.copy(booking = Loadable.Content(booking)) }
                },
                onFailure = { failure -> handleFailure(failure.asAppFailure(), contentFallback = null) },
            )
        }
    }

    private fun cancel() {
        val booking = mutableState.value.currentBooking ?: return
        if (mutableState.value.isCancelling || !mutableState.value.canCancel(clock)) return

        scope.launch {
            mutableState.update {
                it.copy(
                    cancelStatus = ActionStatus.Submitting,
                    message = null,
                )
            }
            bookingRepository.cancelBooking(booking.id).fold(
                onSuccess = { updatedBooking ->
                    mutableState.update {
                        it.copy(
                            booking = Loadable.Content(updatedBooking),
                            cancelStatus = ActionStatus.Idle,
                            showCancelConfirm = false,
                            showRouteMap = false,
                            message = updatedBooking.cancelSuccessMessage(),
                        )
                    }
                    effects.send(BookingDetailsEffect.BookingChanged)
                },
                onFailure = { failure ->
                    handleFailure(failure.asAppFailure(), contentFallback = booking)
                },
            )
        }
    }

    private suspend fun handleFailure(appFailure: AppFailure, contentFallback: Booking?) {
        if (appFailure == AppFailure.Unauthorized) {
            mutableState.update {
                it.copy(
                    cancelStatus = ActionStatus.Idle,
                    showCancelConfirm = false,
                    showRouteMap = false,
                )
            }
            effects.send(BookingDetailsEffect.SignedOut)
            return
        }

        mutableState.update {
            if (contentFallback != null) {
                it.copy(
                    booking = Loadable.Content(contentFallback),
                    cancelStatus = ActionStatus.Idle,
                    showCancelConfirm = true,
                    message = appFailure.toUserMessage(),
                )
            } else {
                it.copy(
                    booking = Loadable.Error(appFailure),
                    cancelStatus = ActionStatus.Idle,
                    showCancelConfirm = false,
                    message = null,
                )
            }
        }
    }
}

fun BookingDetailsState.canCancel(clock: AppClock): Boolean {
    val booking = currentBooking ?: return false
    val slotStartAt = booking.slot?.startAt ?: return false
    return booking.status == BookingStatus.Active &&
        CancellationPolicy.classify(clock.now(), slotStartAt) != CancellationKind.UnavailableAfterStart
}

fun BookingDetailsState.cancellationKind(clock: AppClock): CancellationKind? {
    val slotStartAt = currentBooking?.slot?.startAt ?: return null
    return CancellationPolicy.classify(clock.now(), slotStartAt)
}

private fun Booking.cancelSuccessMessage(): String = when (status) {
    BookingStatus.LateCancel -> "Поздняя отмена: место не освобождено. Штраф не взимается."
    else -> "Бронь отменена"
}

private fun AppFailure.toUserMessage(): String = when (this) {
    AppFailure.NetworkUnavailable -> "Нет соединения. Проверьте интернет и попробуйте снова"
    AppFailure.Timeout -> "Сервер не ответил вовремя. Попробуйте ещё раз"
    AppFailure.Unknown -> "Не удалось отменить запись"
    AppFailure.Unauthorized -> "Сессия истекла"
    is AppFailure.Api -> message
}
