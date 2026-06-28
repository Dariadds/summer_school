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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    if (state.deleteConfirmVisible) {
        DeleteAccountConfirmDialog(
            onConfirm = { onIntent(ProfileIntent.DeleteConfirmed) },
            onDismiss = { onIntent(ProfileIntent.DeleteDismissed) },
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
            .verticalScroll(rememberScrollState())
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.profileInfoY,
            ),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.md),
    ) {
        when (state.mode) {
            ProfileMode.View -> ProfileViewContent(
                state = state,
                clientName = clientName,
                phone = phone,
                onIntent = onIntent,
            )
            ProfileMode.Edit -> ProfileEditContent(
                state = state,
                onIntent = onIntent,
            )
            ProfileMode.ConfirmPhone -> ProfilePhoneConfirmContent(
                state = state,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun ProfileViewContent(
    state: ProfileState,
    clientName: String,
    phone: String,
    onIntent: (ProfileIntent) -> Unit,
) {
    ProfileInfoRow(label = "Имя", value = clientName.ifBlank { "Имя не указано" })
    ProfileInfoRow(label = "Телефон", value = phone)
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.EditClicked) },
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text("Редактировать", fontWeight = FontWeight.Bold)
    }
    ProfileLinks()
    ProfileDangerActions(state = state, onIntent = onIntent)
}

@Composable
private fun ProfileEditContent(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
) {
    ProfileTextField(
        value = state.nameInput,
        onValueChange = { onIntent(ProfileIntent.NameChanged(it)) },
        label = "Имя",
        enabled = !state.isSubmitting,
    )
    ProfileTextField(
        value = state.phoneInput,
        onValueChange = { onIntent(ProfileIntent.PhoneChanged(it)) },
        label = "Телефон",
        enabled = !state.isSubmitting,
    )
    state.fieldError?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = { onIntent(ProfileIntent.SaveClicked) },
        enabled = state.canSave,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text(if (state.isSubmitting) "Сохраняем..." else "Сохранить", fontWeight = FontWeight.Bold)
    }
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.EditCancelled) },
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text("Отменить")
    }
}

@Composable
private fun ProfilePhoneConfirmContent(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
) {
    Text(
        text = "Подтвердите новый номер кодом из SMS",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = state.pendingPhone ?: state.phoneInput,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ProfileTextField(
        value = state.codeInput,
        onValueChange = { onIntent(ProfileIntent.CodeChanged(it)) },
        label = "Код из SMS",
        enabled = !state.isSubmitting,
    )
    state.fieldError?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = { onIntent(ProfileIntent.ConfirmPhoneClicked) },
        enabled = state.canConfirmPhone,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text(if (state.isSubmitting) "Проверяем..." else "Подтвердить", fontWeight = FontWeight.Bold)
    }
    TextButton(
        onClick = { onIntent(ProfileIntent.ResendPhoneCode) },
        enabled = state.canResendCode,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (state.resendSecondsRemaining > 0) {
                "Отправить код повторно (00:${state.resendSecondsRemaining.toString().padStart(2, '0')})"
            } else {
                "Отправить код повторно"
            },
        )
    }
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.BackToEdit) },
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text("Назад к редактированию")
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
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
    }
}

@Composable
private fun ProfileLinks() {
    Column(
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        InfoLine("Правила клуба", "›")
        InfoLine("Поддержка", "›")
        InfoLine("Версия приложения", "1.0.0")
    }
}

@Composable
private fun ProfileDangerActions(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
) {
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.LogoutClicked) },
        enabled = state.actionStatus == ActionStatus.Idle,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text(if (state.isSubmitting) "Выходим..." else "Выйти", fontWeight = FontWeight.Bold)
    }
    OutlinedButton(
        onClick = { onIntent(ProfileIntent.DeleteClicked) },
        enabled = state.actionStatus == ActionStatus.Idle,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        Text(if (state.isSubmitting) "Удаляем..." else "Удалить аккаунт", fontWeight = FontWeight.Bold)
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

@Composable
private fun DeleteAccountConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить аккаунт?") },
        text = { Text("Профиль будет удалён. Для новых записей потребуется зарегистрироваться снова.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отменить")
            }
        },
    )
}
