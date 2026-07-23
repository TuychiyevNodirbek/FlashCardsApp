package uz.nodirbek.flashcardsapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import uz.nodirbek.flashcardsapp.data.local.database.FlashCardsDatabase
import uz.nodirbek.flashcardsapp.data.local.preferences.PreferencesDataStore
import uz.nodirbek.flashcardsapp.data.repository.CardRepository
import uz.nodirbek.flashcardsapp.data.repository.DeckRepository
import uz.nodirbek.flashcardsapp.data.repository.StatsRepository
import uz.nodirbek.flashcardsapp.domain.usecase.RateCardUseCase
import uz.nodirbek.flashcardsapp.notification.NotificationScheduler
import uz.nodirbek.flashcardsapp.ui.navigation.NavGraph
import uz.nodirbek.flashcardsapp.ui.navigation.Screen
import uz.nodirbek.flashcardsapp.ui.theme.FlashCardsAppTheme
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var notificationScheduler: NotificationScheduler

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled by OS */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val database = FlashCardsDatabase.getDatabase(this)
        val cardRepository = CardRepository(database.cardDao())
        val deckRepository = DeckRepository(database.deckDao())
        val statsRepository = StatsRepository(database.dailyStatsDao())
        val preferencesDataStore = PreferencesDataStore(this)
        val rateCardUseCase = RateCardUseCase()
        notificationScheduler = NotificationScheduler(this)

        val factory = HomeViewModelFactory(
            cardRepository = cardRepository,
            deckRepository = deckRepository,
            statsRepository = statsRepository,
            preferencesDataStore = preferencesDataStore,
            rateCardUseCase = rateCardUseCase
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

            FlashCardsAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        val navigateToStudy = intent?.getBooleanExtra("navigateToStudy", false) ?: false
                        if (navigateToStudy) {
                            navController.navigate(Screen.SrsReview.createRoute("default"))
                        }
                    }

                    NavGraph(navController = navController, homeViewModel = homeViewModel)
                }
            }
        }
    }
}
