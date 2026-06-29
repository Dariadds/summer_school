package com.volna.app.catalog.presentation

import com.volna.app.catalog.PageRequest
import com.volna.app.catalog.InstructorRepository
import com.volna.app.catalog.SlotFilters
import com.volna.app.catalog.SlotRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.logging.AppLogger
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.EmptyReason
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Instructor
import com.volna.app.domain.model.InstructorId
import com.volna.app.domain.model.RouteType
import com.volna.app.domain.model.Slot
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SlotListState(
    val slots: Loadable<List<Slot>> = Loadable.Initial,
    val filters: SlotFilters = SlotFilters(),
    val datePreset: SlotDatePreset = SlotDatePreset.Any,
    val draftFilters: SlotFilters = SlotFilters(),
    val draftDatePreset: SlotDatePreset = SlotDatePreset.Any,
    val instructors: Loadable<List<Instructor>> = Loadable.Initial,
    val filtersVisible: Boolean = false,
)

enum class SlotDatePreset {
    Any,
    Today,
    NextSevenDays,
}

sealed interface SlotListIntent {
    data object Load : SlotListIntent
    data object Retry : SlotListIntent
    data object OpenFilters : SlotListIntent
    data object CloseFilters : SlotListIntent
    data object ApplyFilters : SlotListIntent
    data object ResetFilters : SlotListIntent
    data object RetryInstructors : SlotListIntent
    data class SelectDatePreset(val preset: SlotDatePreset) : SlotListIntent
    data class ToggleRouteType(val routeType: RouteType) : SlotListIntent
    data class ToggleInstructor(val instructorId: InstructorId) : SlotListIntent
    data object ToggleOnlyAvailable : SlotListIntent
    data object Reset : SlotListIntent
}

sealed interface SlotListEffect {
    data object SignedOut : SlotListEffect
}

// CMP-13 / BS-001: slot filters, instructor dictionary loading, and SCR-002 filtered reload.
class SlotListStore(
    private val slotRepository: SlotRepository,
    private val instructorRepository: InstructorRepository,
    private val scope: CoroutineScope,
) : MviStore<SlotListState, SlotListIntent, SlotListEffect> {
    private val mutableState = MutableStateFlow(SlotListState())
    private val effects = Channel<SlotListEffect>(Channel.BUFFERED)

    override val state: StateFlow<SlotListState> = mutableState

    override fun accept(intent: SlotListIntent) {
        when (intent) {
            SlotListIntent.Load -> load()
            SlotListIntent.Retry -> load()
            SlotListIntent.OpenFilters -> openFilters()
            SlotListIntent.CloseFilters -> mutableState.update { it.copy(filtersVisible = false) }
            SlotListIntent.ApplyFilters -> applyFilters()
            SlotListIntent.ResetFilters -> mutableState.update {
                it.copy(draftFilters = SlotFilters(), draftDatePreset = SlotDatePreset.Any)
            }
            SlotListIntent.RetryInstructors -> loadInstructors(force = true)
            is SlotListIntent.SelectDatePreset -> selectDatePreset(intent.preset)
            is SlotListIntent.ToggleRouteType -> toggleRouteType(intent.routeType)
            is SlotListIntent.ToggleInstructor -> toggleInstructor(intent.instructorId)
            SlotListIntent.ToggleOnlyAvailable -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(onlyAvailable = !it.draftFilters.onlyAvailable))
            }
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
                                Loadable.Empty(
                                    if (filters == SlotFilters()) {
                                        EmptyReason.NoSlots
                                    } else {
                                        EmptyReason.NoSlotsByFilters
                                    },
                                )
                            } else {
                                Loadable.Content(page.items)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load slots")
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

    private fun openFilters() {
        mutableState.update {
            it.copy(
                filtersVisible = true,
                draftFilters = it.filters,
                draftDatePreset = it.datePreset,
            )
        }
        loadInstructors(force = false)
    }

    private fun applyFilters() {
        mutableState.update {
            it.copy(
                filters = it.draftFilters,
                datePreset = it.draftDatePreset,
                filtersVisible = false,
                slots = Loadable.Initial,
            )
        }
        load()
    }

    private fun selectDatePreset(preset: SlotDatePreset) {
        val now = Clock.System.now()
        val filters = when (preset) {
            SlotDatePreset.Any -> SlotFilters()
            SlotDatePreset.Today -> SlotFilters(dateFrom = now, dateTo = now + 1.days)
            SlotDatePreset.NextSevenDays -> SlotFilters(dateFrom = now, dateTo = now + 7.days)
        }
        mutableState.update {
            it.copy(
                draftDatePreset = preset,
                draftFilters = it.draftFilters.copy(
                    dateFrom = filters.dateFrom,
                    dateTo = filters.dateTo,
                ),
            )
        }
    }

    private fun toggleRouteType(routeType: RouteType) {
        mutableState.update { state ->
            state.copy(
                draftFilters = state.draftFilters.copy(
                    routeTypes = state.draftFilters.routeTypes.toggle(routeType),
                ),
            )
        }
    }

    private fun toggleInstructor(instructorId: InstructorId) {
        mutableState.update { state ->
            state.copy(
                draftFilters = state.draftFilters.copy(
                    instructorIds = state.draftFilters.instructorIds.toggle(instructorId),
                ),
            )
        }
    }

    private fun loadInstructors(force: Boolean) {
        val current = mutableState.value.instructors
        if (!force && (current == Loadable.Loading || current is Loadable.Content || current is Loadable.Empty)) return

        scope.launch {
            mutableState.update { it.copy(instructors = Loadable.Loading) }
            instructorRepository.listInstructors().fold(
                onSuccess = { page ->
                    mutableState.update {
                        it.copy(
                            instructors = if (page.items.isEmpty()) {
                                Loadable.Empty(EmptyReason.NoSlots)
                            } else {
                                Loadable.Content(page.items)
                            },
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load instructors")
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(SlotListEffect.SignedOut)
                    } else {
                        mutableState.update { it.copy(instructors = Loadable.Error(appFailure)) }
                    }
                },
            )
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
