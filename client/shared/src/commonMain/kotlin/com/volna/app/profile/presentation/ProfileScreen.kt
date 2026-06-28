package com.volna.app.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.ui.ActionStatus
import com.volna.app.core.ui.Loadable

@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onIntent(ProfileIntent.Load)
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onIntent(ProfileIntent.MessageShown)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        when (val profile = state.profile) {
            Loadable.Initial,
            Loadable.Loading -> CircularProgressIndicator()
            is Loadable.Content -> ProfileContent(
                state = state,
                clientName = profile.value.name.orEmpty(),
                phone = profile.value.phone.value,
                onIntent = onIntent,
            )
            is Loadable.Error -> ProfileError(onRetry = { onIntent(ProfileIntent.Load) })
            is Loadable.Empty -> ProfileError(onRetry = { onIntent(ProfileIntent.Load) })
        }
        SnackbarHost(snackbarHostState)
    }

    if (state.logoutConfirmVisible) {
        LogoutConfirmDialog(
            onConfirm = { onIntent(ProfileIntent.LogoutConfirmed) },
            onDismiss = { onIntent(ProfileIntent.LogoutDismissed) },
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    clientName: String,
    phone: String,
    onIntent: (ProfileIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm)) {
        Text(
            text = clientName.ifBlank { "Имя не указано" },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = phone,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onIntent(ProfileIntent.LogoutClicked) },
            enabled = state.actionStatus == ActionStatus.Idle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSubmitting) "Выходим..." else "Выйти")
        }
    }
}

@Composable
private fun ProfileError(
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm)) {
        Text("Не удалось загрузить профиль")
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выйти из аккаунта?") },
        text = { Text("После выхода для записи на прогулку нужно будет снова ввести телефон и код.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Выйти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Не выходить")
            }
        },
    )
}
