package uz.nodirbek.flashcardsapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import uz.nodirbek.flashcardsapp.ui.screen.*
import uz.nodirbek.flashcardsapp.ui.viewmodel.HomeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Screen.bottomNavRoots

    // State for match done params
    var matchTimeSeconds by remember { mutableStateOf(0) }
    // State for test results
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Bottom-nav roots ─────────────────────────────────────────
            composable(
                Screen.Home.route,
                enterTransition = { BottomNavTransitions.enter() },
                exitTransition = { BottomNavTransitions.exit() }
            ) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToStudy = { navController.navigate(Screen.SrsReview.createRoute("default")) },
                    onNavigateToImport = { navController.navigate(Screen.Import.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDeck = { deckId -> navController.navigate(Screen.Deck.createRoute(deckId)) }
                )
            }

            composable(
                Screen.Stats.route,
                enterTransition = { BottomNavTransitions.enter() },
                exitTransition = { BottomNavTransitions.exit() }
            ) {
                StatsScreen(viewModel = homeViewModel)
            }

            composable(
                Screen.Settings.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { SideModalTransitions.exit() }
            ) {
                SettingsScreen(
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onImportClick = { navController.navigate(Screen.Import.route) }
                )
            }

            // ── Deck detail ──────────────────────────────────────────────
            composable(
                route = Screen.Deck.route,
                arguments = listOf(navArgument(Screen.Deck.ARG) { type = NavType.StringType }),
                enterTransition = { DeckDetailTransitions.enter() },
                exitTransition = { DeckDetailTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Deck.ARG) ?: return@composable
                DeckScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSrs = { id -> navController.navigate(Screen.SrsReview.createRoute(id)) },
                    onNavigateToFlashcards = { id -> navController.navigate(Screen.Flashcards.createRoute(id)) },
                    onNavigateToTestSetup = { id -> navController.navigate(Screen.TestSetup.createRoute(id)) },
                    onNavigateToMatch = { id -> navController.navigate(Screen.Match.createRoute(id)) },
                    onNavigateToForgetting = { id -> navController.navigate(Screen.ForgettingEdge.createRoute(id)) }
                )
            }

            // ── SRS Review ───────────────────────────────────────────────
            composable(
                route = Screen.SrsReview.route,
                arguments = listOf(navArgument(Screen.SrsReview.ARG) { type = NavType.StringType }),
                enterTransition = { StudyTransitions.enter() },
                exitTransition = { StudyTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.SrsReview.ARG) ?: return@composable
                StudyScreen(
                    viewModel = homeViewModel,
                    deckId = deckId,
                    onBackClick = { navController.popBackStack() },
                    onSessionDone = { _, _, _ ->
                        // done screen is inlined in StudyScreen; this triggers after it
                    }
                )
            }

            composable(
                Screen.ReviewDone.route,
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { ResultsTransitions.exit() }
            ) {
                ReviewDoneScreen(
                    sessionCount = 0,
                    accuracy = 0f,
                    xpEarned = 0,
                    streak = homeViewModel.uiState.value.streak,
                    onContinue = { navController.popBackStack() }
                )
            }

            // ── Flashcards ───────────────────────────────────────────────
            composable(
                route = Screen.Flashcards.route,
                arguments = listOf(navArgument(Screen.Flashcards.ARG) { type = NavType.StringType }),
                enterTransition = { StudyTransitions.enter() },
                exitTransition = { StudyTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Flashcards.ARG) ?: return@composable
                FlashcardsScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Test Setup → Test → Results ──────────────────────────────
            composable(
                route = Screen.TestSetup.route,
                arguments = listOf(navArgument(Screen.TestSetup.ARG) { type = NavType.StringType }),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { GameSetupTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.TestSetup.ARG) ?: return@composable
                TestSetupScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartTest = { id, count, isWritten ->
                        navController.navigate(Screen.Test.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Test.route,
                arguments = listOf(navArgument(Screen.Test.ARG) { type = NavType.StringType }),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { GameSetupTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Test.ARG) ?: return@composable
                TestScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onFinished = { results ->
                        testResults = results
                        navController.navigate(Screen.TestResults.route) {
                            popUpTo(Screen.Test.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.TestResults.route,
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { ResultsTransitions.exit() }
            ) {
                TestResultsScreen(
                    results = testResults,
                    onDone = { navController.popBackStack() },
                    onRepeatMistakes = { navController.popBackStack() }
                )
            }

            // ── Match → Done ─────────────────────────────────────────────
            composable(
                route = Screen.Match.route,
                arguments = listOf(navArgument(Screen.Match.ARG) { type = NavType.StringType }),
                enterTransition = { GameSetupTransitions.enter() },
                exitTransition = { GameSetupTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.Match.ARG) ?: return@composable
                MatchScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onFinished = { seconds, _, _ ->
                        matchTimeSeconds = seconds
                        navController.navigate(Screen.MatchDone.route) {
                            popUpTo(Screen.Match.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.MatchDone.route,
                enterTransition = { ResultsTransitions.enter() },
                exitTransition = { ResultsTransitions.exit() }
            ) {
                MatchDoneScreen(
                    timeSeconds = matchTimeSeconds,
                    isNewRecord = false,
                    bestSeconds = matchTimeSeconds,
                    onPlayAgain = { navController.popBackStack() },
                    onDone = {
                        navController.popBackStack(Screen.Deck.route, inclusive = false)
                    }
                )
            }

            // ── Forgetting Edge ──────────────────────────────────────────
            composable(
                route = Screen.ForgettingEdge.route,
                arguments = listOf(navArgument(Screen.ForgettingEdge.ARG) { type = NavType.StringType }),
                enterTransition = { ForgettingEdgeTransitions.enter() },
                exitTransition = { ForgettingEdgeTransitions.exit() }
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString(Screen.ForgettingEdge.ARG) ?: return@composable
                ForgettingEdgeScreen(
                    deckId = deckId,
                    viewModel = homeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartReview = { id -> navController.navigate(Screen.SrsReview.createRoute(id)) }
                )
            }

            // ── Import ────────────────────────────────────────────────────
            composable(
                Screen.Import.route,
                enterTransition = { SideModalTransitions.enter() },
                exitTransition = { SideModalTransitions.exit() }
            ) {
                ImportScreen(
                    viewModel = homeViewModel,
                    onCardsImported = { cards ->
                        homeViewModel.addCards(cards)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
