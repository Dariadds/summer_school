package com.volna.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.volna.app.auth.data.DefaultSessionRepository
import com.volna.app.auth.data.KtorAuthRepository
import com.volna.app.auth.presentation.AuthEffect
import com.volna.app.auth.presentation.AuthIntent
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthStore
import com.volna.app.catalog.data.KtorSlotRepository
import com.volna.app.catalog.presentation.SlotListEffect
import com.volna.app.catalog.presentation.SlotListIntent
import com.volna.app.catalog.presentation.SlotListState
import com.volna.app.catalog.presentation.SlotListStore
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.network.VolnaApiClient
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.core.ui.Loadable
import com.volna.app.domain.model.Slot
import com.volna.app.profile.data.KtorProfileRepository
import com.volna.app.profile.presentation.ProfileEffect
import com.volna.app.profile.presentation.ProfileIntent
import com.volna.app.profile.presentation.ProfileScreen
import com.volna.app.profile.presentation.ProfileState
import com.volna.app.profile.presentation.ProfileStore

private enum class MainTab(val title: String) {
    Slots("Прогулки"),
    Bookings("Мои записи"),
    Profile("Профиль"),
}

private enum class RootState {
    CheckingSession,
    Auth,
    Main,
}

@Composable
fun VolnaApp() {
    VolnaTheme {
        val appScope = rememberCoroutineScope()
        val sessionRepository = remember { DefaultSessionRepository(PlatformSessionStorage) }
        val apiClient = remember { VolnaApiClient(sessionRepository) }
        val authRepository = remember { KtorAuthRepository(apiClient, sessionRepository) }
        val profileRepository = remember { KtorProfileRepository(apiClient, sessionRepository) }
        val slotRepository = remember { KtorSlotRepository(apiClient) }
        val authStore = remember { AuthStore(authRepository, profileRepository, appScope) }
        val profileStore = remember { ProfileStore(profileRepository, authRepository, appScope) }
        val slotListStore = remember { SlotListStore(slotRepository, appScope) }
        val authState by authStore.state.collectAsState()
        val profileState by profileStore.state.collectAsState()
        val slotListState by slotListStore.state.collectAsState()
        var rootState by remember { mutableStateOf(RootState.CheckingSession) }

        LaunchedEffect(sessionRepository) {
            rootState = if (sessionRepository.token().isNullOrBlank()) {
                RootState.Auth
            } else {
                RootState.Main
            }
        }

        LaunchedEffect(authStore) {
            while (true) {
                when (authStore.effects()) {
                    AuthEffect.Authenticated -> rootState = RootState.Main
                }
            }
        }

        LaunchedEffect(profileStore) {
            while (true) {
                when (profileStore.effects()) {
                    ProfileEffect.SignedOut -> {
                        authStore.accept(AuthIntent.Reset)
                        profileStore.accept(ProfileIntent.Reset)
                        slotListStore.accept(SlotListIntent.Reset)
                        rootState = RootState.Auth
                    }
                }
            }
        }

        LaunchedEffect(slotListStore) {
            while (true) {
                when (slotListStore.effects()) {
                    SlotListEffect.SignedOut -> {
                        authStore.accept(AuthIntent.Reset)
                        profileStore.accept(ProfileIntent.Reset)
                        slotListStore.accept(SlotListIntent.Reset)
                        rootState = RootState.Auth
                    }
                }
            }
        }

        when (rootState) {
            RootState.CheckingSession -> SessionSplash()
            RootState.Auth -> AuthScreen(
                state = authState,
                onIntent = authStore::accept,
            )
            RootState.Main -> MainTabs(
                slotListState = slotListState,
                onSlotListIntent = slotListStore::accept,
                profileState = profileState,
                onProfileIntent = profileStore::accept,
            )
        }
    }
}

@Composable
private fun SessionSplash() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VolnaTheme.tokens.spacing.md),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Волна",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MainTabs(
    slotListState: SlotListState,
    onSlotListIntent: (SlotListIntent) -> Unit,
    profileState: ProfileState,
    onProfileIntent: (ProfileIntent) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Slots) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = VolnaTheme.tokens.sizing.screenMaxWidth),
        ) {
            when (selectedTab) {
                MainTab.Slots -> SlotListScreen(
                    state = slotListState,
                    onIntent = onSlotListIntent,
                )
                MainTab.Bookings -> BookingsPlaceholder()
                MainTab.Profile -> ProfileScreen(
                    state = profileState,
                    onIntent = onProfileIntent,
                )
            }
            FloatingNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SlotListScreen(
    state: SlotListState,
    onIntent: (SlotListIntent) -> Unit,
) {
    LaunchedEffect(Unit) {
        onIntent(SlotListIntent.Load)
    }
    ScreenTitle("Прогулки")
    Text(
        text = "≡",
        modifier = Modifier
            .offset(x = VolnaTheme.tokens.sizing.filterIconX, y = VolnaTheme.tokens.sizing.topTitleY)
            .size(VolnaTheme.tokens.spacing.xl),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
    )
    when (val slots = state.slots) {
        Loadable.Initial,
        Loadable.Loading -> {
            SkeletonCard(y = VolnaTheme.tokens.sizing.listCardTopY)
            SkeletonCard(y = VolnaTheme.tokens.sizing.listCardSecondY)
        }
        is Loadable.Content -> SlotCards(slots.value)
        is Loadable.Empty -> StateMessage(
            title = "Пока нет доступных прогулок",
            description = "Загляните позже",
        )
        is Loadable.Error -> StateMessage(
            title = "Не удалось загрузить",
            description = "Проверьте соединение и попробуйте снова",
            buttonText = "Повторить",
            onClick = { onIntent(SlotListIntent.Retry) },
        )
    }
}

@Composable
private fun SkeletonCard(
    y: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .height(VolnaTheme.tokens.sizing.listCardHeight)
            .offset(x = VolnaTheme.tokens.spacing.md, y = y)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            ),
    )
}

@Composable
private fun SlotCards(slots: List<Slot>) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listCardTopY),
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
    ) {
        slots.take(3).forEach { slot ->
            SlotCard(slot)
        }
    }
}

@Composable
private fun SlotCard(slot: Slot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolnaTheme.tokens.sizing.listCardHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.lg),
            )
            .padding(VolnaTheme.tokens.spacing.md),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs)) {
            Text(slot.route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Инструктор: ${slot.instructor.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Свободно мест: ${slot.freeSeats}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${slot.price.value} ₽", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScreenTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = VolnaTheme.tokens.sizing.topTitleY),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StateMessage(
    title: String,
    description: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(VolnaTheme.tokens.sizing.contentWidth)
            .offset(x = VolnaTheme.tokens.spacing.md, y = VolnaTheme.tokens.sizing.listStateMessageY),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.xs),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (buttonText != null && onClick != null) {
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(bottom = VolnaTheme.tokens.sizing.navBottomPadding)
            .width(VolnaTheme.tokens.sizing.navWidth)
            .height(VolnaTheme.tokens.sizing.navHeight)
            .shadow(
                elevation = VolnaTheme.tokens.spacing.sm,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(VolnaTheme.tokens.radius.pill),
            )
            .padding(horizontal = VolnaTheme.tokens.spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        NavItem(
            tab = MainTab.Slots,
            selected = selectedTab == MainTab.Slots,
            icon = "▣",
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Bookings,
            selected = selectedTab == MainTab.Bookings,
            icon = "☷",
            onClick = onTabSelected,
        )
        NavItem(
            tab = MainTab.Profile,
            selected = selectedTab == MainTab.Profile,
            icon = "○",
            onClick = onTabSelected,
        )
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    selected: Boolean,
    icon: String,
    onClick: (MainTab) -> Unit,
) {
    Text(
        text = icon,
        modifier = Modifier
            .size(VolnaTheme.tokens.spacing.xl)
            .clickable { onClick(tab) },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun BookingsPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        ScreenTitle("Мои записи")
        Column(
            modifier = Modifier
                .width(VolnaTheme.tokens.sizing.contentWidth)
                .offset(
                    x = VolnaTheme.tokens.spacing.md,
                    y = VolnaTheme.tokens.sizing.stateMessageY,
                ),
            verticalArrangement = Arrangement.spacedBy(VolnaTheme.tokens.spacing.sm),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text("Здесь появятся ваши прогулки", textAlign = TextAlign.Center)
            Button(onClick = {}) {
                Text("Записаться")
            }
        }
    }
}
