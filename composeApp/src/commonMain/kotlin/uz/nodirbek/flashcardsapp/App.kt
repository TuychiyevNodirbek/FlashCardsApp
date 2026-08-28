package uz.nodirbek.flashcardsapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.ui.navigation.NavGraph
import uz.nodirbek.flashcardsapp.ui.screen.OnboardingScreen
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

/**
 * Общая точка входа приложения (Android: вызывается из MainActivity,
 * iOS: из MainViewController). Собирает HomeViewModel из уже готового
 * [AppContainer] (создаётся платформенным кодом — конструктор AppContainer
 * платформенный, Android нужен Context, iOS — нет) и показывает
 * онбординг → NavGraph, как раньше делал MainActivity напрямую.
 *
 * @param startDestination если задан, сразу после старта переходит на этот route
 *   (используется Android-версией для deep-link'а "navigateToStudy" из уведомления)
 */
@Composable
fun App(container: AppContainer, startDestination: String? = null) {
    val homeViewModel = viewModel {
        HomeViewModel(
            cardRepository = container.cardRepository,
            deckRepository = container.deckRepository,
            statsRepository = container.statsRepository,
            preferencesDataStore = container.preferencesDataStore,
            rateCardUseCase = container.rateCardUseCase
        )
    }

    val uiState by homeViewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (uiState.theme) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    // null = ещё загружается, false = нужно показать онбординг, true = уже видели
    var onboardingSeen by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        onboardingSeen = container.preferencesDataStore.onboardingSeen.first()
    }

    FlashCardsAppTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (onboardingSeen) {
                null -> {} // ждём DataStore
                false -> OnboardingScreen(
                    onFinish = {
                        scope.launch {
                            container.preferencesDataStore.setOnboardingSeen()
                            onboardingSeen = true
                        }
                    }
                )
                true -> {
                    val navController = rememberNavController()
                    LaunchedEffect(Unit) {
                        if (startDestination != null) {
                            navController.navigate(startDestination)
                        }
                    }
                    NavGraph(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        container = container
                    )
                }
            }
        }
    }
}
