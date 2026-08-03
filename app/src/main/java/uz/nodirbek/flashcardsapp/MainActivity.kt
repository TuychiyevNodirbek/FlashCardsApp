package uz.nodirbek.flashcardsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler
import uz.nodirbek.flashcardsapp.ui.navigation.NavGraph
import uz.nodirbek.flashcardsapp.ui.navigation.Screen
import uz.nodirbek.flashcardsapp.ui.screen.OnboardingScreen
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = AppContainer(this)
        notificationScheduler = NotificationScheduler(this)
        notificationScheduler.scheduleStreakWarning()  // daily 20:00 streak protection reminder

        val factory = HomeViewModelFactory(
            cardRepository = container.cardRepository,
            deckRepository = container.deckRepository,
            statsRepository = container.statsRepository,
            preferencesDataStore = container.preferencesDataStore,
            rateCardUseCase = container.rateCardUseCase
        )
        homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setContent {
            val uiState by homeViewModel.uiState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (uiState.theme) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            // null = ещё загружается, false = нужно показать онбординг, true = уже видели
            var onboardingSeen by remember { mutableStateOf<Boolean?>(null) }
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
                                lifecycleScope.launch {
                                    container.preferencesDataStore.setOnboardingSeen()
                                    onboardingSeen = true
                                }
                            }
                        )
                        true -> {
                            val navController = rememberNavController()
                            LaunchedEffect(Unit) {
                                val navigateToStudy = intent?.getBooleanExtra("navigateToStudy", false) ?: false
                                if (navigateToStudy) {
                                    navController.navigate(Screen.SrsReview.createRoute("default"))
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
    }
}
