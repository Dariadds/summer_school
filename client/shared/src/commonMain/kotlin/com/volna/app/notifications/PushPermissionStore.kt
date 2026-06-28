package com.volna.app.notifications

import com.volna.app.core.mvi.MviStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PushPermissionState(
    val showPrompt: Boolean = false,
    val requested: Boolean = false,
    val lastStatus: PushPermissionStatus = PushPermissionStatus.NotDetermined,
)

sealed interface PushPermissionIntent {
    data object BookingSuccessShown : PushPermissionIntent
    data object RequestPermission : PushPermissionIntent
    data object DismissPrompt : PushPermissionIntent
}

sealed interface PushPermissionEffect

// CMP-16 / LOGIC-007: one calm push permission prompt after booking success.
class PushPermissionStore(
    private val gateway: PushPermissionGateway,
    private val flagStore: PushPermissionFlagStore,
    private val scope: CoroutineScope,
) : MviStore<PushPermissionState, PushPermissionIntent, PushPermissionEffect> {
    private val mutableState = MutableStateFlow(PushPermissionState())
    private val effects = Channel<PushPermissionEffect>(Channel.BUFFERED)

    override val state: StateFlow<PushPermissionState> = mutableState

    override fun accept(intent: PushPermissionIntent) {
        when (intent) {
            PushPermissionIntent.BookingSuccessShown -> maybeShowPrompt()
            PushPermissionIntent.RequestPermission -> requestPermission()
            PushPermissionIntent.DismissPrompt -> dismissPrompt()
        }
    }

    override suspend fun effects(): PushPermissionEffect = effects.receive()

    private fun maybeShowPrompt() {
        scope.launch {
            if (!flagStore.wasRequested()) {
                mutableState.update { it.copy(showPrompt = true) }
            }
        }
    }

    private fun requestPermission() {
        scope.launch {
            flagStore.markRequested()
            val status = gateway.requestPermission()
            mutableState.update {
                it.copy(
                    showPrompt = false,
                    requested = true,
                    lastStatus = status,
                )
            }
        }
    }

    private fun dismissPrompt() {
        scope.launch {
            flagStore.markRequested()
            mutableState.update {
                it.copy(
                    showPrompt = false,
                    requested = true,
                )
            }
        }
    }
}
