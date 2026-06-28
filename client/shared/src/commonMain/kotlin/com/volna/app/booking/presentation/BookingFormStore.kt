package com.volna.app.booking.presentation

import com.volna.app.booking.BookingRepository
import com.volna.app.booking.IdempotencyKey
import com.volna.app.booking.IdempotencyKeyFactory
import com.volna.app.core.error.ApiErrorCode
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.ActionStatus
import com.volna.app.domain.model.Booking
import com.volna.app.domain.model.BookingDraft
import com.volna.app.domain.model.MoneyRub
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.domain.policy.AvailabilityPolicy
import com.volna.app.domain.policy.AvailabilityViolation
import com.volna.app.domain.policy.BookingPriceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingFormState(
    val slot: Slot? = null,
    val seatsCount: Int = 1,
    val rentalCount: Int = 0,
    val actionStatus: ActionStatus = ActionStatus.Idle,
    val message: String? = null,
    val createdBooking: Booking? = null,
    val idempotencyKey: IdempotencyKey? = null,
    val idempotencyPayload: BookingPayload? = null,
) {
    val isSubmitting: Boolean = actionStatus == ActionStatus.Submitting
    val availability = slot?.let(AvailabilityPolicy::availability)
    val totalPrice: MoneyRub? = slot?.let {
        BookingPriceCalculator.calculate(it, seatsCount, rentalCount)
    }
    val canSubmit: Boolean = slot != null &&
        !isSubmitting &&
        createdBooking == null &&
        validationMessage == null
    val validationMessage: String?
        get() {
            val currentSlot = slot ?: return null
            return AvailabilityPolicy.validate(
                BookingDraft(
                    slot = currentSlot,
                    seatsCount = seatsCount,
                    rentalCount = rentalCount,
                ),
            )?.toUserMessage()
        }
}

data class BookingPayload(
    val slotId: SlotId,
    val seatsCount: Int,
    val rentalCount: Int,
)

sealed interface BookingFormIntent {
    data class Open(val slot: Slot) : BookingFormIntent
    data object IncrementSeats : BookingFormIntent
    data object DecrementSeats : BookingFormIntent
    data object IncrementRental : BookingFormIntent
    data object DecrementRental : BookingFormIntent
    data object Submit : BookingFormIntent
    data object MessageShown : BookingFormIntent
    data object SuccessDismissed : BookingFormIntent
    data object Reset : BookingFormIntent
}

sealed interface BookingFormEffect {
    data object SignedOut : BookingFormEffect
}

class BookingFormStore(
    private val bookingRepository: BookingRepository,
    private val keyFactory: IdempotencyKeyFactory,
    private val scope: CoroutineScope,
) : MviStore<BookingFormState, BookingFormIntent, BookingFormEffect> {
    private val mutableState = MutableStateFlow(BookingFormState())
    private val effects = Channel<BookingFormEffect>(Channel.BUFFERED)

    override val state: StateFlow<BookingFormState> = mutableState

    override fun accept(intent: BookingFormIntent) {
        when (intent) {
            is BookingFormIntent.Open -> open(intent.slot)
            BookingFormIntent.IncrementSeats -> changeSeats(delta = 1)
            BookingFormIntent.DecrementSeats -> changeSeats(delta = -1)
            BookingFormIntent.IncrementRental -> changeRental(delta = 1)
            BookingFormIntent.DecrementRental -> changeRental(delta = -1)
            BookingFormIntent.Submit -> submit()
            BookingFormIntent.MessageShown -> mutableState.update { it.copy(message = null) }
            BookingFormIntent.SuccessDismissed -> mutableState.update { it.copy(createdBooking = null) }
            BookingFormIntent.Reset -> mutableState.value = BookingFormState()
        }
    }

    override suspend fun effects(): BookingFormEffect = effects.receive()

    private fun open(slot: Slot) {
        val maxSeats = AvailabilityPolicy.availability(slot).maxSeatsForBooking.coerceAtLeast(1)
        mutableState.value = BookingFormState(
            slot = slot,
            seatsCount = 1.coerceAtMost(maxSeats),
            rentalCount = 0,
        )
    }

    private fun changeSeats(delta: Int) {
        mutableState.update { state ->
            val maxSeats = state.availability?.maxSeatsForBooking ?: 1
            val nextSeats = (state.seatsCount + delta).coerceIn(1, maxSeats.coerceAtLeast(1))
            state.copy(
                seatsCount = nextSeats,
                rentalCount = state.rentalCount.coerceAtMost(nextSeats),
                idempotencyKey = null,
                idempotencyPayload = null,
                message = null,
            )
        }
    }

    private fun changeRental(delta: Int) {
        mutableState.update { state ->
            val maxRental = minOf(state.seatsCount, state.slot?.freeRentalBoards ?: 0)
            state.copy(
                rentalCount = (state.rentalCount + delta).coerceIn(0, maxRental),
                idempotencyKey = null,
                idempotencyPayload = null,
                message = null,
            )
        }
    }

    private fun submit() {
        val state = mutableState.value
        val slot = state.slot ?: return
        if (state.isSubmitting) return
        state.validationMessage?.let { message ->
            mutableState.update { it.copy(message = message) }
            return
        }

        val payload = BookingPayload(
            slotId = slot.id,
            seatsCount = state.seatsCount,
            rentalCount = state.rentalCount,
        )
        val idempotencyKey = if (state.idempotencyPayload == payload) {
            state.idempotencyKey ?: keyFactory.next()
        } else {
            keyFactory.next()
        }

        scope.launch {
            mutableState.update {
                it.copy(
                    actionStatus = ActionStatus.Submitting,
                    message = null,
                    idempotencyKey = idempotencyKey,
                    idempotencyPayload = payload,
                )
            }
            bookingRepository.createBooking(
                draft = BookingDraft(
                    slot = slot,
                    seatsCount = payload.seatsCount,
                    rentalCount = payload.rentalCount,
                ),
                idempotencyKey = idempotencyKey,
            ).fold(
                onSuccess = { booking ->
                    mutableState.update {
                        it.copy(
                            actionStatus = ActionStatus.Idle,
                            createdBooking = booking,
                            idempotencyKey = null,
                            idempotencyPayload = null,
                        )
                    }
                },
                onFailure = { failure ->
                    handleFailure(failure.asAppFailure())
                },
            )
        }
    }

    private suspend fun handleFailure(appFailure: AppFailure) {
        if (appFailure == AppFailure.Unauthorized) {
            mutableState.update { it.copy(actionStatus = ActionStatus.Idle) }
            effects.send(BookingFormEffect.SignedOut)
            return
        }

        mutableState.update { state ->
            val updatedSlot = if (appFailure is AppFailure.Api && appFailure.code == ApiErrorCode.SlotFull) {
                state.slot?.copy(
                    freeSeats = appFailure.details?.availableSeats ?: state.slot.freeSeats,
                    freeRentalBoards = appFailure.details?.availableRentalBoards ?: state.slot.freeRentalBoards,
                )
            } else {
                state.slot
            }
            val updatedSeatsCount = updatedSlot?.let {
                state.seatsCount.coerceAtMost(
                    AvailabilityPolicy.availability(it).maxSeatsForBooking.coerceAtLeast(1),
                )
            } ?: state.seatsCount
            state.copy(
                slot = updatedSlot,
                seatsCount = updatedSeatsCount,
                rentalCount = updatedSlot?.let { state.rentalCount.coerceAtMost(minOf(updatedSeatsCount, it.freeRentalBoards)) }
                    ?: state.rentalCount,
                actionStatus = ActionStatus.Idle,
                message = appFailure.toUserMessage(),
            )
        }
    }
}

private fun AvailabilityViolation.toUserMessage(): String = when (this) {
    AvailabilityViolation.NoSeats -> "В этом слоте больше нет свободных мест"
    AvailabilityViolation.SlotCancelled -> "Прогулка отменена"
    is AvailabilityViolation.TooManyRentalBoards -> "Доступно прокатных досок: $freeRentalBoards"
    is AvailabilityViolation.TooManySeats -> "Можно выбрать не больше $maxSeats мест"
}

private fun AppFailure.toUserMessage(): String = when (this) {
    AppFailure.NetworkUnavailable -> "Нет соединения. Проверьте интернет и попробуйте снова"
    AppFailure.Timeout -> "Сервер не ответил вовремя. Попробуйте ещё раз"
    AppFailure.Unknown -> "Не удалось оформить запись"
    AppFailure.Unauthorized -> "Сессия истекла"
    is AppFailure.Api -> message
}
