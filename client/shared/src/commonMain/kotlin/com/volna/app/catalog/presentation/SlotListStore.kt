package com.volna.app.catalog.presentation

import com.volna.app.catalog.PageRequest
import com.volna.app.catalog.SlotFilters
import com.volna.app.catalog.SlotRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SlotListState(
    val slots: Loadable<List<Slot>> = Loadable.Initial,
    val filters: SlotFilters = SlotFilters(),
)

sealed interface SlotListIntent {
    data object Load : SlotListIntent
    data object Retry : SlotListIntent
    data object Reset : SlotListIntent
}

sealed interface SlotListEffect {
    data object SignedOut : SlotListEffect
}

class SlotListStore(
    private val slotRepository: SlotRepository,
    private val scope: CoroutineScope,
) : MviStore<SlotListState, SlotListIntent, SlotListEffect> {
    private val mutableState = MutableStateFlow(SlotListState())
    private val effects = Channel<SlotListEffect>(Channel.BUFFERED)

    override val state: StateFlow<SlotListState> = mutableState

    override fun accept(intent: SlotListIntent) {
        when (intent) {
            SlotListIntent.Load -> load()
            SlotListIntent.Retry -> load()
            SlotListIntent.Reset -> mutableState.value = SlotListState()
        }
    }

    override suspend fun effects(): SlotListEffect = effects.receive()

    private fun load() {
        if (mutableState.value.slots == Loadable.Loading) return

        scope.launch {
            val filters = mutableState.value.filters
            mutableState.update { it.copy(slots = Loadable.Loading) }
            slotRepository.listSlots(filters, PageRequest()).fold(
                onSuccess = { page ->
                    mutableState.update {
                        it.copy(
                            slots = if (page.items.isEmpty()) {
                                Loadable.Empty(EmptyReason.NoSlots)
                            } else {
                                Loadable.Content(page.items)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(SlotListEffect.SignedOut)
                    } else {
                        mutableState.update { it.copy(slots = Loadable.Error(appFailure)) }
                    }
                },
            )
        }
    }
}
