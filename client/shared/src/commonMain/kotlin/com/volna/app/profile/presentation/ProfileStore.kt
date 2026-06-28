package com.volna.app.profile.presentation

import com.volna.app.auth.AuthRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.asAppFailure
import com.volna.app.core.mvi.MviStore
import com.volna.app.core.ui.ActionStatus
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Client
import com.volna.app.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val profile: Loadable<Client> = Loadable.Initial,
    val actionStatus: ActionStatus = ActionStatus.Idle,
    val logoutConfirmVisible: Boolean = false,
    val message: String? = null,
) {
    val isSubmitting: Boolean = actionStatus == ActionStatus.Submitting
}

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data object LogoutClicked : ProfileIntent
    data object LogoutDismissed : ProfileIntent
    data object LogoutConfirmed : ProfileIntent
    data object MessageShown : ProfileIntent
    data object Reset : ProfileIntent
}

sealed interface ProfileEffect {
    data object SignedOut : ProfileEffect
}

class ProfileStore(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
) : MviStore<ProfileState, ProfileIntent, ProfileEffect> {
    private val mutableState = MutableStateFlow(ProfileState())
    private val effects = Channel<ProfileEffect>(Channel.BUFFERED)

    override val state: StateFlow<ProfileState> = mutableState

    override fun accept(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load -> loadProfile()
            ProfileIntent.LogoutClicked -> mutableState.update { it.copy(logoutConfirmVisible = true) }
            ProfileIntent.LogoutDismissed -> mutableState.update { it.copy(logoutConfirmVisible = false) }
            ProfileIntent.LogoutConfirmed -> logout()
            ProfileIntent.MessageShown -> mutableState.update { it.copy(message = null) }
            ProfileIntent.Reset -> mutableState.value = ProfileState()
        }
    }

    override suspend fun effects(): ProfileEffect = effects.receive()

    private fun loadProfile() {
        if (mutableState.value.profile == Loadable.Loading) return

        scope.launch {
            mutableState.update { it.copy(profile = Loadable.Loading, message = null) }
            profileRepository.getProfile().fold(
                onSuccess = { client ->
                    mutableState.update { it.copy(profile = Loadable.Content(client)) }
                },
                onFailure = { failure ->
                    val appFailure = failure.asAppFailure()
                    if (appFailure == AppFailure.Unauthorized) {
                        effects.send(ProfileEffect.SignedOut)
                    } else {
                        mutableState.update { it.copy(profile = Loadable.Error(appFailure)) }
                    }
                },
            )
        }
    }

    private fun logout() {
        if (mutableState.value.isSubmitting) return

        scope.launch {
            mutableState.update {
                it.copy(
                    actionStatus = ActionStatus.Submitting,
                    logoutConfirmVisible = false,
                    message = null,
                )
            }
            authRepository.logout().fold(
                onSuccess = {
                    mutableState.update { it.copy(actionStatus = ActionStatus.Idle) }
                    effects.send(ProfileEffect.SignedOut)
                },
                onFailure = {
                    mutableState.update { it.copy(actionStatus = ActionStatus.Idle) }
                    effects.send(ProfileEffect.SignedOut)
                },
            )
        }
    }
}
