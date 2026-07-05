package com.volna.app.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.logging.AppLogger
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.FavoriteRoute
import com.volna.app.favorites.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesState(
    val favorites: Loadable<List<FavoriteRoute>> = Loadable.Initial,
    val removing: Set<String> = emptySet(),
)

sealed interface FavoritesIntent {
    data object Load : FavoritesIntent
    data class Remove(val slotId: String) : FavoritesIntent
    data object Reset : FavoritesIntent
}

sealed interface FavoritesEffect {
    data object SignedOut : FavoritesEffect
}

class FavoritesStore(
    private val favoritesRepository: FavoritesRepository,
    scope: CoroutineScope? = null,
) : ViewModel(), MviStore<FavoritesState, FavoritesIntent, FavoritesEffect> {
    private val mutableState = MutableStateFlow(FavoritesState())
    private val effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    private val storeScope = scope ?: viewModelScope

    override val state: StateFlow<FavoritesState> = mutableState

    override fun accept(intent: FavoritesIntent) {
        when (intent) {
            FavoritesIntent.Load -> loadFavorites()
            is FavoritesIntent.Remove -> removeFavorite(intent.slotId)
            FavoritesIntent.Reset -> mutableState.value = FavoritesState()
        }
    }

    override suspend fun effects(): FavoritesEffect = effects.receive()

    private fun loadFavorites() {
        if (mutableState.value.favorites == Loadable.Loading) return

        storeScope.launch {
            mutableState.update { it.copy(favorites = Loadable.Loading) }
            favoritesRepository.getFavorites().fold(
                onSuccess = { favorites ->
                    mutableState.update {
                        it.copy(
                            favorites = Loadable.Content(favorites),
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to load favorite routes")
                    val appFailure = failure.asAppFailure()
                    mutableState.update { it.copy(favorites = Loadable.Error(appFailure)) }
                },
            )
        }
    }

    private fun removeFavorite(slotId: String) {
        if (mutableState.value.removing.contains(slotId)) return

        storeScope.launch {
            mutableState.update { it.copy(removing = it.removing + slotId) }
            favoritesRepository.removeFavorite(slotId).fold(
                onSuccess = {
                    mutableState.update { state ->
                        val updatedFavorites = when (val content = state.favorites) {
                            is Loadable.Content -> content.value.filterNot { it.slotId == slotId }
                            else -> emptyList()
                        }
                        state.copy(
                            favorites = Loadable.Content(updatedFavorites),
                            removing = state.removing - slotId,
                        )
                    }
                },
                onFailure = { failure ->
                    AppLogger.e(failure, "Failed to remove favorite route")
                    mutableState.update { it.copy(removing = it.removing - slotId) }
                },
            )
        }
    }
}
