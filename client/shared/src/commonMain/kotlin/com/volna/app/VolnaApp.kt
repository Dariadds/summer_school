package com.volna.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import com.volna.app.auth.data.DefaultSessionRepository
import com.volna.app.auth.data.KtorAuthRepository
import com.volna.app.auth.presentation.AuthEffect
import com.volna.app.auth.presentation.AuthScreen
import com.volna.app.auth.presentation.AuthStore
import com.volna.app.core.theme.VolnaTheme
import com.volna.app.core.network.VolnaApiClient
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.profile.data.KtorProfileRepository

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
        val authStore = remember { AuthStore(authRepository, profileRepository, appScope) }
        val authState by authStore.state.collectAsState()
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

        when (rootState) {
            RootState.CheckingSession -> SessionSplash()
            RootState.Auth -> AuthScreen(
                state = authState,
                onIntent = authStore::accept,
            )
            RootState.Main -> MainTabs()
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
private fun MainTabs() {
    var selectedTab by remember { mutableStateOf(MainTab.Slots) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = { Text(tab.title.take(1)) },
                    )
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (selectedTab) {
                MainTab.Slots -> SlotListPlaceholder()
                MainTab.Bookings -> BookingsPlaceholder()
                MainTab.Profile -> ProfilePlaceholder()
            }
        }
    }
}

@Composable
private fun SlotListPlaceholder() {
    val spacing = VolnaTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "Прогулки",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Все") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Есть места") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Новичкам") })
                }
            }
        }
        items(sampleSlots) { slot ->
            SlotCard(slot)
        }
    }
}

@Composable
private fun SlotCard(slot: SampleSlot) {
    val spacing = VolnaTheme.tokens.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(slot.date, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(slot.availability) })
            }
            Text(slot.route, style = MaterialTheme.typography.titleMedium)
            Text(slot.instructor, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.xxs))
            Button(onClick = {}, enabled = slot.canBook) {
                Text(if (slot.canBook) "Записаться" else "Мест нет")
            }
        }
    }
}

@Composable
private fun BookingsPlaceholder() {
    val spacing = VolnaTheme.tokens.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text("Мои записи", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Здесь появятся предстоящие и прошедшие прогулки.")
        Button(onClick = {}) {
            Text("Записаться на прогулку")
        }
    }
}

@Composable
private fun ProfilePlaceholder() {
    val spacing = VolnaTheme.tokens.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Телефон и имя клиента будут загружаться из Profile API.")
        Button(onClick = {}) {
            Text("Выйти")
        }
    }
}

private data class SampleSlot(
    val date: String,
    val route: String,
    val instructor: String,
    val availability: String,
    val canBook: Boolean,
)

private val sampleSlots = listOf(
    SampleSlot("Сегодня, 18:30", "Острова и каналы", "Инструктор Мария", "5 мест", true),
    SampleSlot("Завтра, 10:00", "Большая вода", "Инструктор Иван", "2 места", true),
    SampleSlot("Сб, 12:00", "Новичковый маршрут", "Инструктор Анна", "Мест нет", false),
)
