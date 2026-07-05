package com.volna.app.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volna.app.catalog.SlotRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.logging.AppLogger
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.FavoriteRoute
import com.volna.app.domain.model.RecentRoute
import com.volna.app.domain.model.Slot
import com.volna.app.domain.model.SlotId
import com.volna.app.favorites.FavoritesRepository
import com.volna.app.recent.RecentRoutesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class SlotDetailsState(
    val slot: Loadable<Slot> = Loadable.Initial,
    val showRouteMap: Boolean = false,
    val isFavorite: Boolean = false,
    val favoriteProcessing: Boolean = false,
)

sealed interface SlotDetailsIntent {
    data class Load(val slotId: SlotId) : SlotDetailsIntent
    data object Retry : SlotDetailsIntent
    data object OpenRouteMap : SlotDetailsIntent
    data object DismissRouteMap : SlotDetailsIntent
    data object ToggleFavorite : SlotDetailsIntent
    data object Reset : SlotDetailsIntent
}

sealed interface SlotDetailsEffect {
    data object SignedOut : SlotDetailsEffect
}

class SlotDetailsStore(
    private val slotRepository: SlotRepository,
    private val recentRoutesRepository: RecentRoutesRepository,
    private val favoritesRepository: FavoritesRepository,
    scope: CoroutineScope? = null,
) : ViewModel(), MviStore<SlotDetailsState, SlotDetailsIntent, SlotDetailsEffect> {
    private val mutableState = MutableStateFlow(SlotDetailsState())
    private val effects = Channel<SlotDetailsEffect>(Channel.BUFFERED)
    private val storeScope = scope ?: viewModelScope
    private var lastSlotId: SlotId? = null

    override val state: StateFlow<SlotDetailsState> = mutableState

    override fun accept(intent: SlotDetailsIntent) {
        when (intent) {
            is SlotDetailsIntent.Load -> load(intent.slotId)
            SlotDetailsIntent.Retry -> lastSlotId?.let(::load)
            SlotDetailsIntent.OpenRouteMap -> mutableState.update { it.copy(showRouteMap = true) }
            SlotDetailsIntent.DismissRouteMap -> mutableState.update { it.copy(showRouteMap = false) }
            SlotDetailsIntent.ToggleFavorite -> toggleFavorite()
            SlotDetailsIntent.Reset -> {
                lastSlotId = null
                mutableState.value = SlotDetailsState()
            }
        }
    }

    override suspend fun effects(): SlotDetailsEffect = effects.receive()

    private fun load(slotId: SlotId) {
        if (mutableState.value.slot == Loadable.Loading && lastSlotId == slotId) return
        lastSlotId = slotId

        storeScope.launch {
            mutableState.update {
                it.copy(
                    slot = Loadable.Loading,
                    showRouteMap = false,
                    isFavorite = false,
                    favoriteProcessing = false
                )
            }

            slotRepository.getSlot(slotId).fold(
                onSuccess = { slot ->
                    mutableState.update { it.copy(slot = Loadable.Content(slot)) }

                    recentRoutesRepository.addRecent(
                        RecentRoute(
                            slotId = slot.id.value,
                            routeName = slot.route.name,
                            routeDescription = "${slot.route.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${slot.route.durationMin} мин",
                            price = slot.price.value,
                            instructorName = slot.instructor.name,
                            viewedAt = System.currentTimeMillis()
                        )
                    )

                    favoritesRepository.isFavorite(slot.id.value).fold(
                        onSuccess = { isFavorite ->
                            mutableState.update { it.copy(isFavorite = isFavorite) }
                        },
                        onFailure = {
                            // Ignore favorite lookup failures.
                        }
                    )
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load slot details")
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(SlotDetailsEffect.SignedOut)
                    } else {
                        mutableState.update { it.copy(slot = Loadable.Error(appFailure)) }
                    }
                }
            )
        }
    }

    private fun toggleFavorite() {
        val slot = (mutableState.value.slot as? Loadable.Content)?.value ?: return
        if (mutableState.value.favoriteProcessing) return

        val currentlyFavorite = mutableState.value.isFavorite
        mutableState.update { it.copy(favoriteProcessing = true) }

        storeScope.launch {
            val result = if (currentlyFavorite) {
                favoritesRepository.removeFavorite(slot.id.value)
            } else {
                favoritesRepository.addFavorite(
                    FavoriteRoute(
                        slotId = slot.id.value,
                        routeName = slot.route.name,
                        routeDescription = "${slot.route.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${slot.route.durationMin} min",
                        price = slot.price.value,
                        instructorName = slot.instructor.name,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }

            mutableState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        isFavorite = !currentlyFavorite,
                        favoriteProcessing = false
                    )
                } else {
                    AppLogger.e(
                        result.exceptionOrNull() ?: Exception("Failed to toggle favorite"),
                        "Failed to toggle favorite route"
                    )
                    state.copy(favoriteProcessing = false)
                }
            }
        }
    }
}