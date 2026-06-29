package com.volna.app.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.volna.app.core.phone.formatPhoneNumber
import com.volna.app.core.ui.PhoneNumberVisualTransformation
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
            when (state.step) {
                AuthStep.Phone -> PhoneStep(state, onIntent)
                AuthStep.Otp -> OtpStep(state, onIntent)
                AuthStep.Name -> NameStep(state, onIntent)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(VolnaTheme.tokens.spacing.md),
            )
        }
    }
}

@Composable
private fun PhoneStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthLogo()
    AuthHeader(
        title = "Вход",
        description = "Войдите по номеру телефона, чтобы\nзаписаться на прогулку",
    )
    AuthTextField(
        value = state.phoneInput,
        onValueChange = { onIntent(AuthIntent.PhoneChanged(it)) },
        label = "Телефон",
        placeholder = "+7 (___) ___-__-__",
        keyboardType = KeyboardType.Phone,
        visualTransformation = PhoneNumberVisualTransformation(),
        fieldError = state.fieldError,
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authInputY,
        ),
    )
    TermsText(
        text = if (state.resendSecondsRemaining > 0) {
            "Повторный код можно запросить через ${state.resendSecondsRemaining} с"
        } else {
            "Нажимая «Получить код», вы соглашаетесь\nс условиями сервиса"
        },
    )
    SubmitButton(
        text = "Получить код",
        loading = state.isSubmitting,
        enabled = state.canRequestCode,
        onClick = { onIntent(AuthIntent.RequestCode) },
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authButtonY,
        ),
    )
}

@Composable
private fun OtpStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    BackButton(onClick = { onIntent(AuthIntent.BackToPhone) })
    AuthHeader(
        title = "Подтверждение",
        description = "Мы отправили код на ${formatPhoneNumber(state.phoneInput)}",
    )
    OtpCodeInput(
        value = state.codeInput,
        onValueChange = { onIntent(AuthIntent.CodeChanged(it)) },
        fieldError = state.fieldError,
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authInputY,
        ),
    )
    TextButton(
        enabled = state.canResendCode,
        onClick = { onIntent(AuthIntent.ResendCode) },
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.authTermsY,
            ),
    ) {
        Text(
            text = if (state.resendSecondsRemaining > 0) {
                "Отправить код повторно (00:${state.resendSecondsRemaining.toString().padStart(2, '0')})"
            } else {
                "Отправить код повторно"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SubmitButton(
        text = "Подтвердить",
        loading = state.isSubmitting,
        enabled = state.canVerifyCode,
        onClick = { onIntent(AuthIntent.VerifyCode) },
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authButtonY,
        ),
    )
}

@Composable
private fun NameStep(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    AuthHeader(
        title = "Как вас зовут?",
        description = "Имя будет отображаться в вашем\nпрофиле",
    )
    AuthTextField(
        value = state.nameInput,
        onValueChange = { onIntent(AuthIntent.NameChanged(it)) },
        label = "Имя",
        placeholder = "Введите имя",
        keyboardType = KeyboardType.Text,
        fieldError = state.fieldError,
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authInputY,
        ),
    )
    TermsText("Продолжая, вы соглашаетесь на обработку\nперсональных данных")
    SubmitButton(
        text = "Продолжить",
        loading = state.isSubmitting,
        enabled = state.canContinueName,
        onClick = { onIntent(AuthIntent.ContinueWithName) },
        modifier = Modifier.offset(
            x = VolnaTheme.tokens.spacing.md,
            y = VolnaTheme.tokens.sizing.authButtonY,
        ),
    )
}

@Composable
private fun AuthLogo() {
    Text(
        text = "волна",
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = VolnaTheme.tokens.sizing.authLogoY),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
}

@Composable
private fun AuthHeader(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.authTitleY,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    fieldError: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(VolnaTheme.tokens.sizing.contentWidth),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(VolnaTheme.tokens.sizing.fieldHeight)
                        .border(
                            width = VolnaTheme.tokens.spacing.xxs / 2,
                            color = if (fieldError == null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            shape = RoundedCornerShape(VolnaTheme.tokens.radius.md),
                        )
                        .padding(horizontal = VolnaTheme.tokens.spacing.md),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        fieldError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    fieldError: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(VolnaTheme.tokens.sizing.contentWidth),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text("Код из SMS", style = MaterialTheme.typography.bodyMedium)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
                    ) {
                        repeat(4) { index ->
                            CodeBox(
                                digit = value.getOrNull(index)?.toString().orEmpty(),
                                isError = fieldError != null,
                            )
                        }
                    }
                    Box(modifier = Modifier.size(VolnaTheme.tokens.spacing.xxs)) {
                        innerTextField()
                    }
                }
            },
        )
        fieldError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CodeBox(
    digit: String,
    isError: Boolean,
) {
    Box(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.codeInputWidth)
            .height(VolnaTheme.tokens.sizing.fieldHeight)
            .border(
                width = VolnaTheme.tokens.spacing.xxs / 2,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.md),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.ifBlank { " " },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TermsText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(
                x = VolnaTheme.tokens.spacing.md,
                y = VolnaTheme.tokens.sizing.authTermsY,
            ),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.backButtonY)
            .size(VolnaTheme.tokens.spacing.xl + VolnaTheme.tokens.spacing.xs),
    ) {
        Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SubmitButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .height(VolnaTheme.tokens.sizing.buttonHeight),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = VolnaTheme.tokens.spacing.xxs,
            )
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}
