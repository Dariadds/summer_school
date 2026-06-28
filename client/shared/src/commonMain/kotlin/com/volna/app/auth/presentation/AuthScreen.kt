package com.volna.app.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.volna.app.core.theme.VolnaTheme

@Composable
fun AuthScreen(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onIntent(AuthIntent.MessageShown)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Волна",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (state.step) {
                    AuthStep.Phone -> "Войдите по номеру телефона"
                    AuthStep.Otp -> "Мы отправили код на ${state.phoneInput}"
                    AuthStep.Name -> "Как к вам обращаться?"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(VolnaTheme.tokens.spacing.lg))

            when (state.step) {
                AuthStep.Phone -> PhoneStep(state, onIntent)
                AuthStep.Otp -> OtpStep(state, onIntent)
                AuthStep.Name -> NameStep(state, onIntent)
            }
        }
    }
}

@Composable
private fun PhoneStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthTextField(
        value = state.phoneInput,
        onValueChange = { onIntent(AuthIntent.PhoneChanged(it)) },
        label = "Телефон",
        placeholder = "+79991234567",
        keyboardType = KeyboardType.Phone,
        fieldError = state.fieldError,
    )
    if (state.resendSecondsRemaining > 0) {
        Spacer(Modifier.height(VolnaTheme.tokens.spacing.xs))
        Text(
            text = "Повторный код можно запросить через ${state.resendSecondsRemaining} с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(VolnaTheme.tokens.spacing.md))
    SubmitButton(
        text = "Получить код",
        loading = state.isSubmitting,
        enabled = state.canRequestCode,
        onClick = { onIntent(AuthIntent.RequestCode) },
    )
}

@Composable
private fun OtpStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthTextField(
        value = state.codeInput,
        onValueChange = { onIntent(AuthIntent.CodeChanged(it)) },
        label = "Код из SMS",
        placeholder = "1234",
        keyboardType = KeyboardType.Number,
        fieldError = state.fieldError,
    )
    Spacer(Modifier.height(VolnaTheme.tokens.spacing.md))
    SubmitButton(
        text = "Подтвердить",
        loading = state.isSubmitting,
        enabled = state.canVerifyCode,
        onClick = { onIntent(AuthIntent.VerifyCode) },
    )
    Spacer(Modifier.height(VolnaTheme.tokens.spacing.xs))
    TextButton(
        enabled = state.canResendCode,
        onClick = { onIntent(AuthIntent.ResendCode) },
    ) {
        Text(
            if (state.resendSecondsRemaining > 0) {
                "Отправить код повторно (${state.resendSecondsRemaining})"
            } else {
                "Отправить код повторно"
            },
        )
    }
    TextButton(
        enabled = !state.isSubmitting,
        onClick = { onIntent(AuthIntent.BackToPhone) },
    ) {
        Text("Изменить телефон")
    }
}

@Composable
private fun NameStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthTextField(
        value = state.nameInput,
        onValueChange = { onIntent(AuthIntent.NameChanged(it)) },
        label = "Имя",
        placeholder = "Иван",
        keyboardType = KeyboardType.Text,
        fieldError = state.fieldError,
    )
    Spacer(Modifier.height(VolnaTheme.tokens.spacing.md))
    SubmitButton(
        text = "Продолжить",
        loading = state.isSubmitting,
        enabled = state.canContinueName,
        onClick = { onIntent(AuthIntent.ContinueWithName) },
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    fieldError: String?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = fieldError != null,
        supportingText = fieldError?.let { error ->
            { Text(error) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubmitButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = VolnaTheme.tokens.spacing.xxs,
            )
        } else {
            Text(text)
        }
    }
}
