package com.volna.app.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = VolnaTheme.tokens.sizing.screenMaxWidth),
        ) {
            Text(
                text = "Профиль",
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = VolnaTheme.tokens.sizing.topTitleY),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            when (val profile = state.profile) {
                Loadable.Initial,
                Loadable.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
                is Loadable.Content -> ProfileContent(
                    state = state,
                    clientName = profile.value.name.orEmpty(),
                    phone = profile.value.phone.value,
                    onIntent = onIntent,
                )
                is Loadable.Error -> ProfileError(onRetry = { onIntent(ProfileIntent.Load) })
                is Loadable.Empty -> ProfileError(onRetry = { onIntent(ProfileIntent.Load) })
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(VolnaTheme.tokens.spacing.md),
            )
        }
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
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.profileInfoY,
            ),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        ProfileInfoRow(label = "Имя", value = clientName.ifBlank { "Имя не указано" })
        ProfileInfoRow(label = "Телефон", value = phone)
    }
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.profileLinksY,
            ),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        InfoLine("Правила клуба", "›")
        InfoLine("Поддержка", "›")
        InfoLine("Версия приложения", "1.0.0")
    }
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.LogoutClicked) },
        enabled = state.actionStatus == ActionStatus.Idle,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .height(VolnaTheme.tokens.sizing.buttonHeight)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.profileLogoutY),
    ) {
        Text(if (state.isSubmitting) "Выходим..." else "Выйти", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Text("✎", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileError(
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.stateMessageY,
            ),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Не удалось загрузить профиль", textAlign = TextAlign.Center)
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
